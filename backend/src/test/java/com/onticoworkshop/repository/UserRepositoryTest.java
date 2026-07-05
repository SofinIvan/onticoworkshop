package com.onticoworkshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.onticoworkshop.BaseRepositoryTest;
import com.onticoworkshop.model.User;

class UserRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  void shouldSaveAndFindByEmail() {
    User user = new User("usr_t1", "testuser", "Test User", "test@example.com",
        "UTC", null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    userRepository.save(user);

    Optional<User> found = userRepository.findByEmail("test@example.com");

    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("testuser");
  }

  @Test
  void shouldSaveAndFindByUsername() {
    User user = new User("usr_t2", "john", "John Doe", "john@example.com",
        "UTC", null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    userRepository.save(user);

    Optional<User> found = userRepository.findByUsername("john");

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("john@example.com");
  }

  @Test
  void shouldReturnEmptyForMissingUser() {
    Optional<User> found = userRepository.findById("usr_nonexistent");

    assertThat(found).isEmpty();
  }

  @Test
  void shouldReturnEmptyForUnknownEmail() {
    Optional<User> found = userRepository.findByEmail("noone@example.com");

    assertThat(found).isEmpty();
  }
}
