package com.clothingbrand.ecommerce.bootstrap;

import com.clothingbrand.ecommerce.config.AdminBootstrapProperties;
import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(AdminBootstrapProperties properties, UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            return;
        }

        String email = properties.getEmail();
        String password = properties.getPassword();

        if (email == null || email.isBlank() || password == null || password.isBlank() || password.length() < 8) {
            throw new IllegalStateException("Admin bootstrap is enabled but missing valid ADMIN_EMAIL or ADMIN_PASSWORD. Password must be at least 8 characters.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getRole().getName() == RoleName.ROLE_ADMIN) {
                logger.info("Admin account already exists, skipping bootstrap.");
            } else {
                logger.warn("Cannot bootstrap admin: Email is already registered as a customer.");
            }
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found in the database."));

        User adminUser = new User();
        adminUser.setEmail(normalizedEmail);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setPasswordHash(passwordEncoder.encode(password));
        adminUser.setRole(adminRole);
        adminUser.setActive(true);

        userRepository.save(adminUser);
        
        logger.info("Admin account bootstrapped successfully.");
    }
}
