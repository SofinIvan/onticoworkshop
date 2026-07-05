package com.onticoworkshop.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onticoworkshop.BaseIntegrationTest;
import com.onticoworkshop.dto.CreateUserRequest;
import com.onticoworkshop.dto.UpdateUserRequest;

class UserServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldCreateAndRetrieveUser() throws Exception {
    CreateUserRequest request = new CreateUserRequest("dave", "Dave Smith", "dave@test.com",
        "UTC", "Bio here", "https://avatar.url");

    String response = mockMvc.perform(post("/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.username").value("dave"))
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(get("/user/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Dave Smith"))
        .andExpect(jsonPath("$.bio").value("Bio here"));
  }

  @Test
  void shouldUpdateUserPartially() throws Exception {
    CreateUserRequest createReq = new CreateUserRequest("eve", "Eve", "eve@test.com",
        "UTC", null, null);

    String response = mockMvc.perform(post("/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).get("id").asText();

    UpdateUserRequest updateReq = new UpdateUserRequest();
    updateReq.setDisplayName("Eve Updated");

    mockMvc.perform(patch("/user/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Eve Updated"))
        .andExpect(jsonPath("$.username").value("eve"));
  }

  @Test
  void shouldListUsers() throws Exception {
    CreateUserRequest req1 = new CreateUserRequest("frank", "Frank", "frank@test.com",
        "UTC", null, null);
    CreateUserRequest req2 = new CreateUserRequest("grace", "Grace", "grace@test.com",
        "UTC", null, null);

    mockMvc.perform(post("/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req1)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req2)))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/user"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());
  }

  @Test
  void shouldReturn404ForMissingUser() throws Exception {
    mockMvc.perform(get("/user/usr_nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
