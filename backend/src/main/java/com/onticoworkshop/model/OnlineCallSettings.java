package com.onticoworkshop.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "online_call_settings")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class OnlineCallSettings {

  @Id
  private String userId;

  private String availabilityScheduleId;

  private String title;

  private String description;

  private int durationMinutes;

  private String timezone;

  @JsonProperty("isActive")
  private boolean isActive;

  private int minimumNoticeMinutes;

  private int slotIntervalMinutes;

  private int bufferBeforeMinutes;

  private int bufferAfterMinutes;

  private String meetingUrl;

  private String createdAt;

  private String updatedAt;
}
