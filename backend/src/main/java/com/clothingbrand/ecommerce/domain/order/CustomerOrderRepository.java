package com.clothingbrand.ecommerce.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<CustomerOrder> findByIdAndUserIdWithItems(@Param("orderId") Long orderId, @Param("userId") Long userId);
}
