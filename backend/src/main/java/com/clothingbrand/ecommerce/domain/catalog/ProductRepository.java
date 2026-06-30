package com.clothingbrand.ecommerce.domain.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:query = '' OR LOWER(p.name) LIKE :query)")
    Page<Product> findActiveProducts(@Param("categoryId") Long categoryId, @Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.id = :id AND p.active = true")
    Optional<Product> findByIdAndActiveWithVariants(@Param("id") Long id);

    boolean existsByCategoryId(Long categoryId);
}
