package com.onticoworkshop.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onticoworkshop.dto.CreateUserRequest;
import com.onticoworkshop.dto.UpdateUserRequest;
import com.onticoworkshop.model.User;
import com.onticoworkshop.repository.UserRepository;
import com.onticoworkshop.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User buildUser(String id, String username) {
    return new User(id, username, "Display " + username, username + "@test.com",
        "UTC", null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
  }

  @Test
  void createUser_shouldSaveAndReturnUser() {
    CreateUserRequest request = new CreateUserRequest("charlie", "Charlie", "charlie@test.com", "UTC", null, null);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.createUser(request);

    assertThat(result.getId()).startsWith("usr_");
    assertThat(result.getUsername()).isEqualTo("charlie");
    assertThat(result.getEmail()).isEqualTo("charlie@test.com");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void updateUser_shouldPartiallyUpdateFields() {
    User existing = buildUser("usr_1", "alice");
    when(userRepository.findById("usr_1")).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setDisplayName("Alice Updated");

    User result = userService.updateUser("usr_1", request);

    assertThat(result.getDisplayName()).isEqualTo("Alice Updated");
    assertThat(result.getUsername()).isEqualTo("alice");
  }
}
