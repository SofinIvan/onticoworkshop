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
import com.onticoworkshop.dto.CreateAvailabilityRequest;
import com.onticoworkshop.dto.UpdateAvailabilityRequest;
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.DateOverride;

class AvailabilityServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldCreateAndRetrieveAvailability() throws Exception {
    CreateAvailabilityRequest request = new CreateAvailabilityRequest("org1", "Working hours",
        "UTC", null, null);

    String response = mockMvc.perform(post("/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.name").value("Working hours"))
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(get("/availability/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.organizerId").value("org1"));
  }

  @Test
  void shouldCreateAvailabilityWithRulesAndOverrides() throws Exception {
    List<AvailabilityRule> rules = List.of(
        new AvailabilityRule(null, "monday", "09:00", "17:00"),
        new AvailabilityRule(null, "friday", "09:00", "15:00"));
    List<DateOverride> overrides = List.of(
        new DateOverride(null, "2024-12-25", true, null, null));

    CreateAvailabilityRequest request = new CreateAvailabilityRequest("org1", "Custom Hours",
        "UTC", rules, overrides);

    String response = mockMvc.perform(post("/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(get("/availability/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rules.length()").value(2))
        .andExpect(jsonPath("$.dateOverrides.length()").value(1));
  }

  @Test
  void shouldUpdateAvailability() throws Exception {
    CreateAvailabilityRequest createReq = new CreateAvailabilityRequest("org1", "Old Name",
        "UTC", null, null);

    String response = mockMvc.perform(post("/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    UpdateAvailabilityRequest updateReq = new UpdateAvailabilityRequest();
    updateReq.setName("New Name");
    updateReq.setRules(List.of(new AvailabilityRule(null, "tuesday", "10:00", "14:00")));

    mockMvc.perform(patch("/availability/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"))
        .andExpect(jsonPath("$.rules.length()").value(1));
  }

  @Test
  void shouldDeleteAvailability() throws Exception {
    CreateAvailabilityRequest request = new CreateAvailabilityRequest("org1", "To Delete",
        "UTC", null, null);

    String response = mockMvc.perform(post("/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(delete("/availability/" + id))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/availability/" + id))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn404ForMissingAvailability() throws Exception {
    mockMvc.perform(get("/availability/av_nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldListAvailabilitiesByOrganizer() throws Exception {
    CreateAvailabilityRequest req1 = new CreateAvailabilityRequest("orgX", "A1", "UTC", null, null);
    CreateAvailabilityRequest req2 = new CreateAvailabilityRequest("orgX", "A2", "UTC", null, null);
    CreateAvailabilityRequest req3 = new CreateAvailabilityRequest("orgY", "A3", "UTC", null, null);

    mockMvc.perform(post("/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req1)))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req2)))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req3)))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/availability").param("organizerId", "orgX"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2));
  }
}
