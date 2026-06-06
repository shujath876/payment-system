package org.com.payment.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.payment.enums.PaymentStatus;
import org.com.payment.exception.PaymentException;
import org.com.payment.payment.dto.PaymentRequestDto;
import org.com.payment.payment.dto.PaymentResponseDTO;
import org.com.payment.payment.dto.RefundResponseDTO;
import org.com.payment.payment.entity.Payment;
import org.com.payment.payment.repository.PaymentRepository;
import org.com.payment.provider.dto.StripeRequestDTO;
import org.com.payment.provider.dto.StripeResponseDTO;
import org.com.payment.provider.service.StripeProviderService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ValidationService validationService;
    private final StripeProviderService providerService;

    public PaymentResponseDTO initiatePayment(PaymentRequestDto request) {
        log.info("Initiating payment for merchant: {}", request.getMerchantId());
        validationService.validatePaymentRequest(request);
        Payment payment = Payment.builder()
                .merchantId(request.getMerchantId())
                .customerEmail(request.getCustomerEmail())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .status(PaymentStatus.PENDING)
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment record created with ID : {}", savedPayment.getId());
        // Step 3 - Call Stripe
        StripeRequestDTO stripeRequest = StripeRequestDTO.builder()
                .paymentId(savedPayment.getId())
                .amount(savedPayment.getAmount())
                .currency(savedPayment.getCurrency())
                .customerEmail(savedPayment.getCustomerEmail())
                .description(savedPayment.getDescription())
                .build();
        StripeResponseDTO stripeResponse = providerService.processPayment(stripeRequest);
        if(stripeResponse.isSuccess()){
            savedPayment.setStatus(PaymentStatus.PENDING);
            savedPayment.setStripeTransactionId(stripeResponse.getTransactionId());
            log.info("Payment {} succeeded. Stripe ID: {}",
                    savedPayment.getId(),stripeResponse.getTransactionId());

        }
        else {
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setFailureReason(stripeResponse.getFailureReason());
            log.info("Payment {} failed. Reason:{}",
                    savedPayment.getId(),stripeResponse.getFailureReason());
        }
        Payment updatedPayment = paymentRepository.save(savedPayment);

        return mapToResponseDTO(savedPayment,stripeResponse.getClientSecret());

    }
    public RefundResponseDTO refundPayment(Long paymentId) {
        log.info("Refund requested for payment ID: {}", paymentId);

        // Step 1 — Find the payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(
                        "Payment not found with ID: " + paymentId, 404));

        // Step 2 — Only SUCCESS payments can be refunded
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException(
                    "Only successful payments can be refunded. " +
                            "Current status: " + payment.getStatus(), 400);
        }

        // Step 3 — Check if already refunded
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentException(
                    "Payment " + paymentId + " has already been refunded.", 400);
        }

        // Step 4 — Call Stripe to process refund
        StripeResponseDTO stripeResponse = providerService
                .processRefund(payment.getStripeTransactionId());

        // Step 5 — Update payment record
        if (stripeResponse.isSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setStripeRefundId(stripeResponse.getTransactionId());
            payment.setRefundAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("Payment {} successfully refunded. Refund ID: {}",
                    paymentId, stripeResponse.getTransactionId());

            return RefundResponseDTO.builder()
                    .paymentId(payment.getId())
                    .merchantId(payment.getMerchantId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .status(PaymentStatus.REFUNDED)
                    .stripeRefundId(stripeResponse.getTransactionId())
                    .stripeTransactionId(payment.getStripeTransactionId())
                    .refundedAt(payment.getRefundAt())
                    .message("Payment refunded successfully")
                    .build();

        } else {
            throw new PaymentException(
                    "Refund failed: " + stripeResponse.getFailureReason(), 500);
        }
    }

    public PaymentResponseDTO getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(()->new PaymentException(
                        "Payment not found with ID: "+paymentId,404
                ));
        return mapToResponseDTO(payment,null);

    }
    public List<PaymentResponseDTO> getPaymentsByMerchant(String merchantId){
        List<Payment> payments = paymentRepository.findByMerchantId(merchantId);
        if(payments.isEmpty()){
            throw  new PaymentException(
                    "No payment found for this merchant : "+merchantId,404);

        }
        return payments.stream()
                .map(payment -> mapToResponseDTO(payment,null))
                .collect(Collectors.toList());
    }
    private PaymentResponseDTO mapToResponseDTO(Payment payment,String clientSecret){
        return PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .merchantId(payment.getMerchantId())
                .customerEmail(payment.getCustomerEmail())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .stripeTransactionId(payment.getStripeTransactionId())
                .stripeClientSecret(clientSecret)
                .failureReason(payment.getFailureReason())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .build();

    }
}
