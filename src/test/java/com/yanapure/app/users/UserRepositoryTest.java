package com.yanapure.app.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByPhone() {
        // Given
        User user = new User();
        user.setName("John Doe");
        user.setPhone("+14155552671");
        user.setEmail("john@example.com");
        user.setRole(Role.USER);
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByPhone("+14155552671");

        // Then
        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
        assertEquals("+14155552671", found.get().getPhone());
    }

    @Test
    void testExistsByPhone() {
        // Given
        User user = new User();
        user.setName("Jane Doe");
        user.setPhone("+14155552672");
        user.setRole(Role.USER);
        entityManager.persistAndFlush(user);

        // When & Then
        assertTrue(userRepository.existsByPhone("+14155552672"));
        assertFalse(userRepository.existsByPhone("+14155552673"));
    }

    @Test
    void testFindByEmail() {
        // Given
        User user = new User();
        user.setName("Bob Smith");
        user.setPhone("+14155552673");
        user.setEmail("bob@example.com");
        user.setRole(Role.USER);
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByEmail("bob@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("Bob Smith", found.get().getName());
        assertEquals("bob@example.com", found.get().getEmail());
    }

    @Test
    void testFindByRole() {
        // Given
        User user1 = new User();
        user1.setName("User 1");
        user1.setPhone("+14155552674");
        user1.setRole(Role.USER);
        entityManager.persistAndFlush(user1);

        User admin = new User();
        admin.setName("Admin 1");
        admin.setPhone("+14155552675");
        admin.setRole(Role.ADMIN);
        entityManager.persistAndFlush(admin);

        // When
        List<User> users = userRepository.findByRole(Role.USER);
        List<User> admins = userRepository.findByRole(Role.ADMIN);

        // Then
        assertEquals(1, users.size());
        assertEquals(1, admins.size());
        assertEquals("User 1", users.get(0).getName());
        assertEquals("Admin 1", admins.get(0).getName());
    }

    @Test
    void testCountByRole() {
        // Given
        User user1 = new User();
        user1.setName("User 1");
        user1.setPhone("+14155552676");
        user1.setRole(Role.USER);
        entityManager.persistAndFlush(user1);

        User user2 = new User();
        user2.setName("User 2");
        user2.setPhone("+14155552677");
        user2.setRole(Role.USER);
        entityManager.persistAndFlush(user2);

        User admin = new User();
        admin.setName("Admin 1");
        admin.setPhone("+14155552678");
        admin.setRole(Role.ADMIN);
        entityManager.persistAndFlush(admin);

        // When
        long userCount = userRepository.countByRole(Role.USER);
        long adminCount = userRepository.countByRole(Role.ADMIN);

        // Then
        assertEquals(2, userCount);
        assertEquals(1, adminCount);
    }

    @Test
    void testFindUsersWithEmail() {
        // Given
        User userWithEmail = new User();
        userWithEmail.setName("User With Email");
        userWithEmail.setPhone("+14155552679");
        userWithEmail.setEmail("user@example.com");
        userWithEmail.setRole(Role.USER);
        entityManager.persistAndFlush(userWithEmail);

        User userWithoutEmail = new User();
        userWithoutEmail.setName("User Without Email");
        userWithoutEmail.setPhone("+14155552680");
        userWithoutEmail.setRole(Role.USER);
        entityManager.persistAndFlush(userWithoutEmail);

        // When
        List<User> usersWithEmail = userRepository.findUsersWithEmail();

        // Then
        assertEquals(1, usersWithEmail.size());
        assertEquals("User With Email", usersWithEmail.get(0).getName());
    }
}
