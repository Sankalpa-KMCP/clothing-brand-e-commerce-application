package com.clothingbrand.ecommerce.domain.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    
    boolean existsByProductIdAndSizeAndColor(Long productId, String size, String color);
    boolean existsByProductIdAndSizeAndColorAndIdNot(Long productId, String size, String color, Long id);
}
