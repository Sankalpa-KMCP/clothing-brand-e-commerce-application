package com.clothingbrand.ecommerce.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3StorageProperties {
    private String endpoint;
    private String region;
    private String bucketName;
    private String accessKey;
    private String secretKey;
    private String publicUrlPrefix;
    private boolean pathStyleAccess = false;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublicUrlPrefix() {
        return publicUrlPrefix;
    }

    public void setPublicUrlPrefix(String publicUrlPrefix) {
        this.publicUrlPrefix = publicUrlPrefix;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment env;

    @jakarta.annotation.PostConstruct
    public void validate() {
        if (env == null || "s3".equalsIgnoreCase(env.getProperty("app.storage.type"))) {
            if (endpoint == null || endpoint.isBlank()) {
                throw new IllegalStateException("app.storage.s3.endpoint must be configured when storage type is S3");
            }
            if (region == null || region.isBlank()) {
                throw new IllegalStateException("app.storage.s3.region must be configured when storage type is S3");
            }
            if (bucketName == null || bucketName.isBlank()) {
                throw new IllegalStateException("app.storage.s3.bucket-name must be configured when storage type is S3");
            }
            if (accessKey == null || accessKey.isBlank()) {
                throw new IllegalStateException("app.storage.s3.access-key must be configured when storage type is S3");
            }
            if (secretKey == null || secretKey.isBlank()) {
                throw new IllegalStateException("app.storage.s3.secret-key must be configured when storage type is S3");
            }
            if (publicUrlPrefix == null || publicUrlPrefix.isBlank()) {
                throw new IllegalStateException("app.storage.s3.public-url-prefix must be configured when storage type is S3");
            }
            if (java.util.Arrays.asList(env.getActiveProfiles()).contains("prod")) {
                if ("your_s3_access_key".equals(accessKey)) {
                    throw new IllegalStateException("app.storage.s3.access-key cannot use default placeholder in production profile");
                }
                if ("your_s3_secret_key".equals(secretKey)) {
                    throw new IllegalStateException("app.storage.s3.secret-key cannot use default placeholder in production profile");
                }
                if ("my-bucket".equals(bucketName)) {
                    throw new IllegalStateException("app.storage.s3.bucket-name cannot use default placeholder in production profile");
                }
                if (endpoint.contains("<account-id>")) {
                    throw new IllegalStateException("app.storage.s3.endpoint cannot contain placeholder <account-id> in production profile");
                }
            }
        }
    }
}
