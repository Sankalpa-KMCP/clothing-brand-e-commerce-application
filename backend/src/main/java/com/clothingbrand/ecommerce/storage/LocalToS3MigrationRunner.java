package com.clothingbrand.ecommerce.storage;

import com.clothingbrand.ecommerce.domain.catalog.Category;
import com.clothingbrand.ecommerce.domain.catalog.CategoryRepository;
import com.clothingbrand.ecommerce.domain.catalog.Product;
import com.clothingbrand.ecommerce.domain.catalog.ProductRepository;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.migration.local-to-s3.enabled", havingValue = "true")
public class LocalToS3MigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalToS3MigrationRunner.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final S3StorageProperties s3Properties;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/api/images/}")
    private String oldLocalPrefix;

    @Value("${app.migration.local-to-s3.confirm:false}")
    private boolean migrationConfirm;

    @Value("${app.migration.local-to-s3.dry-run:true}")
    private boolean dryRun;

    @Value("${app.migration.local-to-s3.report-path:migration-report.json}")
    private String reportPath;

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.migration.local-to-s3.auto-start:false}")
    private boolean autoStart;

    private S3Client s3Client;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    public LocalToS3MigrationRunner(ProductRepository productRepository,
                                    CategoryRepository categoryRepository,
                                    S3StorageProperties s3Properties,
                                    ObjectMapper objectMapper,
                                    com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.s3Properties = s3Properties;
        this.objectMapper = objectMapper;
        this.observabilityService = observabilityService;
    }

    // Setter for testing mocks
    protected void setS3Client(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!autoStart) {
            log.info("Local-to-S3 Image Migration Runner is enabled but auto-start is false. Skipping execution on startup.");
            return;
        }
        executeMigration();
    }

    public void executeMigration() throws Exception {
        log.info("=== STARTING LOCAL-TO-S3 IMAGE MIGRATION UTILITY ===");
        log.info("Dry-run mode: {}", dryRun);
        log.info("Migration confirm flag: {}", migrationConfirm);

        // 1. Safety Gates Validation
        validateSafetyGates();

        // 2. Initialize S3 Client if not mocked
        if (s3Client == null && !dryRun) {
            initS3Client();
        }

        // 3. Scan & Analyze Local Directory & Database
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        List<Product> products = productRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        List<MigrationItem> migrationItems = new ArrayList<>();
        Set<String> referencedFilenames = new HashSet<>();

        // Statistics
        int scannedReferences = 0;
        int alreadyMigratedRows = 0;
        int missingLocalFiles = 0;
        int invalidFiles = 0;
        int malformedOrExternalUrls = 0;
        int duplicateReferences = 0;
        int successUploads = 0;
        int failedUploads = 0;
        int successDbUpdates = 0;
        int failedDbUpdates = 0;

        // Process Products
        for (Product product : products) {
            String url = product.getImageUrl();
            if (url == null || url.isBlank()) {
                continue;
            }
            scannedReferences++;

            if (url.startsWith(s3Properties.getPublicUrlPrefix())) {
                alreadyMigratedRows++;
                continue;
            }

            if (!url.startsWith(oldLocalPrefix)) {
                malformedOrExternalUrls++;
                continue;
            }

            String filename = url.substring(oldLocalPrefix.length());
            Path localFile = uploadPath.resolve(filename).normalize().toAbsolutePath();

            if (referencedFilenames.contains(filename)) {
                duplicateReferences++;
            }
            referencedFilenames.add(filename);

            if (!localFile.getParent().equals(uploadPath)) {
                malformedOrExternalUrls++;
                continue;
            }

            if (!Files.exists(localFile)) {
                missingLocalFiles++;
                migrationItems.add(new MigrationItem("product", product.getId(), url, null, "MISSING_LOCAL_FILE", null));
                continue;
            }

            if (!isValidImage(localFile)) {
                invalidFiles++;
                migrationItems.add(new MigrationItem("product", product.getId(), url, null, "INVALID_FILE_CONTENT", null));
                continue;
            }

            migrationItems.add(new MigrationItem("product", product.getId(), url, filename, "PENDING", localFile));
        }

        // Process Categories
        for (Category category : categories) {
            String url = category.getImageUrl();
            if (url == null || url.isBlank()) {
                continue;
            }
            scannedReferences++;

            if (url.startsWith(s3Properties.getPublicUrlPrefix())) {
                alreadyMigratedRows++;
                continue;
            }

            if (!url.startsWith(oldLocalPrefix)) {
                malformedOrExternalUrls++;
                continue;
            }

            String filename = url.substring(oldLocalPrefix.length());
            Path localFile = uploadPath.resolve(filename).normalize().toAbsolutePath();

            if (referencedFilenames.contains(filename)) {
                duplicateReferences++;
            }
            referencedFilenames.add(filename);

            if (!localFile.getParent().equals(uploadPath)) {
                malformedOrExternalUrls++;
                continue;
            }

            if (!Files.exists(localFile)) {
                missingLocalFiles++;
                migrationItems.add(new MigrationItem("category", category.getId(), url, null, "MISSING_LOCAL_FILE", null));
                continue;
            }

            if (!isValidImage(localFile)) {
                invalidFiles++;
                migrationItems.add(new MigrationItem("category", category.getId(), url, null, "INVALID_FILE_CONTENT", null));
                continue;
            }

            migrationItems.add(new MigrationItem("category", category.getId(), url, filename, "PENDING", localFile));
        }

        // Identify Orphans
        List<String> orphanLocalFiles = new ArrayList<>();
        if (Files.exists(uploadPath)) {
            try (var stream = Files.list(uploadPath)) {
                List<Path> allFiles = stream.filter(Files::isRegularFile).collect(Collectors.toList());
                for (Path file : allFiles) {
                    String filename = file.getFileName().toString();
                    if (!referencedFilenames.contains(filename)) {
                        orphanLocalFiles.add(filename);
                    }
                }
            }
        }

        log.info("Scanned references: {}", scannedReferences);
        log.info("Already migrated: {}", alreadyMigratedRows);
        log.info("Missing local files: {}", missingLocalFiles);
        log.info("Invalid files: {}", invalidFiles);
        log.info("Malformed or external URLs: {}", malformedOrExternalUrls);
        log.info("Orphan local files: {}", orphanLocalFiles.size());

        // 4. Perform Uploads & Updates if NOT Dry Run
        for (MigrationItem item : migrationItems) {
            if (!"PENDING".equals(item.status)) {
                continue;
            }

            String targetKey = "images/" + item.filename;
            String newUrl = s3Properties.getPublicUrlPrefix() + (s3Properties.getPublicUrlPrefix().endsWith("/") ? "" : "/") + targetKey;
            item.targetUrl = newUrl;

            if (dryRun) {
                item.status = "PROPOSED_MIGRATION";
                continue;
            }

            try {
                String localChecksum = getFileChecksum(item.localPath);
                boolean skipUpload = false;

                // Idempotent Check: Verify if S3 object exists with equivalent checksum
                try {
                    HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                            .bucket(s3Properties.getBucketName())
                            .key(targetKey)
                            .build());

                    String s3ETag = head.eTag().replaceAll("\"", "");
                    if (localChecksum.equals(s3ETag)) {
                        log.info("S3 object already exists and checksum matches for key: {}. Skipping upload.", targetKey);
                        skipUpload = true;
                    }
                } catch (Exception ignored) {
                    // Object does not exist, proceed to upload
                }

                if (!skipUpload) {
                    String contentType = Files.probeContentType(item.localPath);
                    if (contentType == null) {
                        contentType = item.filename.endsWith(".png") ? "image/png" : "image/jpeg";
                    }

                    s3Client.putObject(PutObjectRequest.builder()
                                    .bucket(s3Properties.getBucketName())
                                    .key(targetKey)
                                    .contentType(contentType)
                                    .cacheControl("public, max-age=31536000, immutable")
                                    .build(),
                            RequestBody.fromFile(item.localPath));

                    // Verification Check: verify checksum post-upload
                    HeadObjectResponse verifyHead = s3Client.headObject(HeadObjectRequest.builder()
                            .bucket(s3Properties.getBucketName())
                            .key(targetKey)
                            .build());
                    String verifiedETag = verifyHead.eTag().replaceAll("\"", "");
                    if (!localChecksum.equals(verifiedETag)) {
                        throw new IllegalStateException("Post-upload checksum verification failed!");
                    }
                }

                successUploads++;

                // Update Database
                try {
                    if ("product".equals(item.entityType)) {
                        Product p = productRepository.findById(item.entityId).orElseThrow();
                        p.setImageUrl(newUrl);
                        productRepository.saveAndFlush(p);
                    } else if ("category".equals(item.entityType)) {
                        Category c = categoryRepository.findById(item.entityId).orElseThrow();
                        c.setImageUrl(newUrl);
                        categoryRepository.saveAndFlush(c);
                    }
                    successDbUpdates++;
                    item.status = "SUCCESS";
                } catch (Exception e) {
                    log.error("Database update failed for {} id: {}", item.entityType, item.entityId, e);
                    failedDbUpdates++;
                    item.status = "DATABASE_UPDATE_FAILED";
                }

            } catch (Exception e) {
                log.error("Upload failed for file: {}", item.filename, e);
                failedUploads++;
                item.status = "UPLOAD_FAILED";
            }
        }

        // 5. Generate Report
        MigrationReport report = new MigrationReport(
                dryRun,
                scannedReferences,
                alreadyMigratedRows,
                missingLocalFiles,
                invalidFiles,
                malformedOrExternalUrls,
                duplicateReferences,
                successUploads,
                failedUploads,
                successDbUpdates,
                failedDbUpdates,
                orphanLocalFiles.size(),
                migrationItems,
                orphanLocalFiles
        );

        Path reportFile = Paths.get(reportPath).toAbsolutePath().normalize();
        Files.createDirectories(reportFile.getParent());
        Files.writeString(reportFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        log.info("Migration report generated at: {}", reportFile);

        // 6. Fail startup if any failures occurred during real run
        if (dryRun) {
            observabilityService.trackMigrationDryRun();
        } else {
            if (failedUploads > 0 || failedDbUpdates > 0) {
                log.error("Migration completed with failures! Upload failures: {}, DB update failures: {}", failedUploads, failedDbUpdates);
                observabilityService.trackMigrationWriteFailure();
                throw new RuntimeException("Migration utility completed with failures!");
            } else {
                observabilityService.trackMigrationWriteSuccess();
            }
        }

        log.info("=== LOCAL-TO-S3 IMAGE MIGRATION UTILITY FINISHED ===");
    }

    private void validateSafetyGates() {
        if (!dryRun) {
            if (!"s3".equalsIgnoreCase(storageType)) {
                throw new IllegalStateException("app.storage.type must be set to 's3' to perform migration.");
            }

            s3Properties.validate();

            String envConfirm = System.getenv("LOCAL_TO_S3_MIGRATION_CONFIRM");
            if (!migrationConfirm && !"true".equalsIgnoreCase(envConfirm)) {
                throw new IllegalStateException("Migration requires confirmation via environment variable LOCAL_TO_S3_MIGRATION_CONFIRM=true or app.migration.local-to-s3.confirm=true.");
            }
        } else {
            if (s3Properties.getPublicUrlPrefix() == null || s3Properties.getPublicUrlPrefix().isBlank()) {
                throw new IllegalStateException("app.storage.s3.public-url-prefix must be configured even for dry-run to map target URLs.");
            }
        }

        if (uploadDir == null || uploadDir.isBlank()) {
            throw new IllegalStateException("app.upload.dir must be configured.");
        }

        if (oldLocalPrefix == null || oldLocalPrefix.isBlank()) {
            throw new IllegalStateException("app.upload.base-url must be configured.");
        }
    }

    private void initS3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey())))
                .region(Region.of(s3Properties.getRegion()));

        if (s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        if (s3Properties.isPathStyleAccess()) {
            builder.serviceConfiguration(conf -> conf.pathStyleAccessEnabled(true));
        }

        this.s3Client = builder.build();

        // Check bucket connection
        s3Client.headBucket(HeadBucketRequest.builder().bucket(s3Properties.getBucketName()).build());
    }

    private boolean isValidImage(Path localFile) {
        String filename = localFile.getFileName().toString().toLowerCase();
        if (!filename.endsWith(".jpg") && !filename.endsWith(".jpeg") && !filename.endsWith(".png")) {
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(localFile.toFile());
            return image != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String getFileChecksum(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
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
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }

    public static class MigrationItem {
        public String entityType;
        public Long entityId;
        public String oldUrl;
        public String filename;
        public String targetUrl;
        public String status;
        
        // Exclude local path from serialization for security & clean reports
        private transient Path localPath;

        public MigrationItem() {}

        public MigrationItem(String entityType, Long entityId, String oldUrl, String filename, String status, Path localPath) {
            this.entityType = entityType;
            this.entityId = entityId;
            this.oldUrl = oldUrl;
            this.filename = filename;
            this.status = status;
            this.localPath = localPath;
        }
    }

    public static class MigrationReport {
        public boolean dryRun;
        public int scannedReferences;
        public int alreadyMigratedRows;
        public int missingLocalFiles;
        public int invalidFiles;
        public int malformedOrExternalUrls;
        public int duplicateReferences;
        public int successUploads;
        public int failedUploads;
        public int successDbUpdates;
        public int failedDbUpdates;
        public int orphanLocalFilesCount;
        public List<MigrationItem> items;
        public List<String> orphanLocalFiles;

        public MigrationReport() {}

        public MigrationReport(boolean dryRun, int scannedReferences, int alreadyMigratedRows, int missingLocalFiles,
                               int invalidFiles, int malformedOrExternalUrls, int duplicateReferences,
                               int successUploads, int failedUploads, int successDbUpdates, int failedDbUpdates,
                               int orphanLocalFilesCount, List<MigrationItem> items, List<String> orphanLocalFiles) {
            this.dryRun = dryRun;
            this.scannedReferences = scannedReferences;
            this.alreadyMigratedRows = alreadyMigratedRows;
            this.missingLocalFiles = missingLocalFiles;
            this.invalidFiles = invalidFiles;
            this.malformedOrExternalUrls = malformedOrExternalUrls;
            this.duplicateReferences = duplicateReferences;
            this.successUploads = successUploads;
            this.failedUploads = failedUploads;
            this.successDbUpdates = successDbUpdates;
            this.failedDbUpdates = failedDbUpdates;
            this.orphanLocalFilesCount = orphanLocalFilesCount;
            this.items = items;
            this.orphanLocalFiles = orphanLocalFiles;
        }
    }
}
