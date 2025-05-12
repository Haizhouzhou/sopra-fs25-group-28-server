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

import ch.uzh.ifi.hase.soprafs24.entity.LeaderboardEntry;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LoginCredentialPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LoginTokenGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserListGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LeaderboardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

/**
 * User Controller
 * Handles all REST requests related to user management and leaderboard.
 */
@RestController
public class UserController {

    private final UserService userService;
    private final LeaderboardService leaderboardService;

    public UserController(UserService userService, LeaderboardService leaderboardService) {
        this.userService = userService;
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserListGetDTO> getAllUsers() {
        List<User> users = userService.getUsers();
        List<UserListGetDTO> userListGetDTO = new ArrayList<>();

        for (User user : users) {
            userListGetDTO.add(DTOMapper.INSTANCE.convertUserToUserListGetDTO(user));
        }
        return userListGetDTO;
    }

    @GetMapping("/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getSpecificUser(@PathVariable long id) {
        User user = userService.getUserById(id);
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
    }

    @PutMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void editSpecificUser(@PathVariable long id, @RequestBody UserPostDTO userPostDTO) {
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        userService.UserEdit(userInput, id);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LoginTokenGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
        User newUser = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        User createdUser = userService.createUser(newUser);
        return DTOMapper.INSTANCE.convertEntityToLoginTokenGetDTO(createdUser);
    }

    @PutMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LoginTokenGetDTO userLogin(@RequestBody LoginCredentialPostDTO loginCredential) {
        User loginUser = DTOMapper.INSTANCE.convertLoginCredentialPostDTOtoUser(loginCredential);
        loginUser = userService.userLogin(loginUser);
        return DTOMapper.INSTANCE.convertEntityToLoginTokenGetDTO(loginUser);
    }

    @PutMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void userLogout(@RequestBody LoginCredentialPostDTO loginCredential) {
        User logoutUser = DTOMapper.INSTANCE.convertLoginCredentialPostDTOtoUser(loginCredential);
        userService.userLogout(logoutUser);
    }

    // ===== Leaderboard Endpoints =====

    @GetMapping("/users/leaderboard")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<LeaderboardEntry> getLeaderboard() {
        return leaderboardService.getLeaderboard();
    }
  }