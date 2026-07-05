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
import com.onticoworkshop.service.MeetingService;

@WebMvcTest({MeetingController.class, GlobalExceptionHandler.class})
class MeetingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MeetingService meetingService;

  @Test
  void shouldReturn404_whenMeetingNotFound() throws Exception {
    when(meetingService.getMeeting("m_x")).thenThrow(new NotFoundException("Meeting not found: m_x"));

    mockMvc.perform(get("/meeting/m_x"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldDeleteMeeting() throws Exception {
    mockMvc.perform(delete("/meeting/m_1"))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturn404_whenDeleteMissingMeeting() throws Exception {
    doThrow(new NotFoundException("Meeting not found: m_x")).when(meetingService).deleteMeeting("m_x");

    mockMvc.perform(delete("/meeting/m_x"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
