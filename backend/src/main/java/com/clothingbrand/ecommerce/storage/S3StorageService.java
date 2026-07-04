package com.clothingbrand.ecommerce.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3StorageProperties properties;
    private S3Client s3Client;

    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png");
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");

    public S3StorageService(S3StorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        properties.validate();

        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .region(Region.of(properties.getRegion()));

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        if (properties.isPathStyleAccess()) {
            builder.serviceConfiguration(conf -> conf.pathStyleAccessEnabled(true));
        }

        this.s3Client = builder.build();

        // Startup connection and bucket validation
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucketName()).build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed S3 bucket configuration check for: " + properties.getBucketName(), e);
        }
    }

    // Constructor injection for testing
    protected void setS3Client(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG and PNG images are allowed.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG and PNG images are allowed.");
        }

        // Validate actual image content
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new IllegalArgumentException("Invalid image file content.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to validate image content.", e);
        }

        String newFilename = UUID.randomUUID().toString() + "." + extension;
        String objectKey = "images/" + newFilename;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .contentType(contentType)
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to S3.", e);
        }

        String prefix = properties.getPublicUrlPrefix();
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix + objectKey;
    }

    @Override
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }

        String cleanFilename = filename;
        String prefix = properties.getPublicUrlPrefix();
        if (cleanFilename.startsWith(prefix)) {
            cleanFilename = cleanFilename.substring(prefix.length());
        }

        if (cleanFilename.startsWith("/")) {
            cleanFilename = cleanFilename.substring(1);
        }

        String objectKey = cleanFilename;
        if (!objectKey.startsWith("images/")) {
            objectKey = "images/" + objectKey;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            // Log warning only, do not fail operation (matches LocalDiskStorageService behavior)
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
