package com.clothingbrand.ecommerce.payment;

import com.stripe.exception.SignatureVerificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookService webhookService;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    public StripeWebhookController(StripeWebhookService webhookService,
                                   com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.webhookService = webhookService;
        this.observabilityService = observabilityService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        if (sigHeader == null || sigHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            webhookService.handleWebhook(payload, sigHeader);
            observabilityService.trackWebhookVerified();
            observabilityService.trackWebhookHandled();
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            observabilityService.trackWebhookRejected();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            observabilityService.trackWebhookFailed();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            observabilityService.trackWebhookFailed();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
