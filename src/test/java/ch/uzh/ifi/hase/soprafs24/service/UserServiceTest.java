package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // given
    testUser = new User();
    testUser.setId(1L);
    testUser.setName("testName");
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // then
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getName(), createdUser.getName());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertEquals(testUser.getPassword(), createdUser.getPassword());
    assertNotNull(createdUser.getCreation_date());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
  }

  @Test
  void createUser_duplicateName_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  void createUser_duplicateInputs_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  void userLogout_validToken_shouldLogoutAndChangeTokenAndStatus(){
    // arrange
    User userInDb = new User();
    userInDb.setId(2L);
    userInDb.setName("userInDB");
    userInDb.setUsername("usernameInDB");
    userInDb.setPassword("passwordInDB");
    userInDb.setToken("oldToken");

    User input = new User();
    input.setToken("oldToken");

    Mockito.when(userRepository.findByToken("oldToken")).thenReturn(userInDb);
    Mockito.when(userRepository.saveAndFlush(Mockito.any())).thenReturn(userInDb);

    // act
    User result = userService.userLogout(input);

    // assert
    Mockito.verify(userRepository).findByToken("oldToken");
    Mockito.verify(userRepository).saveAndFlush(userInDb);

    assertEquals(UserStatus.OFFLINE, userInDb.getStatus());
    assertNotNull(userInDb.getToken());
    assertNotEquals("oldToken", userInDb.getToken()); // token 应该已改变
    assertEquals(userInDb, result);
  }

  @Test
  void userLogout_invalidToken_shouldThrowException() {
    // arrange
    User input = new User();
    input.setToken("notExistToken");
    Mockito.when(userRepository.findByToken("notExistToken")).thenReturn(null);

    // act and assert
    assertThrows(ResponseStatusException.class, () -> userService.userLogout(input));
  }

  @Test
  void getUserByToken_tokenExists_shouldReturnUser() {
    // assert
    User userInDb = new User();
    userInDb.setId(2L);
    userInDb.setName("userInDB");
    userInDb.setUsername("usernameInDB");
    userInDb.setPassword("passwordInDB");
    userInDb.setToken("tokenInDB");
    Mockito.when(userRepository.findByToken("tokenInDB")).thenReturn(userInDb);

    // act
    User result = userService.getUserByToken("tokenInDB");

    // assert
    Mockito.verify(userRepository).findByToken("tokenInDB");
    assertEquals(userInDb, result);
  }

  @Test
  void getUserByToken_tokenNotExist_shouldReturnNull() {
    // arrange
    Mockito.when(userRepository.findByToken("noSuchToken")).thenReturn(null);

    // act
    User result = userService.getUserByToken("noSuchToken");

    // assert
    Mockito.verify(userRepository).findByToken("noSuchToken");
    assertEquals(null, result);
  }

  @Test
  void incrementWincounter_userExists_shouldIncrementWinCounter() {
    // arrange
    User userInDb = new User();
    userInDb.setId(42L);
    userInDb.setWincounter(3);

    Mockito.when(userRepository.findById(42L)).thenReturn(java.util.Optional.of(userInDb));
    Mockito.when(userRepository.saveAndFlush(userInDb)).thenReturn(userInDb);

    // act
    userService.incrementWincounter(42L);

    // assert
    Mockito.verify(userRepository).findById(42L);
    Mockito.verify(userRepository).saveAndFlush(userInDb);
    assertEquals(4, userInDb.getWincounter());
  }

  @Test
  void incrementWincounter_userNotExist_shouldThrowException() {
    // arrange
    Mockito.when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

    // act & assert
    assertThrows(ResponseStatusException.class, () -> userService.incrementWincounter(99L));
    Mockito.verify(userRepository).findById(99L);
  }

}
