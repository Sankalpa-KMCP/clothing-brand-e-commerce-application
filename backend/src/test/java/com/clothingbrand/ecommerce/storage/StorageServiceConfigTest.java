package com.clothingbrand.ecommerce.storage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class StorageServiceConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration.class
            ))
            .withUserConfiguration(
                    S3StorageProperties.class,
                    LocalDiskStorageService.class,
                    S3StorageService.class
            );

    @Test
    void whenStorageTypeMissing_thenLocalDiskStorageServiceIsDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(StorageService.class);
            assertThat(context).hasSingleBean(LocalDiskStorageService.class);
            assertThat(context).doesNotHaveBean(S3StorageService.class);
        });
    }

    @Test
    void whenStorageTypeIsLocal_thenLocalDiskStorageServiceIsSelected() {
        contextRunner.withPropertyValues("app.storage.type=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(StorageService.class);
                    assertThat(context).hasSingleBean(LocalDiskStorageService.class);
                    assertThat(context).doesNotHaveBean(S3StorageService.class);
                });
    }

    @Test
    void whenStorageTypeIsS3_thenS3StorageServiceIsSelectedAndValidates() {
        contextRunner.withPropertyValues(
                "app.storage.type=s3",
                "app.storage.s3.endpoint=http://localhost:9000",
                "app.storage.s3.region=us-east-1",
                "app.storage.s3.bucket-name=test-bucket",
                "app.storage.s3.access-key=access",
                "app.storage.s3.secret-key=secret",
                "app.storage.s3.public-url-prefix=https://cdn.example.com"
        ).run(context -> {
            // S3StorageService is selected and instantiated. S3 connection validation runs headBucket,
            // which fails on real connection since no MinIO is running, confirming that validation runs fail-fast.
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure().getCause()).hasMessageContaining("Failed S3 bucket configuration check for: test-bucket");
        });
    }
}
