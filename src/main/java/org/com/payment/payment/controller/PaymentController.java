package org.com.payment.payment.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.payment.payment.dto.PaymentRequestDto;
import org.com.payment.payment.dto.PaymentResponseDTO;
import org.com.payment.payment.dto.RefundResponseDTO;
import org.com.payment.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> initializePayment(
            @Valid @RequestBody PaymentRequestDto requestDto
            ){
        log.info("Received payment from merchant :{}",requestDto.getMerchantId());
        PaymentResponseDTO response = paymentService.initiatePayment(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @PathVariable Long paymentId){
        PaymentResponseDTO response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);

    }
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByMerchant(
            @PathVariable String merchantId){
        List<PaymentResponseDTO> responseDTOS = paymentService.getPaymentsByMerchant(merchantId);
        return ResponseEntity.ok(responseDTOS);
    }
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundResponseDTO> refundPayment(
            @PathVariable Long paymentId) {

        log.info("Refund request received for payment ID: {}", paymentId);
        RefundResponseDTO response = paymentService.refundPayment(paymentId);
        return ResponseEntity.ok(response);
    }


}
