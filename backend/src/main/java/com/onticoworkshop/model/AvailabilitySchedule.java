package com.onticoworkshop.model;

import java.util.ArrayList;
import java.util.List;

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
@Table(name = "availability_schedules")
public class AvailabilitySchedule {

  @Id
  private String id;

  private String userId;

  private String name;

  private String timezone;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "schedule_id")
  private List<AvailabilityRule> rules = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "schedule_id")
  private List<DateOverride> dateOverrides = new ArrayList<>();

  private String createdAt;

  private String updatedAt;
}
