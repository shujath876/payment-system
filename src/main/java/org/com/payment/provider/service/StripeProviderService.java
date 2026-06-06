package org.com.payment.provider.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.com.payment.provider.dto.StripeRequestDTO;
import org.com.payment.provider.dto.StripeResponseDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripeProviderService {
    public StripeResponseDTO processPayment(StripeRequestDTO request) {
        try {
            long amountInSmallestUnit = request.getAmount()
                    .multiply(new BigDecimal("100"))
                    .longValue();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInSmallestUnit)
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setDescription(request.getDescription())
                    .setReceiptEmail(request.getCustomerEmail())
                    .addPaymentMethodType("card")
                    .build();
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            log.info("Stripe PaymentIntent created :{}", paymentIntent.getId());
            return StripeResponseDTO.builder()
                    .success(true)
                    .transactionId(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .build();


        } catch(StripeException ex) {
            log.error("Stripe error for payment ID {}:{}",request.getPaymentId(),
                    ex.getMessage());
            return StripeResponseDTO.builder()
                    .success(false)
                    .failureReason(ex.getMessage())
                    .build();

        }

    }
    public StripeResponseDTO processRefund(String stripeTransactionId) {
        try {
            log.info("Processing refund for Stripe transaction: {}", stripeTransactionId);

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(stripeTransactionId)
                    .build();

            Refund refund = Refund.create(params);

            log.info("Refund successful. Refund ID: {}", refund.getId());

            return StripeResponseDTO.builder()
                    .success(true)
                    .transactionId(refund.getId())
                    .build();

        } catch (StripeException e) {
            log.error("Refund failed for transaction {}: {}",
                    stripeTransactionId, e.getMessage());

            return StripeResponseDTO.builder()
                    .success(false)
                    .failureReason(e.getMessage())
                    .build();
        }
    }

}
