package com.clothingbrand.ecommerce.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderDeliveryAddressRepository extends JpaRepository<OrderDeliveryAddress, Long> {
    Optional<OrderDeliveryAddress> findByOrderId(Long orderId);
}
