package com.clothingbrand.ecommerce.domain.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testFlywayV2_insertsReferenceRoles() {
        Optional<Role> customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER);
        Optional<Role> adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN);

        assertThat(customerRole).isPresent();
        assertThat(adminRole).isPresent();
    }

    @Test
    void testUserPersistence_withValidRole() {
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("dummy_hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(customerRole);

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getPasswordHash()).isEqualTo("dummy_hash");
        assertThat(savedUser.getActive()).isTrue();

        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getRole().getName()).isEqualTo(RoleName.ROLE_CUSTOMER);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void testUniqueEmailConstraint() {
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

        User user1 = new User();
        user1.setEmail("duplicate@example.com");
        user1.setPasswordHash("hash1");
        user1.setFirstName("First");
        user1.setLastName("One");
        user1.setRole(customerRole);
        userRepository.saveAndFlush(user1);

        User user2 = new User();
        user2.setEmail("duplicate@example.com");
        user2.setPasswordHash("hash2");
        user2.setFirstName("Second");
        user2.setLastName("Two");
        user2.setRole(customerRole);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
