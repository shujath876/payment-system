package org.com.payment.payment.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.payment.enums.PaymentStatus;
import org.com.payment.exception.PaymentException;
import org.com.payment.payment.entity.Payment;
import org.com.payment.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentRepository paymentRepository;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public void processWebhookEvent(String payload, String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new PaymentException("Invalid webhook signature", 400);
        }

        log.info("Webhook event received: {}", event.getType());

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

            log.info("Payment succeeded for Stripe ID: {}", stripeTransactionId);

            Optional<Payment> optionalPayment = paymentRepository
                    .findByStripeTransactionId(stripeTransactionId);

            if (optionalPayment.isEmpty()) {
                log.warn("No payment found for Stripe ID: {}", stripeTransactionId);
                return;
            }

            Payment payment = optionalPayment.get();
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            log.info("Payment {} updated to SUCCESS", payment.getId());

        } catch (Exception e) {
            log.error("Error processing payment success webhook: {}", e.getMessage());
        }
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

            Optional<Payment> optionalPayment = paymentRepository
                    .findByStripeTransactionId(stripeTransactionId);

            if (optionalPayment.isEmpty()) {
                log.warn("No payment found for Stripe ID: {}", stripeTransactionId);
                return;
            }

            Payment payment = optionalPayment.get();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            paymentRepository.save(payment);

            log.info("Payment {} updated to FAILED", payment.getId());

        } catch (Exception e) {
            log.error("Error processing payment failed webhook: {}", e.getMessage());
        }
    }
}