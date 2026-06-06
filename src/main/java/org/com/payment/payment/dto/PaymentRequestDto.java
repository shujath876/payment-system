package org.com.payment.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDto {

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;

    @NonNull
    @DecimalMin(value = "1.00",message = "Amount must be at least 1.00")
    private BigDecimal amount;
    @NonNull
    @Pattern(regexp = "^[A-Z]{3}$",message = "Currenct must be 3-letter code like INR or USD")
    private String currency;
    private  String description;


}
