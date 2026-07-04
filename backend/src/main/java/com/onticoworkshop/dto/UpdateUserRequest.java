package com.onticoworkshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

  private String username;

  private String displayName;

  private String email;

  private String timezone;

  private String bio;

  private String avatarUrl;
}
