package com.onticoworkshop.controller;

import static org.mockito.Mockito.when;
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
import com.onticoworkshop.service.BookingService;
import com.onticoworkshop.service.MeetingService;
import com.onticoworkshop.service.UserService;

@WebMvcTest({PublicController.class, GlobalExceptionHandler.class})
class PublicControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MeetingService meetingService;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private BookingService bookingService;

  @Test
  void shouldReturn404_whenMeetingByUuidNotFound() throws Exception {
    when(meetingService.getMeetingByUuid("bad-uuid"))
        .thenThrow(new NotFoundException("Meeting not found for uuid: bad-uuid"));

    mockMvc.perform(get("/public/meeting").param("uuid", "bad-uuid"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
