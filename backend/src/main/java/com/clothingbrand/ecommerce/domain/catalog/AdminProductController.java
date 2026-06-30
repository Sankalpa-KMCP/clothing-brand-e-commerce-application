package com.clothingbrand.ecommerce.domain.catalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final CatalogService catalogService;

    public AdminProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    public ResponseEntity<AdminProductResponseDto> createProduct(@Valid @RequestBody AdminProductRequestDto requestDto) {
        return new ResponseEntity<>(catalogService.createProduct(requestDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminProductResponseDto> updateProduct(@PathVariable Long id, @Valid @RequestBody AdminProductRequestDto requestDto) {
        return ResponseEntity.ok(catalogService.updateProduct(id, requestDto));
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<AdminProductVariantResponseDto> createProductVariant(@PathVariable Long productId, @Valid @RequestBody AdminProductVariantRequestDto requestDto) {
        return new ResponseEntity<>(catalogService.createProductVariant(productId, requestDto), HttpStatus.CREATED);
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<AdminProductVariantResponseDto> updateProductVariant(@PathVariable Long productId, @PathVariable Long variantId, @Valid @RequestBody AdminProductVariantRequestDto requestDto) {
        return ResponseEntity.ok(catalogService.updateProductVariant(productId, variantId, requestDto));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<Void> deleteProductVariant(@PathVariable Long productId, @PathVariable Long variantId) {
        catalogService.deleteProductVariant(productId, variantId);
        return ResponseEntity.noContent().build();
    }
}
