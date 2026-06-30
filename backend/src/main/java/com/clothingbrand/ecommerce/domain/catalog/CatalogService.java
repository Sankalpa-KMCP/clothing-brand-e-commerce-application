package com.clothingbrand.ecommerce.domain.catalog;

import com.clothingbrand.ecommerce.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
            .map(c -> new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getImageUrl()))
            .toList();
    }

    public Page<ProductListDto> getProducts(int page, int size, Long categoryId, String query) {
        // Safe defaults and boundaries for pagination
        int safePage = Math.max(0, page);
        int safeSize = size > 0 && size <= 100 ? size : 20;
        
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        String safeQuery = (query == null || query.trim().isEmpty()) ? "" : "%" + query.trim().toLowerCase() + "%";
        
        return productRepository.findActiveProducts(categoryId, safeQuery, pageable)
                .map(ProductListDto::fromEntity);
    }

    public ProductDetailDto getProduct(Long id) {
        return productRepository.findByIdAndActiveWithVariants(id)
            .map(ProductDetailDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
