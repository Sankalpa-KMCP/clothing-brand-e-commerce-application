package com.clothingbrand.ecommerce.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/images")
public class AdminImageUploadController {

    private final StorageService storageService;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    public AdminImageUploadController(StorageService storageService,
                                      com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.storageService = storageService;
        this.observabilityService = observabilityService;
    }

    @PostMapping
    public ResponseEntity<ImageUploadResponseDto> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = storageService.store(file);
            observabilityService.trackUploadSuccess();
            return new ResponseEntity<>(new ImageUploadResponseDto(imageUrl), HttpStatus.CREATED);
        } catch (RuntimeException ex) {
            observabilityService.trackUploadFailure();
            throw ex;
        }
    }
}
