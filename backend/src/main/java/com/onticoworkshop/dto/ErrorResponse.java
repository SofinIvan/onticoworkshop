package com.onticoworkshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {

  private int statusCode;

  private String code;

  private String message;

  private String target;

  private java.util.List<String> details;
}
