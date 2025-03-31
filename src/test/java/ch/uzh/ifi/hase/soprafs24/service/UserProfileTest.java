
package ch.uzh.ifi.hase.soprafs24.service;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.controller.UserController;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;

/**
 * UserProfileTest
 * This test class checks registration and login behavior of the user profile endpoints.
 */
@WebMvcTest(UserController.class)
public class UserProfileTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setName("Alice");
    user.setUsername("alice123");
    user.setPassword("secret");
    user.setToken("token123");
    user.setStatus(UserStatus.OFFLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setName("Alice");
    userPostDTO.setUsername("alice123");
    userPostDTO.setPassword("secret");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  @Test
  public void loginUser_validCredentials_userLoggedIn() throws Exception {
    // given
    User user = new User();
    user.setId(2L);
    user.setName("Bob");
    user.setUsername("bob321");
    user.setPassword("pass123");
    user.setToken("token321");
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO loginDTO = new UserPostDTO();
    loginDTO.setUsername("bob321");
    loginDTO.setPassword("pass123");

    given(userService.loginUser(Mockito.eq("bob321"), Mockito.eq("pass123"))).willReturn(user);

    // when
    MockHttpServletRequestBuilder loginRequest = post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(loginDTO));

    // then
    mockMvc.perform(loginRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created. %s", e.toString()));
    }
  }
}
