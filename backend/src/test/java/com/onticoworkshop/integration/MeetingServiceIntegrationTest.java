package com.onticoworkshop.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onticoworkshop.BaseIntegrationTest;
import com.onticoworkshop.dto.CreateMeetingRequest;
import com.onticoworkshop.dto.UpdateMeetingRequest;
import com.onticoworkshop.model.MeetingTimeRule;

class MeetingServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldCreateAndRetrieveMeeting() throws Exception {
    CreateMeetingRequest request = new CreateMeetingRequest("org1", "Strategy Call",
        "Quarterly review", "UTC");

    String response = mockMvc.perform(post("/meeting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.title").value("Strategy Call"))
        .andExpect(jsonPath("$.durationMinutes").value(30))
        .andExpect(jsonPath("$.isActive").value(true))
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(get("/meeting/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Quarterly review"))
        .andExpect(jsonPath("$.uuid").isString());
  }

  @Test
  void shouldUpdateMeeting() throws Exception {
    CreateMeetingRequest createReq = new CreateMeetingRequest("org1", "Old Title",
        "desc", "UTC");

    String response = mockMvc.perform(post("/meeting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    UpdateMeetingRequest updateReq = new UpdateMeetingRequest();
    updateReq.setTitle("New Title");
    updateReq.setDurationMinutes(60);

    mockMvc.perform(patch("/meeting/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New Title"))
        .andExpect(jsonPath("$.durationMinutes").value(60));
  }

  @Test
  void shouldCreateAndUpdateMeetingWithTimeRules() throws Exception {
    CreateMeetingRequest createReq = new CreateMeetingRequest("org1",
        "Meeting with rules", "desc", "UTC");

    String response = mockMvc.perform(post("/meeting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    UpdateMeetingRequest updateReq = new UpdateMeetingRequest();
    updateReq.setTimeRules(List.of(
        new MeetingTimeRule(null, "monday", "09:00", "12:00"),
        new MeetingTimeRule(null, "wednesday", "14:00", "17:00")));

    mockMvc.perform(patch("/meeting/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk());

    mockMvc.perform(get("/meeting/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeRules.length()").value(2));
  }

  @Test
  void shouldDeleteMeeting() throws Exception {
    CreateMeetingRequest createReq = new CreateMeetingRequest("org1", "To Delete",
        "desc", "UTC");

    String response = mockMvc.perform(post("/meeting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(delete("/meeting/" + id))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/meeting/" + id))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn404ForMissingMeeting() throws Exception {
    mockMvc.perform(get("/meeting/m_nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldListMeetingsByOrganizer() throws Exception {
    CreateMeetingRequest req1 = new CreateMeetingRequest("orgZ", "M1", "desc", "UTC");
    CreateMeetingRequest req2 = new CreateMeetingRequest("orgZ", "M2", "desc", "UTC");
    CreateMeetingRequest req3 = new CreateMeetingRequest("orgW", "Other", "desc", "UTC");

    mockMvc.perform(post("/meeting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req1)))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/meeting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req2)))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/meeting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req3)))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/meeting").param("organizerId", "orgZ"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2));
  }
}
