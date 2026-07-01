package com.clothingbrand.ecommerce.domain.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    @Query("SELECT c FROM CustomerAddress c WHERE c.user.id = :userId ORDER BY c.isDefault DESC, c.createdAt DESC, c.id DESC")
    List<CustomerAddress> findAllByUserIdOrdered(@Param("userId") Long userId);

    Optional<CustomerAddress> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CustomerAddress c SET c.isDefault = false WHERE c.user.id = :userId AND c.isDefault = true")
    void clearDefaultAddressesForUser(@Param("userId") Long userId);

    Optional<CustomerAddress> findByUserIdAndIsDefaultTrue(Long userId);
}
