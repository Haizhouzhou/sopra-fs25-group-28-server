package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LoginCredentialPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LoginTokenGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserListGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;



/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserListGetDTO> getAllUsers() {//modified
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserListGetDTO> userListGetDTO = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      userListGetDTO.add(DTOMapper.INSTANCE.convertUserToUserListGetDTO(user));
    }
    return userListGetDTO;
  }

  @GetMapping("/users/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO getSpecificUser(@PathVariable long id){
     User user = userService.getUserById(id);

    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }

  @PutMapping("users/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public void editSepcificUser(@PathVariable long id, @RequestBody UserPostDTO userPostDTO) {
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    // System.out.println("Received token: " + (userInput.getToken() != null ? userInput.getToken() : "null"));
    User editUser = userService.UserEdit(userInput, id);
    
    // return DTOMapper.INSTANCE.convertEntityToUserGetDTO(editUser);
  }
  

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public LoginTokenGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User newUser = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // create user
    User createdUser = userService.createUser(newUser);
    // return login token to frontend, then front end will automatically login with token
    return DTOMapper.INSTANCE.convertEntityToLoginTokenGetDTO(createdUser);
    // convert internal representation of user back to API
    // return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

  @PutMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public LoginTokenGetDTO userLogin(@RequestBody LoginCredentialPostDTO loginCredential) {
      // RequestBody:
      // check user status
      // return: DTO that contains accesstoken
      User loginUser = DTOMapper.INSTANCE.convertLoginCredentialPostDTOtoUser(loginCredential);
      loginUser = userService.userLogin(loginUser); 
      
      return DTOMapper.INSTANCE.convertEntityToLoginTokenGetDTO(loginUser);
  }

  @PutMapping("/logout")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void userLogout(@RequestBody LoginCredentialPostDTO loginCredential){
    //TODO: add token to LoginCredentialPostDTO， use this information to change users status
    User logoutUser = DTOMapper.INSTANCE.convertLoginCredentialPostDTOtoUser(loginCredential);
    logoutUser = userService.userLogout(logoutUser);

  }

  
}
