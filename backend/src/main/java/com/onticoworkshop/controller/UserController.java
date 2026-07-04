package com.onticoworkshop.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onticoworkshop.dto.CreateUserRequest;
import com.onticoworkshop.dto.UpdateOnlineCallSettingsRequest;
import com.onticoworkshop.dto.UpdateUserRequest;
import com.onticoworkshop.model.OnlineCallSettings;
import com.onticoworkshop.model.User;
import com.onticoworkshop.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<Map<String, Object>> listUsers() {
    return ResponseEntity.ok(Map.of("items", userService.listUsers()));
  }

  @PostMapping
  public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<User> getUser(@PathVariable String userId) {
    return ResponseEntity.ok(userService.getUser(userId));
  }

  @PatchMapping("/{userId}")
  public ResponseEntity<User> updateUser(
      @PathVariable String userId,
      @Valid @RequestBody UpdateUserRequest request) {
    return ResponseEntity.ok(userService.updateUser(userId, request));
  }

  @GetMapping("/{userId}/online-call")
  public ResponseEntity<OnlineCallSettings> getOnlineCallSettings(@PathVariable String userId) {
    return ResponseEntity.ok(userService.getOnlineCallSettings(userId));
  }

  @PatchMapping("/{userId}/online-call")
  public ResponseEntity<OnlineCallSettings> updateOnlineCallSettings(
      @PathVariable String userId,
      @Valid @RequestBody UpdateOnlineCallSettingsRequest request) {
    return ResponseEntity.ok(userService.updateOnlineCallSettings(userId, request));
  }
}
