package com.onticoworkshop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.onticoworkshop.BaseIntegrationTest;
import com.onticoworkshop.dto.CreateUserRequest;
import com.onticoworkshop.dto.UpdateUserRequest;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.User;
import com.onticoworkshop.service.UserService;

class UserServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private UserService userService;

  @Test
  void shouldCreateAndRetrieveUser() {
    CreateUserRequest request = new CreateUserRequest("dave", "Dave Smith", "dave@test.com",
        "UTC", "Bio here", "https://avatar.url");
    User created = userService.createUser(request);

    assertThat(created.getId()).startsWith("usr_");
    assertThat(created.getUsername()).isEqualTo("dave");
    assertThat(created.getEmail()).isEqualTo("dave@test.com");
    assertThat(created.getBio()).isEqualTo("Bio here");

    User found = userService.getUser(created.getId());
    assertThat(found.getDisplayName()).isEqualTo("Dave Smith");
  }

  @Test
  void shouldUpdateUserPartially() {
    CreateUserRequest createReq = new CreateUserRequest("eve", "Eve", "eve@test.com",
        "UTC", null, null);
    User created = userService.createUser(createReq);

    UpdateUserRequest updateReq = new UpdateUserRequest();
    updateReq.setDisplayName("Eve Updated");
    updateReq.setBio("New bio");

    User updated = userService.updateUser(created.getId(), updateReq);

    assertThat(updated.getDisplayName()).isEqualTo("Eve Updated");
    assertThat(updated.getBio()).isEqualTo("New bio");
    assertThat(updated.getUsername()).isEqualTo("eve");
  }

  @Test
  void shouldListUsers() {
    CreateUserRequest req1 = new CreateUserRequest("frank", "Frank", "frank@test.com",
        "UTC", null, null);
    CreateUserRequest req2 = new CreateUserRequest("grace", "Grace", "grace@test.com",
        "UTC", null, null);
    userService.createUser(req1);
    userService.createUser(req2);

    List<User> users = userService.listUsers();

    assertThat(users).hasSizeGreaterThanOrEqualTo(2);
    assertThat(users).extracting("username").contains("frank", "grace");
  }

  @Test
  void shouldThrowNotFoundForMissingUser() {
    assertThatThrownBy(() -> userService.getUser("usr_nonexistent"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void shouldGetUserByUsername() {
    CreateUserRequest request = new CreateUserRequest("hank", "Hank", "hank@test.com",
        "UTC", null, null);
    userService.createUser(request);

    User found = userService.getUserByUsername("hank");

    assertThat(found.getEmail()).isEqualTo("hank@test.com");
  }
}
