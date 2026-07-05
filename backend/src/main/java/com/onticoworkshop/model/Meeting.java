package com.onticoworkshop.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meetings")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Meeting {

  @Id
  private String id;

  private String organizerId;

  private String title;

  private String description;

  private int durationMinutes;

  private String timezone;

  @JsonProperty("isActive")
  private boolean isActive;

  private int slotIntervalMinutes;

  private int minimumNoticeMinutes;

  private int bufferBeforeMinutes;

  private int bufferAfterMinutes;

  private String meetingUrl;

  private String uuid;

  private String createdAt;

  private String updatedAt;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "meeting_id")
  private List<MeetingTimeRule> timeRules = new ArrayList<>();
}
