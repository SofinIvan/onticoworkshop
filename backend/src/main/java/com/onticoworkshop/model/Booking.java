package com.onticoworkshop.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "bookings")
public class Booking {

  @Id
  private String id;

  private String userId;

  private String status;

  private String guestName;

  private String guestEmail;

  private String guestTimezone;

  private String start;

  private String end;

  private String timezone;

  private String meetingUrl;

  private String notes;

  private String cancellationReason;

  private String cancelledAt;

  private String rescheduledFromBookingId;

  private String createdAt;

  private String updatedAt;

  @JsonIgnore
  public String getGuestName() { return guestName; }

  @JsonIgnore
  public String getGuestEmail() { return guestEmail; }

  @JsonIgnore
  public String getGuestTimezone() { return guestTimezone; }

  @JsonProperty("guest")
  public Map<String, String> getGuest() {
    return Map.of("name", guestName, "email", guestEmail, "timezone", guestTimezone);
  }
}
