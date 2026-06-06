package org.com.payment.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.com.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class RefundResponseDTO {

        private Long paymentId;
        private String merchantId;
        private BigDecimal amount;
        private String currency;
        private PaymentStatus status;
        private String stripeRefundId;
        private String stripeTransactionId;
        private LocalDateTime refundedAt;
        private String message;
}
