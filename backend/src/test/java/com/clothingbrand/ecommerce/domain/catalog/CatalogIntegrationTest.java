package com.clothingbrand.ecommerce.domain.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional // Ensures data is rolled back after each test
class CatalogIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category categoryTShirts;
    private Category categoryHoodies;
    private Product activeProductTee;
    private Product activeProductHoodie;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clear repos to ensure isolated data context inside transaction if needed,
        // but @Transactional automatically rolls back, so we just create what we need.
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // 1. Create Categories
        Category cat1 = new Category();
        cat1.setName("Hoodies"); // Alphabetically first
        cat1.setDescription("Cozy hoodies");
        categoryHoodies = categoryRepository.save(cat1);

        Category cat2 = new Category();
        cat2.setName("T-Shirts"); // Alphabetically second
        cat2.setDescription("Classic t-shirts");
        categoryTShirts = categoryRepository.save(cat2);

        // 2. Create Active Product with variants
        Product p1 = new Product();
        p1.setName("Basic Black Tee");
        p1.setDescription("A basic black t-shirt");
        p1.setCategory(categoryTShirts);
        p1.setActive(true);

        ProductVariant v1 = new ProductVariant();
        v1.setProduct(p1);
        v1.setSku("TEE-BLK-S");
        v1.setSize("S");
        v1.setColor("Black");
        v1.setPrice(new BigDecimal("19.99"));
        v1.setStockQuantity(5);

        ProductVariant v2 = new ProductVariant();
        v2.setProduct(p1);
        v2.setSku("TEE-BLK-M");
        v2.setSize("M");
        v2.setColor("Black");
        v2.setPrice(new BigDecimal("14.99")); // Lowest price
        v2.setStockQuantity(0); // Out of stock

        p1.getVariants().addAll(List.of(v1, v2));
        activeProductTee = productRepository.save(p1);

        // 3. Create another Active Product
        Product p2 = new Product();
        p2.setName("Graphic Hoodie");
        p2.setCategory(categoryHoodies);
        p2.setActive(true);

        ProductVariant v3 = new ProductVariant();
        v3.setProduct(p2);
        v3.setSku("HOOD-GRAPH-L");
        v3.setSize("L");
        v3.setColor("Gray");
        v3.setPrice(new BigDecimal("49.99"));
        v3.setStockQuantity(10);

        p2.getVariants().add(v3);
        activeProductHoodie = productRepository.save(p2);

        // 4. Create Inactive Product
        Product p3 = new Product();
        p3.setName("Old Winter Coat");
        p3.setCategory(categoryHoodies);
        p3.setActive(false);

        ProductVariant v4 = new ProductVariant();
        v4.setProduct(p3);
        v4.setSku("COAT-WINT-M");
        v4.setSize("M");
        v4.setColor("Navy");
        v4.setPrice(new BigDecimal("99.99"));
        v4.setStockQuantity(2);

        p3.getVariants().add(v4);
        inactiveProduct = productRepository.save(p3);
    }

    @Test
    void testGetCategories_ordersByNameAndOnlyReturnsPublicFields() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Hoodies"))
                .andExpect(jsonPath("$[1].name").value("T-Shirts"))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].description").value("Cozy hoodies"))
                // Ensure raw entity fields like 'createdAt' are missing from DTO
                .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void testGetProducts_returnsOnlyActiveProductsAndCorrectStartingPrice() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                // Only 2 active products, despite 3 in DB
                .andExpect(jsonPath("$.content.length()").value(2))
                // Ordered by createdAt DESC, so Hoodie (created last) is first
                .andExpect(jsonPath("$.content[0].name").value("Graphic Hoodie"))
                .andExpect(jsonPath("$.content[1].name").value("Basic Black Tee"))
                // Check starting price logic (14.99 is the lowest of 19.99 and 14.99)
                .andExpect(jsonPath("$.content[1].startingPrice").value(14.99))
                // Check SKU is not exposed
                .andExpect(jsonPath("$.content[1].sku").doesNotExist());
    }

    @Test
    void testGetProducts_withCategoryIdFilter() throws Exception {
        mockMvc.perform(get("/api/products").param("categoryId", categoryTShirts.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Basic Black Tee"));
    }

    @Test
    void testGetProducts_withSearchQuery_mixedCase() throws Exception {
        // Search "black" should match "Basic Black Tee"
        mockMvc.perform(get("/api/products").param("query", "bLaCk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Basic Black Tee"));
    }

    @Test
    void testGetProducts_withSearchQuery_blankBehavesSafely() throws Exception {
        mockMvc.perform(get("/api/products").param("query", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void testGetProducts_withSearchQuery_wildcardsDoNotCrash() throws Exception {
        // PostgreSQL bytea crash regression test
        mockMvc.perform(get("/api/products").param("query", "%"))
                .andExpect(status().isOk())
                // Currently matching all or none depending on wildcard logic, we just verify it doesn't crash (status 500)
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetProducts_pagination_structureAndDefaults() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void testGetProducts_pagination_invalidValuesHandledGracefully() throws Exception {
        // Negative page and excessive size
        mockMvc.perform(get("/api/products").param("page", "-5").param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0)) // Defaulted to page 0
                .andExpect(jsonPath("$.size").value(20)); // Capped/defaulted to size 20 (or max)
    }

    @Test
    void testGetProductDetail_validActiveProduct_returnsExpectedFieldsAndNoSecrets() throws Exception {
        mockMvc.perform(get("/api/products/{id}", activeProductTee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Basic Black Tee"))
                .andExpect(jsonPath("$.category.name").value("T-Shirts"))
                .andExpect(jsonPath("$.variants.length()").value(2))
                
                // First variant (M) has stock 0, so inStock = false
                // Second variant (S) has stock 5, so inStock = true
                // Note: Order of variants depends on DB fetch, so we check for existence of properties.
                .andExpect(jsonPath("$.variants[0].size").exists())
                .andExpect(jsonPath("$.variants[0].color").exists())
                .andExpect(jsonPath("$.variants[0].price").exists())
                .andExpect(jsonPath("$.variants[0].inStock").isBoolean())
                
                // Assert sensitive fields are hidden
                .andExpect(jsonPath("$.variants[0].sku").doesNotExist())
                .andExpect(jsonPath("$.variants[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.sku").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void testGetProductDetail_inactiveProduct_returns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", inactiveProduct.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                // No internal exception stack trace or DB details
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void testGetProductDetail_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 9999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }
}
