package com.onticoworkshop.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onticoworkshop.BaseIntegrationTest;
import com.onticoworkshop.dto.CancelBookingRequest;
import com.onticoworkshop.dto.CreateAvailabilityRequest;
import com.onticoworkshop.dto.CreateBookingRequest;
import com.onticoworkshop.dto.CreateMeetingRequest;
import com.onticoworkshop.dto.CreateUserRequest;
import com.onticoworkshop.dto.RescheduleBookingRequest;
import com.onticoworkshop.model.AvailabilityRule;

class BookingServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldCreateBooking() throws Exception {
    String userId = createTestUser("alice_booker");
    String meetingId = createTestMeeting(userId, "Alice Meeting");
    createTestAvailability(userId);

    CreateBookingRequest request = new CreateBookingRequest(
        "alice_booker", meetingId, null, "Bob Guest", "bob@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);

    mockMvc.perform(post("/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.status").value("confirmed"))
        .andExpect(jsonPath("$.guest.email").value("bob@test.com"));
  }

  @Test
  void shouldPreventDuplicateBooking() throws Exception {
    String userId = createTestUser("dupe_booker");
    String meetingId = createTestMeeting(userId, "Dup Meeting");
    createTestAvailability(userId);

    CreateBookingRequest request = new CreateBookingRequest(
        "dupe_booker", meetingId, null, "Charlie", "charlie@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);

    mockMvc.perform(post("/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void shouldCancelBooking() throws Exception {
    String userId = createTestUser("cancel_booker");
    String meetingId = createTestMeeting(userId, "Cancel Meeting");
    createTestAvailability(userId);

    CreateBookingRequest request = new CreateBookingRequest(
        "cancel_booker", meetingId, null, "Diana", "diana@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);

    String response = mockMvc.perform(post("/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String bookingId = objectMapper.readTree(response).get("id").asText();

    CancelBookingRequest cancelReq = new CancelBookingRequest("Schedule changed");
    mockMvc.perform(post("/booking/" + bookingId + "/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cancelReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"))
        .andExpect(jsonPath("$.cancellationReason").value("Schedule changed"));
  }

  @Test
  void shouldThrowWhenCancellingAlreadyCancelled() throws Exception {
    String userId = createTestUser("cancel2_booker");
    String meetingId = createTestMeeting(userId, "Cancel2 Meeting");
    createTestAvailability(userId);

    CreateBookingRequest request = new CreateBookingRequest(
        "cancel2_booker", meetingId, null, "Eve", "eve@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);

    String response = mockMvc.perform(post("/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String bookingId = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(post("/booking/" + bookingId + "/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CancelBookingRequest("reason"))))
        .andExpect(status().isOk());

    mockMvc.perform(post("/booking/" + bookingId + "/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CancelBookingRequest())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void shouldRescheduleBooking() throws Exception {
    String userId = createTestUser("resched_booker");
    String meetingId = createTestMeeting(userId, "Resched Meeting");
    createTestAvailability(userId);

    CreateBookingRequest request = new CreateBookingRequest(
        "resched_booker", meetingId, null, "Frank", "frank@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);

    String response = mockMvc.perform(post("/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String bookingId = objectMapper.readTree(response).get("id").asText();

    RescheduleBookingRequest reschedReq = new RescheduleBookingRequest(
        "2024-07-11T14:00:00Z", "UTC", "Moved to afternoon");
    mockMvc.perform(post("/booking/" + bookingId + "/reschedule")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reschedReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("confirmed"))
        .andExpect(jsonPath("$.rescheduledFromBookingId").value(bookingId));
  }

  @Test
  void shouldGetBookingInfo() throws Exception {
    String userId = createTestUser("info_booker");
    createTestMeeting(userId, "Info Meeting");

    mockMvc.perform(get("/booking/new").param("username", "info_booker"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profile.username").value("info_booker"))
        .andExpect(jsonPath("$.meetings").isArray());
  }

  @Test
  void shouldListSlotsWithAvailability() throws Exception {
    String userId = createTestUser("slots_booker");
    String meetingId = createTestMeeting(userId, "Slots Meeting");
    createTestAvailability(userId);

    LocalDate monday = LocalDate.now()
        .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));

    mockMvc.perform(get("/booking/slots")
            .param("username", "slots_booker")
            .param("meetingId", meetingId)
            .param("startDate", monday.toString())
            .param("endDate", monday.plusDays(2).toString())
            .param("timezone", "UTC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());
  }

  @Test
  void shouldGetPublicMeetingByUuid() throws Exception {
    String userId = createTestUser("public_org");
    String meetingId = createTestMeeting(userId, "Public Meeting");

    String meetingResponse = mockMvc.perform(get("/meeting/" + meetingId))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    String uuid = objectMapper.readTree(meetingResponse).get("uuid").asText();

    mockMvc.perform(get("/public/meeting").param("uuid", uuid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meeting.title").value("Public Meeting"))
        .andExpect(jsonPath("$.profile").exists());
  }

  @Test
  void shouldGetPublicSlots() throws Exception {
    String userId = createTestUser("pub_slots_org");
    String meetingId = createTestMeeting(userId, "PubSlots Meeting");
    createTestAvailability(userId);

    String meetingResponse = mockMvc.perform(get("/meeting/" + meetingId))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    String uuid = objectMapper.readTree(meetingResponse).get("uuid").asText();
    LocalDate monday = LocalDate.now()
        .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));

    mockMvc.perform(get("/public/slots")
            .param("uuid", uuid)
            .param("startDate", monday.toString())
            .param("endDate", monday.plusDays(2).toString())
            .param("timezone", "UTC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());
  }

  private String createTestUser(String username) throws Exception {
    CreateUserRequest req = new CreateUserRequest(username, username, username + "@org.com",
        "UTC", null, null);
    String response = mockMvc.perform(post("/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response).get("id").asText();
  }

  private String createTestMeeting(String organizerId, String title) throws Exception {
    CreateMeetingRequest req = new CreateMeetingRequest(organizerId, title, "test desc", "UTC");
    String response = mockMvc.perform(post("/meeting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response).get("id").asText();
  }

  private void createTestAvailability(String organizerId) throws Exception {
    List<AvailabilityRule> rules = List.of(
        new AvailabilityRule(null, "monday", "08:00", "18:00"),
        new AvailabilityRule(null, "tuesday", "08:00", "18:00"),
        new AvailabilityRule(null, "wednesday", "08:00", "18:00"),
        new AvailabilityRule(null, "thursday", "08:00", "18:00"),
        new AvailabilityRule(null, "friday", "08:00", "18:00"));
    CreateAvailabilityRequest req = new CreateAvailabilityRequest(organizerId, "Test Hours",
        "UTC", rules, new ArrayList<>());
    mockMvc.perform(post("/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated());
  }
}
