package ch.uzh.ifi.hase.soprafs24.service;

import java.text.ParseException; // 引入 JUnit 5 断言
import java.text.SimpleDateFormat; // 引入 Mockito 参数匹配器
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals; // 引入 BDD 风格的 Mockito 'given'
import static org.junit.jupiter.api.Assertions.assertNotNull; // 引入 Mockito 'never'
import static org.junit.jupiter.api.Assertions.assertThrows; // 引入 Mockito 'times'
import static org.junit.jupiter.api.Assertions.assertTrue; // 引入 Mockito 'verify'
import org.junit.jupiter.api.BeforeEach; // 引入你的 User 实体类
import org.junit.jupiter.api.Test; // 引入你的 UserRepository 接口
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify; // 引入 java.util.Date
import org.mockito.MockitoAnnotations; // 引入 Optional.get() 可能抛出的异常
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

/**
 * UserService 中 UserEdit 方法的单元测试类 (使用 java.util.Date)
 */
public class UserServiceUserEditTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User userToEdit;     // 模拟数据库中已有的用户
    private User inputUser;      // 模拟前端传入的更新数据
    private User userByToken;    // 通过 token 找到的授权用户
    private final Long userId = 1L; // 目标用户的 ID
    private final String userToken = "test-token-123"; 

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private Date originalBirthday;
    private Date newBirthday;

    /**
     * @BeforeEach: 在每个 @Test 方法执行前运行，用于初始化测试环境和数据。
     */
    @BeforeEach
    void setUp() throws ParseException { // 声明可能抛出 ParseException
        MockitoAnnotations.openMocks(this);

        originalBirthday = sdf.parse("1990-05-15");
        newBirthday = sdf.parse("1992-08-20");

        userToEdit = new User();
        userToEdit.setId(userId);
        userToEdit.setName("Original Name");
        userToEdit.setUsername("originalUsername");
        userToEdit.setPassword("originalPassword");
        userToEdit.setToken(userToken);
        userToEdit.setBirthday(originalBirthday);

        userByToken = new User();
        userByToken.setId(userId);
        userByToken.setToken(userToken);

        inputUser = new User();
        inputUser.setToken(userToken); 
        inputUser.setName("New Name");
        inputUser.setUsername("newUsername");
        inputUser.setPassword("newPassword123");
        inputUser.setBirthday(newBirthday);

    }

    @Test
    void testUserEdit_Success_AllFieldsUpdated() {
        // arrange
        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        given(userRepository.findByUsername("newUsername")).willReturn(null);
        given(userRepository.findByName("New Name")).willReturn(null);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        given(userRepository.saveAndFlush(userCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // act
        User updatedUser = userService.UserEdit(inputUser, userId);

        // assert
        verify(userRepository, times(1)).findByToken(eq(userToken));
        verify(userRepository, times(1)).findById(eq(userId));
        verify(userRepository, times(1)).findByUsername(eq("newUsername"));
        verify(userRepository, times(1)).findByName(eq("New Name"));
        verify(userRepository, times(1)).saveAndFlush(any(User.class)); // 验证保存被调用一次

        // if the saveAndFlush User is upadted correctly
        User savedUser = userCaptor.getValue();
        assertEquals("New Name", savedUser.getName());
        assertEquals("newUsername", savedUser.getUsername());
        assertEquals("newPassword123", savedUser.getPassword());
        assertEquals(newBirthday, savedUser.getBirthday()); // 比较 Date 对象
        assertEquals(userId, savedUser.getId()); // ID 不应改变

        // check the return user is updated
        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getName());
        assertEquals("newUsername", updatedUser.getUsername());
        assertEquals("newPassword123", updatedUser.getPassword());
        assertEquals(newBirthday, updatedUser.getBirthday());
    }

    @Test
    void testUserEdit_Success_PartialUpdate() {
        // arrange
        inputUser.setPassword(null); // no new password
        inputUser.setBirthday(null); // no new birthday

        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        given(userRepository.findByUsername("newUsername")).willReturn(null);
        given(userRepository.findByName("New Name")).willReturn(null);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        given(userRepository.saveAndFlush(userCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // act
        User updatedUser = userService.UserEdit(inputUser, userId);

        // assert
        verify(userRepository, times(1)).saveAndFlush(any(User.class));

        User savedUser = userCaptor.getValue();
        assertEquals("New Name", savedUser.getName());
        assertEquals("newUsername", savedUser.getUsername());
        assertEquals("originalPassword", savedUser.getPassword());
        assertEquals(originalBirthday, savedUser.getBirthday());

        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getName());
        assertEquals("newUsername", updatedUser.getUsername());
        assertEquals("originalPassword", updatedUser.getPassword());
        assertEquals(originalBirthday, updatedUser.getBirthday());
    }

    @Test
    void testUserEdit_Failure_Unauthorized() {
        // arrange
        User differentUser = new User();
        differentUser.setId(99L); // different ID
        differentUser.setToken(userToken);
        given(userRepository.findByToken(userToken)).willReturn(differentUser);

        // act and assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.UserEdit(inputUser, userId); // userId (1L) 与 differentUser.getId() (99L) 不匹配
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getReason().contains("cannot change other user's profile"));

        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void testUserEdit_Failure_UserNotFound() {
        // arrange
        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // act and assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.UserEdit(inputUser, userId);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("user with userId was not found", exception.getReason());

        verify(userRepository, times(1)).findByToken(eq(userToken));
        verify(userRepository, times(1)).findById(eq(userId));
        verify(userRepository, times(1)).findByUsername(anyString());
        verify(userRepository, times(1)).findByName(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void testUserEdit_Failure_UsernameConflict() {
        // arrange
        User conflictingUser = new User();
        conflictingUser.setId(2L); // different Id
        conflictingUser.setUsername("newUsername");

        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        given(userRepository.findByUsername("newUsername")).willReturn(conflictingUser);
        given(userRepository.findByName("New Name")).willReturn(null);

        // act & assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.UserEdit(inputUser, userId);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("username already taken"));
        verify(userRepository, never()).saveAndFlush(any(User.class)); // 验证未执行保存
    }

    @Test
    void testUserEdit_Failure_NameConflict() {
        // arrange
        User conflictingUser = new User();
        conflictingUser.setId(3L);
        conflictingUser.setName("New Name"); 

        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        given(userRepository.findByUsername("newUsername")).willReturn(null);
        given(userRepository.findByName("New Name")).willReturn(conflictingUser);

        // act & assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.UserEdit(inputUser, userId);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("name already taken"));
        verify(userRepository, never()).saveAndFlush(any(User.class)); // 验证未执行保存
    }

    @Test
    void testUserEdit_Success_NoConflictWithSelf() {
        // arrange
        inputUser.setUsername("originalUsername");
        inputUser.setName("Original Name");
        inputUser.setBirthday(newBirthday); 

        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        given(userRepository.findByUsername("originalUsername")).willReturn(userToEdit);
        given(userRepository.findByName("Original Name")).willReturn(userToEdit);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        given(userRepository.saveAndFlush(userCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // act & assert
        User updatedUser = assertDoesNotThrow(() -> userService.UserEdit(inputUser, userId));

        verify(userRepository, times(1)).saveAndFlush(any(User.class)); // 验证保存被调用
        User savedUser = userCaptor.getValue();
        assertEquals("Original Name", savedUser.getName());
        assertEquals("originalUsername", savedUser.getUsername());
        assertEquals(newBirthday, savedUser.getBirthday()); 
        assertEquals("newPassword123", savedUser.getPassword());

        assertNotNull(updatedUser);
        assertEquals("Original Name", updatedUser.getName());
        assertEquals("originalUsername", updatedUser.getUsername());
        assertEquals(newBirthday, updatedUser.getBirthday());
        assertEquals("newPassword123", updatedUser.getPassword());
    }
}