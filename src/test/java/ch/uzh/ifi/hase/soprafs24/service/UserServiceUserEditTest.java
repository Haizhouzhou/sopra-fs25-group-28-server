package ch.uzh.ifi.hase.soprafs24.service;

import java.text.ParseException; // 引入 JUnit 5 断言
import java.text.SimpleDateFormat; // 引入 Mockito 参数匹配器
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow; // 引入 BDD 风格的 Mockito 'given'
import static org.junit.jupiter.api.Assertions.assertEquals; // 引入 Mockito 'never'
import static org.junit.jupiter.api.Assertions.assertNotNull; // 引入 Mockito 'times'
import static org.junit.jupiter.api.Assertions.assertThrows; // 引入 Mockito 'verify'
import static org.junit.jupiter.api.Assertions.assertTrue; // 引入你的 User 实体类
import org.junit.jupiter.api.BeforeEach; // 引入你的 UserRepository 接口
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times; // 引入 java.util.Date
import static org.mockito.Mockito.verify; // 引入 Optional.get() 可能抛出的异常
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

/**
 * UserService 中 UserEdit 方法的单元测试类 (使用 java.util.Date)
 */
public class UserServiceUserEditTest {

    // @Mock: 创建 UserRepository 的模拟(Mock)对象。测试时不会实际访问数据库。
    @Mock
    private UserRepository userRepository;

    // @InjectMocks: 创建 UserService 实例，并将上面 @Mock 创建的 userRepository 注入进去。
    @InjectMocks
    private UserService userService;

    // 定义测试数据
    private User userToEdit; // 数据库中存在的用户，将要被编辑
    private User inputUser; // 包含更新信息和 token 的输入对象
    private User userByToken; // 根据 token 找到的用户 (授权用户)
    private final Long userId = 1L; // 目标用户的 ID
    private final String userToken = "test-token-123"; // 用户的 token

    // 用于创建 Date 对象的辅助工具
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private Date originalBirthday;
    private Date newBirthday;

    /**
     * @BeforeEach: 在每个 @Test 方法执行前运行，用于初始化测试环境和数据。
     */
    @BeforeEach
    void setUp() throws ParseException { // 声明可能抛出 ParseException
        // 初始化 Mockito 注解 (@Mock, @InjectMocks)
        MockitoAnnotations.openMocks(this);

        // 初始化日期对象
        originalBirthday = sdf.parse("1990-05-15");
        newBirthday = sdf.parse("1992-08-20");

        // 准备 (Given) - 创建测试用户对象
        userToEdit = new User();
        userToEdit.setId(userId);
        userToEdit.setName("Original Name");
        userToEdit.setUsername("originalUsername");
        userToEdit.setPassword("originalPassword");
        userToEdit.setToken(userToken);
        userToEdit.setBirthday(originalBirthday);

        userByToken = new User();
        userByToken.setId(userId); // 授权用户的 ID 必须与目标用户 ID 一致
        userByToken.setToken(userToken);

        inputUser = new User();
        inputUser.setToken(userToken); // 输入对象包含 token
        inputUser.setName("New Name");
        inputUser.setUsername("newUsername");
        inputUser.setPassword("newPassword123");
        inputUser.setBirthday(newBirthday);
        // inputUser 不需要 ID，ID 通过方法参数传入
    }

    /**
     * 测试场景：成功更新用户信息 (所有字段都更新)
     */
    @Test
    void testUserEdit_Success_AllFieldsUpdated() {
        // --- 准备 (Arrange / Given) ---
        // 模拟仓库方法：
        // 1. 验证 token -> 找到授权用户
        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        // 2. 查找目标用户 -> 找到 userToEdit
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        // 3. 检查用户名冲突 -> 新用户名没被占用 (返回 null)
        given(userRepository.findByUsername("newUsername")).willReturn(null);
        // 4. 检查名字冲突 -> 新名字没被占用 (返回 null)
        given(userRepository.findByName("New Name")).willReturn(null);
        // 5. 模拟保存操作 -> 返回保存后的用户 (这里直接返回传入的对象)
        //    使用 ArgumentCaptor 捕获传递给 saveAndFlush 的对象，以便后续检查
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        given(userRepository.saveAndFlush(userCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // --- 执行 (Act / When) ---
        User updatedUser = userService.UserEdit(inputUser, userId);

        // --- 验证 (Assert / Then) ---
        // 验证 UserRepository 的方法被正确调用
        verify(userRepository, times(1)).findByToken(eq(userToken));
        verify(userRepository, times(1)).findById(eq(userId));
        verify(userRepository, times(1)).findByUsername(eq("newUsername"));
        verify(userRepository, times(1)).findByName(eq("New Name"));
        verify(userRepository, times(1)).saveAndFlush(any(User.class)); // 验证保存被调用一次

        // 验证传递给 saveAndFlush 的 User 对象是否已更新
        User savedUser = userCaptor.getValue();
        assertEquals("New Name", savedUser.getName());
        assertEquals("newUsername", savedUser.getUsername());
        assertEquals("newPassword123", savedUser.getPassword());
        assertEquals(newBirthday, savedUser.getBirthday()); // 比较 Date 对象
        assertEquals(userId, savedUser.getId()); // ID 不应改变

        // 验证返回的 User 对象是否是更新后的对象
        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getName());
        assertEquals("newUsername", updatedUser.getUsername());
        assertEquals("newPassword123", updatedUser.getPassword());
        assertEquals(newBirthday, updatedUser.getBirthday());
    }

    /**
     * 测试场景：成功更新用户信息 (密码和生日字段为 null 或空，不更新)
     */
    @Test
    void testUserEdit_Success_PartialUpdate() {
        // --- 准备 (Arrange / Given) ---
        inputUser.setPassword(null); // 不提供新密码
        inputUser.setBirthday(null); // 不提供新生日

        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        given(userRepository.findByUsername("newUsername")).willReturn(null);
        given(userRepository.findByName("New Name")).willReturn(null);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        given(userRepository.saveAndFlush(userCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // --- 执行 (Act / When) ---
        User updatedUser = userService.UserEdit(inputUser, userId);

        // --- 验证 (Assert / Then) ---
        verify(userRepository, times(1)).saveAndFlush(any(User.class));

        // 验证保存的用户：名字和用户名更新，密码和生日保持不变
        User savedUser = userCaptor.getValue();
        assertEquals("New Name", savedUser.getName());
        assertEquals("newUsername", savedUser.getUsername());
        assertEquals("originalPassword", savedUser.getPassword()); // 密码应为原始密码
        assertEquals(originalBirthday, savedUser.getBirthday()); // 生日应为原始生日

        // 验证返回的用户
        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getName());
        assertEquals("newUsername", updatedUser.getUsername());
        assertEquals("originalPassword", updatedUser.getPassword());
        assertEquals(originalBirthday, updatedUser.getBirthday());
    }

    /**
     * 测试场景：失败 - 未授权 (尝试修改其他用户的信息)
     */
    @Test
    void testUserEdit_Failure_Unauthorized() {
        // --- 准备 (Arrange / Given) ---
        // 模拟 findByToken 返回一个 ID 不同的用户
        User differentUser = new User();
        differentUser.setId(99L); // 不同的 ID
        differentUser.setToken(userToken);
        given(userRepository.findByToken(userToken)).willReturn(differentUser);

        // --- 执行并验证 (Act & Assert / When & Then) ---
        // 断言会抛出 ResponseStatusException 异常
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.UserEdit(inputUser, userId); // userId (1L) 与 differentUser.getId() (99L) 不匹配
        });

        // 验证异常状态码和原因
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getReason().contains("cannot change other user's profile"));

        // 验证后续的数据库操作没有被执行
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    /**
     * 测试场景：失败 - 用户未找到
     * 注意：根据你的代码 `userRepository.findById(id).get()`，如果找不到用户，
     * Optional.empty().get() 会直接抛出 NoSuchElementException。
     * 如果你想抛出 HttpStatus.NOT_FOUND，需要修改 UserEdit 方法中的逻辑，
     * 例如使用 orElseThrow()。此测试验证当前代码的行为。
     */
    @Test
    void testUserEdit_Failure_UserNotFound() {
        // --- 准备 (Arrange / Given) ---
        // 权限检查通过
        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        // 模拟 findById 返回空 Optional，表示用户不存在
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // --- 执行并验证 (Act & Assert / When & Then) ---
        // 断言会抛出 NoSuchElementException (因为调用了 .get() 在空 Optional 上)
        assertThrows(NoSuchElementException.class, () -> {
            userService.UserEdit(inputUser, userId);
        });

        // 验证只执行了 token 和 id 的查找，后续操作未执行
        verify(userRepository, times(1)).findByToken(eq(userToken));
        verify(userRepository, times(1)).findById(eq(userId));
        verify(userRepository, times(1)).findByUsername(anyString());
        verify(userRepository, times(1)).findByName(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }


    /**
     * 测试场景：失败 - 用户名冲突 (新用户名已被其他用户占用)
     */
    @Test
    void testUserEdit_Failure_UsernameConflict() {
        // --- 准备 (Arrange / Given) ---
        // 创建一个已存在的、占用新用户名的用户 (但 ID 不同)
        User conflictingUser = new User();
        conflictingUser.setId(2L); // 不同的 ID
        conflictingUser.setUsername("newUsername"); // 占用了 inputUser 想用的用户名

        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        // 模拟 findByUsername 返回冲突用户
        given(userRepository.findByUsername("newUsername")).willReturn(conflictingUser);
        // 假设名字不冲突
        given(userRepository.findByName("New Name")).willReturn(null);

        // --- 执行并验证 (Act & Assert / When & Then) ---
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.UserEdit(inputUser, userId);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("username already taken"));
        verify(userRepository, never()).saveAndFlush(any(User.class)); // 验证未执行保存
    }

    /**
     * 测试场景：失败 - 名字冲突 (新名字已被其他用户占用)
     */
    @Test
    void testUserEdit_Failure_NameConflict() {
        // --- 准备 (Arrange / Given) ---
        // 创建一个已存在的、占用新名字的用户 (但 ID 不同)
        User conflictingUser = new User();
        conflictingUser.setId(3L); // 不同的 ID
        conflictingUser.setName("New Name"); // 占用了 inputUser 想用的名字

        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        // 用户名不冲突
        given(userRepository.findByUsername("newUsername")).willReturn(null);
        // 模拟 findByName 返回冲突用户
        given(userRepository.findByName("New Name")).willReturn(conflictingUser);

        // --- 执行并验证 (Act & Assert / When & Then) ---
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.UserEdit(inputUser, userId);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("name already taken"));
        verify(userRepository, never()).saveAndFlush(any(User.class)); // 验证未执行保存
    }

     /**
     * 测试场景：成功 - 用户名/名字与自身相同不算冲突
     * 例如，用户只想改生日，提交了相同的用户名和名字。
     */
    @Test
    void testUserEdit_Success_NoConflictWithSelf() {
        // --- 准备 (Arrange / Given) ---
        // 让输入的名字和用户名与数据库中的某个用户相同，但这个用户就是自己 (userToEdit)
        inputUser.setUsername("originalUsername"); // 使用原始用户名
        inputUser.setName("Original Name");       // 使用原始名字
        inputUser.setBirthday(newBirthday);       // 只想改生日

        given(userRepository.findByToken(userToken)).willReturn(userByToken);
        given(userRepository.findById(userId)).willReturn(Optional.of(userToEdit));
        // 模拟 findByUsername 返回自己 -> 不应视为冲突
        given(userRepository.findByUsername("originalUsername")).willReturn(userToEdit);
        // 模拟 findByName 返回自己 -> 不应视为冲突
        given(userRepository.findByName("Original Name")).willReturn(userToEdit);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        given(userRepository.saveAndFlush(userCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // --- 执行 (Act / When) ---
        // 断言操作不会抛出异常
        User updatedUser = assertDoesNotThrow(() -> userService.UserEdit(inputUser, userId));

        // --- 验证 (Assert / Then) ---
        verify(userRepository, times(1)).saveAndFlush(any(User.class)); // 验证保存被调用

        // 验证保存的用户：名字和用户名未变，生日更新
        User savedUser = userCaptor.getValue();
        assertEquals("Original Name", savedUser.getName());
        assertEquals("originalUsername", savedUser.getUsername());
        assertEquals(newBirthday, savedUser.getBirthday()); // 生日已更新
        // 密码也应该更新了（因为 inputUser 中提供了新密码，即使这里主要测试冲突）
        assertEquals("newPassword123", savedUser.getPassword());

        // 验证返回的用户
        assertNotNull(updatedUser);
        assertEquals("Original Name", updatedUser.getName());
        assertEquals("originalUsername", updatedUser.getUsername());
        assertEquals(newBirthday, updatedUser.getBirthday());
        assertEquals("newPassword123", updatedUser.getPassword());
    }
}