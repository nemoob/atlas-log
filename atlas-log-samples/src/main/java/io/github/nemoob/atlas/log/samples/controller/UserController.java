package io.github.nemoob.atlas.log.samples.controller;

import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.annotation.LogLevel;
import io.github.nemoob.atlas.log.samples.model.User;
import io.github.nemoob.atlas.log.samples.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 用户控制器
 * 演示Web环境下的Atlas Log功能
 * 
 * @author nemoob
 * @since 0.2.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    @Log(
        value = "Get_user_information",
        tags = {"api", "user", "query"},
        logArgs = false,
        logResult = false
    )
    public User getUserById(@PathVariable Long id) throws Exception{
        Future<User> future = executorService.submit(() -> userService.getUserById(id));
//        Callable<User> runnable = () -> userService.getUserById(id);
        return future.get();
    }
    
    /**
     * 获取VIP用户信息 - 条件日志
     */
    @GetMapping("/vip/{id}")
    @Log(
        value = "Get_VIP_user_information",
        condition = "#{@userService.isVipUser(args[0])}",
        tags = {"api", "vip"},
        level = LogLevel.INFO
    )
    public User getVipUserById(@PathVariable Long id) {
        return userService.getVipUserById(id);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Log(
        value = "User_login",
        tags = {"api", "auth", "security"},
        level = LogLevel.WARN,
        logArgs = true,
        excludeArgs = {1} // 排除密码
    )
    public boolean login(@RequestParam String username, @RequestParam String password) {
        return userService.login(username, password);
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    @Log(
        value = "Update_user_status",
        tags = {"api", "user", "update"},
        level = LogLevel.WARN
    )
    public void updateUserStatus(@PathVariable Long id, @RequestParam String status) {
        userService.updateUserStatus(id, status);
    }
    
    /**
     * 搜索用户
     */
    @GetMapping("/search")
    @Log(
        value = "Search_users",
        condition = "#{args[0] != null and args[0].length() >= 2}",
        tags = {"api", "search"},
        logArgs = true
    )
    public List<User> searchUsers(@RequestParam String keyword) {
        return userService.searchUsers(keyword);
    }
    
    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @Log(
        value = "Batch_delete_users",
        tags = {"api", "user", "delete", "batch"},
        level = LogLevel.WARN,
        enterMessage = "Start_batch_delete_users_API",
        exitMessage = "Batch_delete_users_API_completed: count=#{result}"
    )
    public int batchDeleteUsers(@RequestBody List<Long> userIds) {
        return userService.batchDeleteUsers(userIds);
    }
    
    /**
     * 管理员操作 - 高权限操作审计
     */
    @PostMapping("/admin/{id}/operation")
    @Log(
        value = "Admin_API_operation",
        tags = {"api", "admin", "audit"},
        level = LogLevel.ERROR, // 高权限操作使用ERROR级别便于监控
        logArgs = true
    )
    public void adminOperation(@PathVariable Long id, @RequestParam String operation) {
        User user = userService.getUserById(id);
        userService.adminOperation(user, operation);
    }
}