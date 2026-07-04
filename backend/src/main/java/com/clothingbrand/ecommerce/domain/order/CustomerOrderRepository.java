package com.clothingbrand.ecommerce.domain.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByUserId(Long userId, Pageable pageable);

    Page<CustomerOrder> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<CustomerOrder> findByIdAndUserIdWithItems(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items WHERE o.id = :orderId")
    Optional<CustomerOrder> findByIdWithItems(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<CustomerOrder> findByIdAndUserIdForUpdate(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.id = :orderId")
    Optional<CustomerOrder> findByIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.user.id = :userId AND o.status = 'PENDING_PAYMENT' AND o.reservationExpiresAt > :now")
    Optional<CustomerOrder> findActivePendingPaymentOrderForUpdate(@Param("userId") Long userId, @Param("now") java.time.OffsetDateTime now);

    @Query("SELECT o FROM CustomerOrder o WHERE o.user.id = :userId AND o.status = 'PENDING_PAYMENT' AND o.reservationExpiresAt > :now")
    Optional<CustomerOrder> findActivePendingPaymentOrder(@Param("userId") Long userId, @Param("now") java.time.OffsetDateTime now);

    @Query("SELECT o.id FROM CustomerOrder o WHERE o.status = com.clothingbrand.ecommerce.domain.order.OrderStatus.PENDING_PAYMENT AND o.paymentStatus = com.clothingbrand.ecommerce.domain.order.PaymentStatus.PENDING AND o.reservationExpiresAt <= :now")
    java.util.List<Long> findExpiredReservationIds(@Param("now") java.time.OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.user.id = :userId AND o.status = com.clothingbrand.ecommerce.domain.order.OrderStatus.PENDING_PAYMENT AND o.reservationExpiresAt <= :now")
    Optional<CustomerOrder> findExpiredPendingPaymentOrderForUpdate(@Param("userId") Long userId, @Param("now") java.time.OffsetDateTime now);
}
