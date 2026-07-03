package com.clothingbrand.ecommerce.storage;

import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.security.JwtService;
import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "app.upload.dir=test-uploads",
        "app.upload.base-url=/api/images/"
})
@Transactional
public class AdminImageUploadControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setup() throws IOException {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Setup Admin User
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        User admin = new User();
        admin.setEmail("admin-" + UUID.randomUUID() + "@test.com");
        admin.setFirstName("Test");
        admin.setLastName("Admin");
        admin.setPasswordHash(passwordEncoder.encode("password"));
        admin.setRole(adminRole);
        admin.setActive(true);
        userRepository.save(admin);
        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));

        // Setup Customer User
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        User customer = new User();
        customer.setEmail("customer-" + UUID.randomUUID() + "@test.com");
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        customer.setPasswordHash(passwordEncoder.encode("password"));
        customer.setRole(customerRole);
        customer.setActive(true);
        userRepository.save(customer);
        customerToken = jwtService.generateToken(new UserDetailsImpl(customer));

        // Ensure clean test uploads folder
        FileSystemUtils.deleteRecursively(Paths.get("test-uploads"));
        Files.createDirectories(Paths.get("test-uploads"));
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(Paths.get("test-uploads"));
    }

    @Test
    void uploadImage_NoToken_Returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", createValidPngBytes());
        mockMvc.perform(multipart("/api/admin/images")
                .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadImage_CustomerToken_Returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", createValidPngBytes());
        mockMvc.perform(multipart("/api/admin/images")
                .file(file)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadImage_AdminToken_Success() throws Exception {
        byte[] validPng = createValidPngBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", validPng);
        
        String responseContent = mockMvc.perform(multipart("/api/admin/images")
                .file(file)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").exists())
                .andReturn().getResponse().getContentAsString();

        // Extract filename from returned URL
        String imageUrl = responseContent.substring(responseContent.indexOf("/api/images/") + 12, responseContent.lastIndexOf("\""));
        Path filePath = Paths.get("test-uploads", imageUrl);
        assertTrue(Files.exists(filePath));

        // Verify retrieval of file publicly
        mockMvc.perform(get("/api/images/" + imageUrl))
                .andExpect(status().isOk());
    }

    @Test
    void uploadImage_InvalidImageContent_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "Not a real image content".getBytes());
        mockMvc.perform(multipart("/api/admin/images")
                .file(file)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid image file content."));
    }

    @Test
    void uploadImage_InvalidExtension_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", createValidPngBytes());
        mockMvc.perform(multipart("/api/admin/images")
                .file(file)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only JPEG and PNG images are allowed."));
    }

    private byte[] createValidPngBytes() {
        // Minimum valid 1x1 PNG bytes
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
