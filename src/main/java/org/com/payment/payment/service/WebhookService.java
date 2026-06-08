package org.com.payment.payment.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final org.com.payment.payment.repository.PaymentRepository paymentRepository;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public void processWebhookEvent(String payload, String sigHeader) {

        Event event;

        // Step 1 — Verify the request is genuinely from Stripe
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new org.com.payment.exception.PaymentException("Invalid webhook signature", 400);
        }

        log.info("Webhook event received: {}", event.getType());

        // Step 2 — Handle the event type
        switch (event.getType()) {

            case "payment_intent.succeeded":
                handlePaymentSucceeded(event);
                break;

            case "payment_intent.payment_failed":
                handlePaymentFailed(event);
                break;

            case "charge.refunded":
                log.info("Charge refunded event received - DB already updated via API");
                break;

            default:
                log.info("Unhandled event type: {}", event.getType());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event
                .getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new org.com.payment.exception.PaymentException(
                        "Could not deserialize PaymentIntent", 500));

        String stripeTransactionId = paymentIntent.getId();
        log.info("Payment succeeded for Stripe ID: {}", stripeTransactionId);

        // Find payment in our DB by Stripe transaction ID
        Optional<org.com.payment.payment.entity.Payment> optionalPayment = paymentRepository
                .findByStripeTransactionId(stripeTransactionId);

        if (optionalPayment.isEmpty()) {
            log.warn("No payment found for Stripe ID: {}", stripeTransactionId);
            return;
        }

        org.com.payment.payment.entity.Payment payment = optionalPayment.get();
        payment.setStatus(org.com.payment.enums.PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        log.info("Payment {} updated to SUCCESS", payment.getId());
    }

    private void handlePaymentFailed(Event event) {
        try {
            String rawJson = event.getData().toJson();

            JsonObject dataObject = JsonParser
                    .parseString(rawJson)
                    .getAsJsonObject()
                    .getAsJsonObject("object");

            if (dataObject == null) {
                log.error("Could not extract object from event data");
                return;
            }

            String stripeTransactionId = dataObject
                    .get("id")
                    .getAsString();

            String failureReason = "Unknown failure reason";
            if (dataObject.has("last_payment_error")
                    && !dataObject.get("last_payment_error").isJsonNull()) {
                JsonObject lastError = dataObject
                        .getAsJsonObject("last_payment_error");
                if (lastError.has("message")) {
                    failureReason = lastError.get("message").getAsString();
                }
            }

        log.error("Payment failed for Stripe ID: {}. Reason: {}",
                stripeTransactionId, failureReason);

        Optional<org.com.payment.payment.entity.Payment> optionalPayment = paymentRepository
                .findByStripeTransactionId(stripeTransactionId);

        if (optionalPayment.isEmpty()) {
            log.warn("No payment found for Stripe ID: {}", stripeTransactionId);
            return;
        }

        org.com.payment.payment.entity.Payment payment = optionalPayment.get();
        payment.setStatus(org.com.payment.enums.PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);

        log.info("Payment {} updated to FAILED", payment.getId());
    }
}