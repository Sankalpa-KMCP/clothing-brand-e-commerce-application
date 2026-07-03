package com.clothingbrand.ecommerce.storage;

import com.clothingbrand.ecommerce.domain.catalog.Category;
import com.clothingbrand.ecommerce.domain.catalog.CategoryRepository;
import com.clothingbrand.ecommerce.domain.catalog.Product;
import com.clothingbrand.ecommerce.domain.catalog.ProductRepository;
import com.clothingbrand.ecommerce.domain.order.*;
import com.clothingbrand.ecommerce.domain.user.*;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "app.storage.type=local",
        "app.storage.s3.endpoint=http://localhost:9000",
        "app.storage.s3.region=us-east-1",
        "app.storage.s3.bucket-name=test-bucket",
        "app.storage.s3.access-key=access",
        "app.storage.s3.secret-key=secret",
        "app.storage.s3.public-url-prefix=https://cdn.example.com/",
        "app.migration.local-to-s3.enabled=true",
        "app.migration.local-to-s3.confirm=true",
        "app.migration.local-to-s3.dry-run=false",
        "app.migration.local-to-s3.report-path=target/test-migration-report.json"
})
public class LocalToS3MigrationIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private S3StorageProperties s3Properties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @TempDir
    Path tempUploadDir;

    private S3Client s3Client;
    private LocalToS3MigrationRunner runner;

    private Product testProduct;
    private Category testCategory;
    private OrderItem testOrderItem;
    private Path img1;
    private Path img2;

    @BeforeEach
    void setUp() throws Exception {
        s3Client = mock(S3Client.class);
        
        runner = new LocalToS3MigrationRunner(
                productRepository,
                categoryRepository,
                s3Properties,
                objectMapper,
                mock(com.clothingbrand.ecommerce.config.ObservabilityService.class)
        );
        runner.setS3Client(s3Client);

        // Setup properties and override local directory path to temp path
        ReflectionTestUtils.setField(runner, "uploadDir", tempUploadDir.toString());
        ReflectionTestUtils.setField(runner, "oldLocalPrefix", "http://localhost:8080/api/images/");
        ReflectionTestUtils.setField(runner, "migrationConfirm", true);
        ReflectionTestUtils.setField(runner, "dryRun", false);
        ReflectionTestUtils.setField(runner, "reportPath", "target/test-migration-report.json");
        ReflectionTestUtils.setField(runner, "storageType", "s3");

        // Create valid dummy images
        img1 = tempUploadDir.resolve("product-image.jpg");
        img2 = tempUploadDir.resolve("category-image.png");
        createDummyImage(img1);
        createDummyImage(img2);

        // Populate Database test data
        transactionTemplate.executeWithoutResult(status -> {
            orderItemRepository.deleteAll();
            customerOrderRepository.deleteAll();
            userRepository.deleteAll();
            productRepository.deleteAll();
            categoryRepository.deleteAll();

            testCategory = new Category();
            testCategory.setName("Test Category");
            testCategory.setImageUrl("http://localhost:8080/api/images/category-image.png");
            testCategory = categoryRepository.save(testCategory);

            testProduct = new Product();
            testProduct.setName("Test Product");
            testProduct.setDescription("A description");
            testProduct.setCategory(testCategory);
            testProduct.setImageUrl("http://localhost:8080/api/images/product-image.jpg");
            testProduct.setActive(true);
            testProduct = productRepository.save(testProduct);

            Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

            User user = new User();
            user.setRole(customerRole);
            user.setEmail("test-migrator@example.com");
            user.setPasswordHash("hash");
            user.setFirstName("Test");
            user.setLastName("Migrator");
            user.setActive(true);
            user = userRepository.save(user);

            CustomerOrder order = new CustomerOrder();
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setSubtotal(BigDecimal.TEN);
            order.setTotal(BigDecimal.TEN);
            order = customerOrderRepository.save(order);

            testOrderItem = new OrderItem();
            testOrderItem.setOrder(order);
            testOrderItem.setOriginalProductId(testProduct.getId());
            testOrderItem.setOriginalVariantId(1L);
            testOrderItem.setProductName("Test Product");
            testOrderItem.setQuantity(1);
            testOrderItem.setUnitPrice(BigDecimal.TEN);
            testOrderItem.setLineTotal(BigDecimal.TEN);
            testOrderItem.setSku("SKU-1");
            testOrderItem.setSize("M");
            testOrderItem.setColor("Black");
            testOrderItem.setProductImageUrl("http://localhost:8080/api/images/product-image.jpg");
            testOrderItem = orderItemRepository.save(testOrderItem);
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            orderItemRepository.deleteAll();
            customerOrderRepository.deleteAll();
            userRepository.deleteAll();
            productRepository.deleteAll();
            categoryRepository.deleteAll();
        });
        
        try {
            Files.deleteIfExists(Paths.get("target/test-migration-report.json"));
        } catch (IOException ignored) {}
    }

    @Test
    void whenDryRun_performsNoDbOrS3Mutations() throws Exception {
        ReflectionTestUtils.setField(runner, "dryRun", true);
        ReflectionTestUtils.setField(runner, "storageType", "local");

        // Run utility
        runner.executeMigration();

        // Verify S3 has NOT received any putObject requests
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Verify DB URLs are untouched
        Product p = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals("http://localhost:8080/api/images/product-image.jpg", p.getImageUrl());

        Category c = categoryRepository.findById(testCategory.getId()).orElseThrow();
        assertEquals("http://localhost:8080/api/images/category-image.png", c.getImageUrl());

        // Check report exists
        assertTrue(Files.exists(Paths.get("target/test-migration-report.json")));
    }

    @Test
    void whenValidState_migratesCorrectlyToS3AndRewritesDB() throws Exception {
        // Setup ETag head responses to throw (Object not exists)
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        // Setup verification headResponse post-upload with matching checksum
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .eTag("\"" + getFileChecksum(img1) + "\"")
                .build();
        when(s3Client.headObject(argThat((HeadObjectRequest r) -> r != null && "images/product-image.jpg".equals(r.key()))))
                .thenThrow(NoSuchKeyException.builder().build()) // first check
                .thenReturn(headResponse); // post-upload verification

        HeadObjectResponse headResponse2 = HeadObjectResponse.builder()
                .eTag("\"" + getFileChecksum(img2) + "\"")
                .build();
        when(s3Client.headObject(argThat((HeadObjectRequest r) -> r != null && "images/category-image.png".equals(r.key()))))
                .thenThrow(NoSuchKeyException.builder().build()) // first check
                .thenReturn(headResponse2); // post-upload verification

        // Run utility
        runner.executeMigration();

        // Verify S3 uploads
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(2)).putObject(requestCaptor.capture(), any(RequestBody.class));

        List<PutObjectRequest> requests = requestCaptor.getAllValues();
        assertTrue(requests.stream().anyMatch(r -> r.key().equals("images/product-image.jpg")));
        assertTrue(requests.stream().anyMatch(r -> r.key().equals("images/category-image.png")));

        // Verify DB URLs rewrote correctly
        Product p = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals("https://cdn.example.com/images/product-image.jpg", p.getImageUrl());

        Category c = categoryRepository.findById(testCategory.getId()).orElseThrow();
        assertEquals("https://cdn.example.com/images/category-image.png", c.getImageUrl());

        // Historical order-item image snapshots MUST remain unchanged
        OrderItem oi = orderItemRepository.findById(testOrderItem.getId()).orElseThrow();
        assertEquals("http://localhost:8080/api/images/product-image.jpg", oi.getProductImageUrl());
    }

    @Test
    void whenRunRepeatedly_isIdempotentAndSkipsS3Uploads() throws Exception {
        // Mock S3 object already exists and has equivalent checksum
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .eTag("\"" + getFileChecksum(img1) + "\"")
                .build();
        HeadObjectResponse headResponse2 = HeadObjectResponse.builder()
                .eTag("\"" + getFileChecksum(img2) + "\"")
                .build();

        when(s3Client.headObject(argThat((HeadObjectRequest r) -> r != null && "images/product-image.jpg".equals(r.key()))))
                .thenReturn(headResponse);
        when(s3Client.headObject(argThat((HeadObjectRequest r) -> r != null && "images/category-image.png".equals(r.key()))))
                .thenReturn(headResponse2);

        // Run first time
        runner.executeMigration();

        // Verify DB updated
        Product p = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals("https://cdn.example.com/images/product-image.jpg", p.getImageUrl());

        // Verify S3 upload did NOT happen because of ETag match
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Run second time (everything already migrated in DB, should skip entirely)
        runner.executeMigration();

        // Still zero uploads
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void whenS3ConfigurationInvalid_throwsException() {
        ReflectionTestUtils.setField(runner, "storageType", "local");
        assertThrows(IllegalStateException.class, () -> runner.executeMigration());

        ReflectionTestUtils.setField(runner, "storageType", "s3");
        ReflectionTestUtils.setField(runner, "migrationConfirm", false);
        assertThrows(IllegalStateException.class, () -> runner.executeMigration());
    }

    @Test
    void whenLocalFileMissing_reportsButDoesNotCorruptDatabase() throws Exception {
        Files.delete(img1); // Delete first file

        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .eTag("\"" + getFileChecksum(img2) + "\"")
                .build();
        when(s3Client.headObject(argThat((HeadObjectRequest r) -> r != null && "images/category-image.png".equals(r.key()))))
                .thenThrow(NoSuchKeyException.builder().build())
                .thenReturn(headResponse);

        // Run
        runner.executeMigration();

        // Verify only 1 upload succeeded (category-image.png)
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Verify DB: category migrated, product remains local since file was missing
        Product p = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals("http://localhost:8080/api/images/product-image.jpg", p.getImageUrl());

        Category c = categoryRepository.findById(testCategory.getId()).orElseThrow();
        assertEquals("https://cdn.example.com/images/category-image.png", c.getImageUrl());
    }

    private void createDummyImage(Path path) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(bufferedImage, "jpg", path.toFile());
    }

    private String getFileChecksum(Path path) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
