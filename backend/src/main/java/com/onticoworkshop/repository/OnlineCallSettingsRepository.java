package com.onticoworkshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onticoworkshop.model.OnlineCallSettings;

public interface OnlineCallSettingsRepository extends JpaRepository<OnlineCallSettings, String> {

  Optional<OnlineCallSettings> findByUserId(String userId);
}
