package com.onticoworkshop.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onticoworkshop.dto.CreateUserRequest;
import com.onticoworkshop.dto.UpdateOnlineCallSettingsRequest;
import com.onticoworkshop.dto.UpdateUserRequest;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.OnlineCallSettings;
import com.onticoworkshop.model.User;
import com.onticoworkshop.repository.OnlineCallSettingsRepository;
import com.onticoworkshop.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final OnlineCallSettingsRepository onlineCallSettingsRepository;

  public List<User> listUsers() {
    return userRepository.findAll();
  }

  public User getUser(String userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found: " + userId));
  }

  public User getUserByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new NotFoundException("User not found: " + username));
  }

  @Transactional
  public User createUser(CreateUserRequest request) {
    String now = Instant.now().toString();
    User user = new User(
        "usr_" + UUID.randomUUID().toString().substring(0, 8),
        request.getUsername(),
        request.getDisplayName(),
        request.getEmail(),
        request.getTimezone(),
        request.getBio(),
        request.getAvatarUrl(),
        now,
        now
    );
    return userRepository.save(user);
  }

  @Transactional
  public User updateUser(String userId, UpdateUserRequest request) {
    User user = getUser(userId);
    if (request.getUsername() != null) user.setUsername(request.getUsername());
    if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
    if (request.getEmail() != null) user.setEmail(request.getEmail());
    if (request.getTimezone() != null) user.setTimezone(request.getTimezone());
    if (request.getBio() != null) user.setBio(request.getBio());
    if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
    user.setUpdatedAt(Instant.now().toString());
    return userRepository.save(user);
  }

  public OnlineCallSettings getOnlineCallSettings(String userId) {
    return onlineCallSettingsRepository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundException("Online call settings not found for user: " + userId));
  }

  @Transactional
  public OnlineCallSettings updateOnlineCallSettings(String userId, UpdateOnlineCallSettingsRequest request) {
    OnlineCallSettings settings = getOnlineCallSettings(userId);
    if (request.getAvailabilityScheduleId() != null)
      settings.setAvailabilityScheduleId(request.getAvailabilityScheduleId());
    if (request.getTitle() != null) settings.setTitle(request.getTitle());
    if (request.getDescription() != null) settings.setDescription(request.getDescription());
    if (request.getDurationMinutes() != null) settings.setDurationMinutes(request.getDurationMinutes());
    if (request.getTimezone() != null) settings.setTimezone(request.getTimezone());
    if (request.getIsActive() != null) settings.setActive(request.getIsActive());
    if (request.getMinimumNoticeMinutes() != null)
      settings.setMinimumNoticeMinutes(request.getMinimumNoticeMinutes());
    if (request.getSlotIntervalMinutes() != null)
      settings.setSlotIntervalMinutes(request.getSlotIntervalMinutes());
    if (request.getBufferBeforeMinutes() != null)
      settings.setBufferBeforeMinutes(request.getBufferBeforeMinutes());
    if (request.getBufferAfterMinutes() != null)
      settings.setBufferAfterMinutes(request.getBufferAfterMinutes());
    if (request.getMeetingUrl() != null) settings.setMeetingUrl(request.getMeetingUrl());
    settings.setUpdatedAt(Instant.now().toString());
    return onlineCallSettingsRepository.save(settings);
  }
}
