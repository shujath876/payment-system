package org.com.payment.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.payment.payment.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j

public class WebhookController {
    private final WebhookService webhookService;
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader)
    {
        log.info("Received Stripe webhook event");
        webhookService.processWebhookEvent(payload,sigHeader);
        return ResponseEntity.ok("Received");
    }



}
