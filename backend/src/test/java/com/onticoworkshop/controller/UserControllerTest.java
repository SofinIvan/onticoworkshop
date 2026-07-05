package com.onticoworkshop.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.onticoworkshop.dto.CreateUserRequest;
import com.onticoworkshop.dto.UpdateUserRequest;
import com.onticoworkshop.exception.GlobalExceptionHandler;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.User;
import com.onticoworkshop.service.UserService;

@WebMvcTest({UserController.class, GlobalExceptionHandler.class})
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @Test
  void shouldRejectInvalidCreateUserRequest() throws Exception {
    CreateUserRequest request = new CreateUserRequest("", "", "bad-email", "", null, null);

    mockMvc.perform(post("/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn404_whenUserNotFound() throws Exception {
    when(userService.getUser("usr_x")).thenThrow(new NotFoundException("User not found: usr_x"));

    mockMvc.perform(get("/user/usr_x"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
