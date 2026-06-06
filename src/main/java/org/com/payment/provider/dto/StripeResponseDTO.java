package org.com.payment.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeResponseDTO {
    private boolean success;
    private String transactionId;
    private String clientSecret;
    private String failureReason;

}
