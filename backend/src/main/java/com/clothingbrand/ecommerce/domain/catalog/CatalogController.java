package com.clothingbrand.ecommerce.domain.catalog;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(catalogService.getAllCategories());
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductListDto>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String query) {
        
        return ResponseEntity.ok(catalogService.getProducts(page, size, categoryId, query));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDetailDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getProduct(id));
    }
}
