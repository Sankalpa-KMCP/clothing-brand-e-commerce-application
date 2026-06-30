package com.clothingbrand.ecommerce.domain.catalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CatalogService catalogService;

    public AdminCategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    public ResponseEntity<AdminCategoryResponseDto> createCategory(@Valid @RequestBody AdminCategoryRequestDto requestDto) {
        return new ResponseEntity<>(catalogService.createCategory(requestDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminCategoryResponseDto> updateCategory(@PathVariable Long id, @Valid @RequestBody AdminCategoryRequestDto requestDto) {
        return ResponseEntity.ok(catalogService.updateCategory(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
