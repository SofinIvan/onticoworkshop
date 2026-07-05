package com.onticoworkshop.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.onticoworkshop.exception.GlobalExceptionHandler;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.service.AvailabilityService;

@WebMvcTest({AvailabilityController.class, GlobalExceptionHandler.class})
class AvailabilityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AvailabilityService availabilityService;

  @Test
  void shouldReturn404_whenAvailabilityNotFound() throws Exception {
    when(availabilityService.getAvailability("av_x")).thenThrow(new NotFoundException("Availability not found: av_x"));

    mockMvc.perform(get("/availability/av_x"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldDeleteAvailability() throws Exception {
    mockMvc.perform(delete("/availability/av_1"))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturn404_whenDeleteMissingAvailability() throws Exception {
    doThrow(new NotFoundException("Availability not found: av_x"))
        .when(availabilityService).deleteAvailability("av_x");

    mockMvc.perform(delete("/availability/av_x"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
