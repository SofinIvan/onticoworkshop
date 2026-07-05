package com.onticoworkshop.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onticoworkshop.dto.CancelBookingRequest;
import com.onticoworkshop.dto.RescheduleBookingRequest;
import com.onticoworkshop.exception.GlobalExceptionHandler;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.service.BookingService;

@WebMvcTest({BookingController.class, GlobalExceptionHandler.class})
class BookingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private BookingService bookingService;

  @Test
  void shouldReturnEmptyList_whenNoOrganizerId() throws Exception {
    mockMvc.perform(get("/booking"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test
  void shouldCancelBooking() throws Exception {
    Booking booking = new Booking("bkg_1", "org1", "m_1", "cancelled", "Guest", "guest@test.com",
        "UTC", "2024-06-10T10:00:00Z", "2024-06-10T10:30:00Z", "UTC",
        null, "reason", "2024-06-01T00:00:00Z", null, null,
        "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z");
    when(bookingService.cancelBooking(eq("bkg_1"), any(CancelBookingRequest.class))).thenReturn(booking);

    CancelBookingRequest cancelReq = new CancelBookingRequest("No longer needed");
    mockMvc.perform(post("/booking/bkg_1/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cancelReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"));
  }

  @Test
  void shouldRescheduleBooking() throws Exception {
    Booking booking = new Booking("bkg_2", "org1", "m_1", "confirmed", "Guest", "guest@test.com",
        "UTC", "2024-06-15T14:00:00Z", "2024-06-15T14:30:00Z", "UTC",
        null, null, null, null, "bkg_1",
        "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    when(bookingService.rescheduleBooking(eq("bkg_1"), any(RescheduleBookingRequest.class)))
        .thenReturn(booking);

    RescheduleBookingRequest reschedReq = new RescheduleBookingRequest(
        "2024-06-15T14:00:00Z", "UTC", null);
    mockMvc.perform(post("/booking/bkg_1/reschedule")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reschedReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("bkg_2"));
  }
}
