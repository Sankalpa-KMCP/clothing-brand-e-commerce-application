package com.clothingbrand.ecommerce.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {

    private S3StorageProperties properties;
    private S3Client s3Client;
    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new S3StorageProperties();
        properties.setEndpoint("http://localhost:9000");
        properties.setRegion("us-east-1");
        properties.setBucketName("test-bucket");
        properties.setAccessKey("access");
        properties.setSecretKey("secret");
        properties.setPublicUrlPrefix("https://cdn.example.com");

        s3Client = mock(S3Client.class);
        storageService = new S3StorageService(properties);
        storageService.setS3Client(s3Client);
    }

    @Test
    void validateProperties_missingValues_throwsIllegalStateException() {
        S3StorageProperties invalidProperties = new S3StorageProperties();
        assertThrows(IllegalStateException.class, invalidProperties::validate);

        invalidProperties.setEndpoint("http://localhost:9000");
        assertThrows(IllegalStateException.class, invalidProperties::validate);
    }

    @Test
    void store_validFile_uploadsToS3AndReturnsUrl() throws Exception {
        byte[] validPngBytes = createValidPngBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", validPngBytes);

        String resultUrl = storageService.store(file);

        // Verify result URL matches custom prefix
        assertTrue(resultUrl.startsWith("https://cdn.example.com/images/"));
        assertTrue(resultUrl.endsWith(".png"));

        // Capture PutObjectRequest
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest putRequest = requestCaptor.getValue();
        assertEquals("test-bucket", putRequest.bucket());
        assertTrue(putRequest.key().startsWith("images/"));
        assertTrue(putRequest.key().endsWith(".png"));
        assertEquals("image/png", putRequest.contentType());
        assertEquals("public, max-age=31536000, immutable", putRequest.cacheControl());
    }

    @Test
    void store_invalidContentType_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Not a real image".getBytes());
        assertThrows(IllegalArgumentException.class, () -> storageService.store(file));
    }

    @Test
    void delete_validFullUrl_deletesCorrectObject() {
        String fullUrl = "https://cdn.example.com/images/abc-123.png";

        storageService.delete(fullUrl);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(requestCaptor.capture());

        DeleteObjectRequest deleteRequest = requestCaptor.getValue();
        assertEquals("test-bucket", deleteRequest.bucket());
        assertEquals("images/abc-123.png", deleteRequest.key());
    }

    @Test
    void delete_filenameOnly_deletesCorrectObject() {
        String filename = "abc-123.png";

        storageService.delete(filename);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(requestCaptor.capture());

        DeleteObjectRequest deleteRequest = requestCaptor.getValue();
        assertEquals("images/abc-123.png", deleteRequest.key());
    }

    private byte[] createValidPngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
                0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08,
                0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0x00, 0x00, 0x00,
                0x0D, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0xDA, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49,
                0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }
}
