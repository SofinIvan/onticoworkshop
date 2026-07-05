package com.onticoworkshop.model;

import jakarta.persistence.Column;
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
@Table(name = "users")
public class User {

  @Id
  private String id;

  @Column(unique = true)
  private String username;

  private String displayName;

  @Column(unique = true)
  private String email;

  private String timezone;

  private String bio;

  private String avatarUrl;

  private String createdAt;

  private String updatedAt;
}
