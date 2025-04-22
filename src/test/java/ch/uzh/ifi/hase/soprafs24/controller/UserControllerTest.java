package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LoginCredentialPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is(user.getName())))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setPassword(("testPassword"));
    user.setToken("1");
    user.setStatus(UserStatus.OFFLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setPassword("testPassword");
    userPostDTO.setUsername("testUsername");

    // given
    given(userService.createUser(Mockito.any())).willThrow(new ResponseStatusException(HttpStatus.CONFLICT,"username already exists"));

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isConflict());
  }

  @Test
  public void createUser_duplicateUsername_throwsException() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setPassword(("testPassword"));
    user.setToken("1");
    user.setStatus(UserStatus.OFFLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setName("Test User");
    userPostDTO.setUsername("testUsername");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token", is(user.getToken())));
        // .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        // .andExpect(jsonPath("$.name", is(user.getName())))
        // .andExpect(jsonPath("$.username", is(user.getUsername())))
        // .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  @Test
  public void getSpecificUser_userFound_retriveProfile() throws Exception{
    // given
    User user = new User();
    user.setId(1L);
    user.setName("testName");
    user.setUsername("testUsername");
    user.setStatus(UserStatus.OFFLINE);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUserById(Mockito.anyLong())).willReturn(user);

    Long requestId = 1L;////the request id is sent as a pathVariable, not in the request body

    // when
    MockHttpServletRequestBuilder getRequest = get("/users/{id}", requestId).contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  @Test
  public void getSpecificUser_userNotFound_throwsException() throws Exception{
    //given
    given(userService.getUserById(Mockito.anyLong())).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,"user with userId was not found"));
    
    Long requestId = 1L;////the request id is sent as a pathVariable, not in the request body

    MockHttpServletRequestBuilder getRequest = get("/users/{id}",requestId)
        .contentType(MediaType.APPLICATION_JSON);
    
    mockMvc.perform(getRequest)
        .andExpect(status().isNotFound());
  }

  @Test
  public void editSepcificUser_validInput_updateProfile() throws Exception{

    User user = new User();
    user.setId(1L);
    user.setName("testName");
    user.setToken("testToken");
    user.setUsername("testUsername");
    user.setPassword("testPassword");
    user.setBirthday(new Date());
    user.setStatus(UserStatus.ONLINE);

    Long requestId = 1L;////the request id is sent as a pathVariable, not in the request body

    UserPostDTO userPutDTO = new UserPostDTO();
    userPutDTO.setName("testName");
    userPutDTO.setUsername("testUsername");
    // userPutDTO.setPassword("testPassword");
    userPutDTO.setBirthday(new Date());

    given(userService.UserEdit(Mockito.any(), Mockito.anyLong())).willReturn(user);
    // given(userService.UserEdit(Mockito.any(), Mockito.anyLong())).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,"user with userId was not found"));

    MockHttpServletRequestBuilder putRequest = put("/users/{id}",requestId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPutDTO));
    
    mockMvc.perform(putRequest)
    .andExpect(status().isNoContent());
  }

  @Test
  public void editSepcificUser_userNotFound_updateProfile() throws Exception{

    Long requestId = 1L;////the request id is sent as a pathVariable, not in the request body

    UserPostDTO userPutDTO = new UserPostDTO();
    userPutDTO.setName("testName");
    userPutDTO.setUsername("testUsername");
    // userPutDTO.setPassword("testPassword");
    userPutDTO.setBirthday(new Date());

    given(userService.UserEdit(Mockito.any(), Mockito.anyLong())).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,"user with userId was not found"));

    MockHttpServletRequestBuilder putRequest = put("/users/{id}",requestId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPutDTO));
    
    mockMvc.perform(putRequest)
    .andExpect(status().isNotFound());
  }

  @Test
  public void userLogin_validInput_changeUserStates() throws Exception{
    // given
    User user = new User();
    user.setId(1L);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setPassword("testPassword");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    LoginCredentialPostDTO loginCredentialPostDTO = new LoginCredentialPostDTO();
    loginCredentialPostDTO.setUsername("testUsername");
    loginCredentialPostDTO.setUsername("testPassword");

    given(userService.userLogin(Mockito.any())).willReturn(user);

    MockHttpServletRequestBuilder putRequest = put("/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(loginCredentialPostDTO));
    
    mockMvc.perform(putRequest)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token", is(user.getToken())));

  }

  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}