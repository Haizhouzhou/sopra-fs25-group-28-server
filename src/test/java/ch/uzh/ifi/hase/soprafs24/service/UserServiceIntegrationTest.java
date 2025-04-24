package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

  @Qualifier("userRepository")
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @BeforeEach
  public void setup() {
    userRepository.deleteAll();
  }

  @Test
  public void createUser_validInputs_success() {
    // given
    assertNull(userRepository.findByUsername("testUsername"));

    User testUser = new User();
    testUser.setName("testName");
    testUser.setPassword("testPassword");
    testUser.setUsername("testUsername");

    // when
    User createdUser = userService.createUser(testUser);

    // then
    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getName(), createdUser.getName()); // name is randomly generated when creating new user
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertNotNull(createdUser.getName());
    assertNotNull(createdUser.getCreation_date());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateUsername_throwsException() {
    assertNull(userRepository.findByUsername("testUsername"));

    User testUser = new User();
    testUser.setName("testName");
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
    User createdUser = userService.createUser(testUser);

    // attempt to create second user with same username
    User testUser2 = new User();

    // change the name but forget about the username
    testUser2.setName("testName2");
    testUser2.setUsername("testUsername");
    testUser2.setPassword("testPassword");

    // check that an error is thrown
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
  }

  @Test
  public void userLogin_usernameNotFound_throwsException(){
    User testLoginCredential = new User();
    testLoginCredential.setUsername("testUsername");
    testLoginCredential.setPassword("testPassword");

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.userLogin(testLoginCredential));
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }

  @Test
  public void userLogin_wrongPassword_throwsException(){

    User testUser = new User();
    testUser.setName("testName");
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
    userService.createUser(testUser);

    User testLoginCredential = new User();
    testLoginCredential.setUsername("testUsername");
    testLoginCredential.setPassword("testPassword");

    testLoginCredential.setPassword("wrongPassword");

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.userLogin(testLoginCredential));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }

}
