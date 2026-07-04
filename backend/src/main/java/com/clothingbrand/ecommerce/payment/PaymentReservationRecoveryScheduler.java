package com.clothingbrand.ecommerce.payment;

import com.clothingbrand.ecommerce.domain.order.CustomerOrderRepository;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentReservationRecoveryScheduler {

    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentReservationRecoveryService recoveryService;
    private final DateTimeProvider dateTimeProvider;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    @Value("${app.payment.reservation.cleanup.enabled:true}")
    private boolean enabled;

    public PaymentReservationRecoveryScheduler(CustomerOrderRepository customerOrderRepository,
                                                PaymentReservationRecoveryService recoveryService,
                                                DateTimeProvider dateTimeProvider,
                                                com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.customerOrderRepository = customerOrderRepository;
        this.recoveryService = recoveryService;
        this.dateTimeProvider = dateTimeProvider;
        this.observabilityService = observabilityService;
    }

    @Scheduled(fixedDelayString = "${app.payment.reservation.cleanup.fixed-delay-ms:60000}")
    public void cleanupExpiredReservations() {
        if (!enabled) {
            return;
        }

        List<Long> expiredOrderIds = customerOrderRepository.findExpiredReservationIds(dateTimeProvider.now());
        for (Long orderId : expiredOrderIds) {
            try {
                recoveryService.recoverExpiredOrder(orderId);
                observabilityService.trackReservationExpiryRecovery();
            } catch (Exception e) {
                // Swallow exception so one failure does not prevent later orders from being recovered
            }
        }
    }
}
