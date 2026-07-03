package com.clothingbrand.ecommerce.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalDiskStorageService implements StorageService {

    private final Path rootLocation;
    private final String baseUrl;

    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png");
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");

    public LocalDiskStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.upload.base-url:http://localhost:8080/api/images/}") String baseUrl
    ) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location: " + uploadDir, e);
        }
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

        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.rootLocation, e);
        }

        String newFilename = UUID.randomUUID().toString() + "." + extension;
        Path destinationFile = this.rootLocation.resolve(newFilename).normalize();

        // Extra check for path traversal
        if (!destinationFile.getParent().equals(this.rootLocation)) {
            throw new IllegalArgumentException("Cannot store file outside current directory.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }

        return this.baseUrl + newFilename;
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = this.rootLocation.resolve(filename).normalize();
            if (file.getParent().equals(this.rootLocation)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            // Log warning, do not fail operation
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
