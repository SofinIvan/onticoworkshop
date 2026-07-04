package com.onticoworkshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOnlineCallSettingsRequest {

  private String availabilityScheduleId;

  private String title;

  private String description;

  private Integer durationMinutes;

  private String timezone;

  private Boolean isActive;

  private Integer minimumNoticeMinutes;

  private Integer slotIntervalMinutes;

  private Integer bufferBeforeMinutes;

  private Integer bufferAfterMinutes;

  private String meetingUrl;
}
