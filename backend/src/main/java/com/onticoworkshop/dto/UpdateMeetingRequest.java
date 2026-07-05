package com.onticoworkshop.dto;

import java.util.List;

import com.onticoworkshop.model.MeetingTimeRule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeetingRequest {

  private String title;

  private String description;

  private Integer durationMinutes;

  private String timezone;

  private Boolean isActive;

  private Integer slotIntervalMinutes;

  private Integer minimumNoticeMinutes;

  private Integer bufferBeforeMinutes;

  private Integer bufferAfterMinutes;

  private String meetingUrl;

  private List<MeetingTimeRule> timeRules;
}
