package org.com.payment.payment.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.NonNull;
import org.com.payment.exception.PaymentException;
import org.com.payment.payment.dto.PaymentRequestDto;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class ValidationService {
    private static final List<String> SUPPORTED_CURRENCIES =
            Arrays.asList("INR", "USD", "EUR", "GBP", "AED", "SAR");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");

    public void validatePaymentRequest(PaymentRequestDto requestDto) {

        validateCurrency(requestDto.getCurrency());
        validateAmount(requestDto.getAmount());
    }

    private void validateAmount(@NonNull @DecimalMin(value = "1.00",message = "Amount must be at least 1.00") BigDecimal amount) {
        if(amount.compareTo(MAX_AMOUNT)>0){
            throw new PaymentException(
                    "Amount exceeds maximum allowed limit of + "+MAX_AMOUNT,400);
        }
    }

    private void validateCurrency(@NonNull @Pattern(regexp = "^[A-Z]{3}$",message = "Currency must be 3-letter code like INR or USD") String currency) {
        if(!SUPPORTED_CURRENCIES.contains(currency)){
            throw new PaymentException("Unsupported currency: "+currency+". Supported currencies : "+SUPPORTED_CURRENCIES,400);
        }
    }

}


