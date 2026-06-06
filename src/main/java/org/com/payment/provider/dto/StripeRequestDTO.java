package org.com.payment.provider.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeRequestDTO {
    private Long paymentId;
    private BigDecimal amount;
    private String currency;
    private String customerEmail;
    private String description;
    private String paymentMethodId;


}
