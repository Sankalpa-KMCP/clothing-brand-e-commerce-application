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
}
