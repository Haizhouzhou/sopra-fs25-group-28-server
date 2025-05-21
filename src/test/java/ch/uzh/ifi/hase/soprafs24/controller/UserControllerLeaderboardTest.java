package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.LeaderboardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

@WebMvcTest(UserController.class)
public class UserControllerLeaderboardTest {

    @MockBean
    private UserService userService;

      @MockBean
      private LeaderboardService leaderboardService;

    @Autowired
    private MockMvc mockMvc;

    // @Mock
    // private LeaderboardService leaderboardService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * test for retriving leaderboard
     */
    @Test
    public void getLeaderboard_returnsLeaderboardSuccessfully() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Firstname Lastname");
        user.setUsername("firstname@lastname");
        user.setAvatar("avatar");
        user.setStatus(UserStatus.OFFLINE);
        user.setWincounter(1);

        when(userService.getUsersSortedByWins()).thenReturn(List.of(user));

        // List<UserListGetDTO> result = userController.getLeaderboard();

        MockHttpServletRequestBuilder getRequest = get("/users/leaderboard").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(user.getId().intValue())))
            .andExpect(jsonPath("$[0].name", is(user.getName())))
            .andExpect(jsonPath("$[0].username", is(user.getUsername())))
            .andExpect(jsonPath("$[0].avatar", is(user.getAvatar())))
            .andExpect(jsonPath("$[0].wincounter", is(user.getWincounter())));
        verify(userService, times(1)).getUsersSortedByWins();
    
    }
}
