package com.clothingbrand.ecommerce.domain.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    
    boolean existsByProductIdAndSizeAndColor(Long productId, String size, String color);
    boolean existsByProductIdAndSizeAndColorAndIdNot(Long productId, String size, String color, Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :adjustment WHERE v.id = :variantId AND v.product.id = :productId AND (v.stockQuantity + :adjustment) >= 0")
    int adjustStock(@Param("productId") Long productId, @Param("variantId") Long variantId, @Param("adjustment") Integer adjustment);
}
