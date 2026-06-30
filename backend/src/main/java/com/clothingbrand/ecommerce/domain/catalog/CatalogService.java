package com.clothingbrand.ecommerce.domain.catalog;

import com.clothingbrand.ecommerce.exception.ResourceNotFoundException;
import com.clothingbrand.ecommerce.exception.DuplicateResourceException;
import com.clothingbrand.ecommerce.exception.ResourceConflictException;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final ProductVariantRepository productVariantRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
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

    @Transactional
    public AdminCategoryResponseDto createCategory(AdminCategoryRequestDto requestDto) {
        String trimmedName = requestDto.name().trim();

        Category category = new Category();
        category.setName(trimmedName);
        category.setDescription(requestDto.description());
        category.setImageUrl(requestDto.imageUrl());

        try {
            Category savedCategory = categoryRepository.save(category);
            return AdminCategoryResponseDto.fromEntity(savedCategory);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Category name already exists");
        }
    }

    @Transactional
    public AdminCategoryResponseDto updateCategory(Long id, AdminCategoryRequestDto requestDto) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String trimmedName = requestDto.name().trim();
        category.setName(trimmedName);
        category.setDescription(requestDto.description());
        category.setImageUrl(requestDto.imageUrl());

        try {
            Category updatedCategory = categoryRepository.save(category);
            return AdminCategoryResponseDto.fromEntity(updatedCategory);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Category name already exists");
        }
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }

        if (productRepository.existsByCategoryId(id)) {
            throw new ResourceConflictException("Cannot delete category as it is still referenced by one or more products");
        }

        categoryRepository.deleteById(id);
    }

    @Transactional
    public AdminProductResponseDto createProduct(AdminProductRequestDto requestDto) {
        Category category = categoryRepository.findById(requestDto.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + requestDto.categoryId()));

        Product product = new Product();
        product.setCategory(category);
        product.setName(requestDto.name().trim());
        product.setDescription(requestDto.description());
        product.setImageUrl(requestDto.imageUrl());
        product.setActive(requestDto.active());

        Product savedProduct = productRepository.save(product);
        return AdminProductResponseDto.fromEntity(savedProduct);
    }

    @Transactional
    public AdminProductResponseDto updateProduct(Long id, AdminProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(requestDto.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + requestDto.categoryId()));

        product.setCategory(category);
        product.setName(requestDto.name().trim());
        product.setDescription(requestDto.description());
        product.setImageUrl(requestDto.imageUrl());
        product.setActive(requestDto.active());

        Product updatedProduct = productRepository.save(product);
        return AdminProductResponseDto.fromEntity(updatedProduct);
    }

    @Transactional
    public AdminProductVariantResponseDto createProductVariant(Long productId, AdminProductVariantRequestDto requestDto) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        String sku = requestDto.sku().trim();
        String size = requestDto.size().trim();
        String color = requestDto.color().trim();

        if (productVariantRepository.existsBySku(sku)) {
            throw new ResourceConflictException("Variant with SKU '" + sku + "' already exists.");
        }
        if (productVariantRepository.existsByProductIdAndSizeAndColor(productId, size, color)) {
            throw new ResourceConflictException("Variant with size '" + size + "' and color '" + color + "' already exists for this product.");
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(requestDto.price());

        ProductVariant savedVariant = productVariantRepository.save(variant);
        return AdminProductVariantResponseDto.fromEntity(savedVariant);
    }

    @Transactional
    public AdminProductVariantResponseDto updateProductVariant(Long productId, Long variantId, AdminProductVariantRequestDto requestDto) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        ProductVariant variant = productVariantRepository.findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variant not found for product id: " + productId);
        }

        String sku = requestDto.sku().trim();
        String size = requestDto.size().trim();
        String color = requestDto.color().trim();

        if (productVariantRepository.existsBySkuAndIdNot(sku, variantId)) {
            throw new ResourceConflictException("Variant with SKU '" + sku + "' already exists.");
        }
        if (productVariantRepository.existsByProductIdAndSizeAndColorAndIdNot(productId, size, color, variantId)) {
            throw new ResourceConflictException("Variant with size '" + size + "' and color '" + color + "' already exists for this product.");
        }

        variant.setSku(sku);
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(requestDto.price());

        ProductVariant updatedVariant = productVariantRepository.save(variant);
        return AdminProductVariantResponseDto.fromEntity(updatedVariant);
    }

    @Transactional
    public void deleteProductVariant(Long productId, Long variantId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductVariant variant = productVariantRepository.findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variant not found for product id: " + productId);
        }

        product.getVariants().remove(variant);
        productVariantRepository.delete(variant);
    }
}
