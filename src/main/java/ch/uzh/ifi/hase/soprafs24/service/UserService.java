package ch.uzh.ifi.hase.soprafs24.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User createUser(User newUser) {
    checkLoginCredential(newUser);
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setCreation_date(new Date());
    newUser.setStatus(UserStatus.ONLINE);
    checkIfUserExists(newUser);
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  public User userLogin(User loginCredential){
    checkLoginCredential(loginCredential);
    User userByUsername = userRepository.findByUsername(loginCredential.getUsername());
    if(userByUsername == null){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user by username not found");
    }
    if(!userByUsername.getPassword().equals(loginCredential.getPassword())){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "wrong password");
    }

    userByUsername.setStatus(UserStatus.ONLINE);

    userRepository.saveAndFlush(userByUsername);

    return userByUsername;
  }
  
  public void incrementWincounter(Long userId) {
    User user = userRepository.findById(userId).orElse(null);
    if (user != null) {
      user.setWincounter(user.getWincounter() + 1);
      userRepository.saveAndFlush(user);
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for ID: " + userId);
    }
  }
  
  public List<User> getUsersSortedByWins() {
    List<User> users = userRepository.findAll();
    users.sort((u1, u2) -> Integer.compare(u2.getWincounter(), u1.getWincounter())); // Descending
    return users;
  }



  public User userLogout(User loginCredential){
    User userByToken = userRepository.findByToken(loginCredential.getToken());
    // System.out.println("token:"+loginCredential.getToken());
    if(userByToken == null){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user by token not found, logout failed");
    }
    userByToken.setToken(UUID.randomUUID().toString());
    userByToken.setStatus(UserStatus.OFFLINE);

    userRepository.saveAndFlush(userByToken);

    return userByToken;
  }

  /**
   *  A helper method that check if the credentials user post is valid
   *  both username and password must not be empty
   *  other criteria may be implemented later
   * 
   *  @param userToBeCreated
   *  @throws org.springframework.web.server.ResponseStatusException
   *  @see User
   */
  private void checkLoginCredential(User userToBeCreated){
    if(userToBeCreated.getUsername().isEmpty()){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty username in login credential!");
    }else if (userToBeCreated.getPassword().isEmpty()){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty password in login credential!");
    }
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
    User userByName = userRepository.findByName(userToBeCreated.getName());

    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null && userByName != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          String.format(baseErrorMessage, "username and the name", "are"));
    } else if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username", "is"));
    } else if (userByName != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "name", "is"));
    }
  }

  public User getUserById(long userId){
    return userRepository.findById(userId).orElse(null);
  }

  public User getUserByToken(String token){
      // System.out.println("Looking for token: " + token);
      System.out.println("Looking for token: " + token);
      User user = userRepository.findByToken(token);
      if (user == null) {
          System.out.println("Token NOT FOUND!");
      } else {
          System.out.println("User found: " + user.getUsername());
      }
      return user;
//      return userRepository.findByToken(token);
  }

  public User UserEdit(User inputUser,long id){
    User userByToken = userRepository.findByToken(inputUser.getToken());
    if(userByToken.getId() != id){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "cannot change other user's profile");
    }

    User checkDuplicUserName = userRepository.findByUsername(inputUser.getUsername());
    User checkDuplicName = userRepository.findByName(inputUser.getName());
    User edituser = userRepository.findById(id).orElse(null);
    if(edituser == null){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,"user with userId was not found");
    }else if(checkDuplicUserName != null && checkDuplicUserName.getId()!=id){
      throw new ResponseStatusException(HttpStatus.CONFLICT,"username already taken");
    }else if(checkDuplicName != null && checkDuplicName.getId()!=id){
      throw new ResponseStatusException(HttpStatus.CONFLICT,"name already taken");
    }else{
      if(inputUser.getBirthday()!= null){
        edituser.setBirthday(inputUser.getBirthday());
      }

      //增加更改头像
      edituser.setAvatar(inputUser.getAvatar());

      edituser.setName(inputUser.getName());
      edituser.setUsername(inputUser.getUsername());
      if(inputUser.getPassword()!= null && !inputUser.getPassword().trim().isEmpty()){
        edituser.setPassword(inputUser.getPassword());
      }
      edituser = userRepository.saveAndFlush(edituser);
    }
    return edituser;
  }

}
