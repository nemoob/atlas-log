package io.github.nemoob.atlas.log.samples.service;

import io.github.nemoob.atlas.log.annotation.ExceptionHandler;
import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.annotation.LogIgnore;
import io.github.nemoob.atlas.log.annotation.LogLevel;
import io.github.nemoob.atlas.log.samples.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务类
 * 演示Atlas Log的基本功能
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class UserService {
    
    /**
     * 根据ID查询用户 - 基本日志功能
     */
    @Log("查询用户信息: 用户ID= #{args[0]} ，查询之后的结果是：#{result}")
    public User getUserById(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不能为空或小于等于0");
        }
        
        // 模拟数据库查询
        return new User()
                .setId(userId)
                .setUid("user_" + userId)
                .setUsername("用户" + userId)
                .setEmail("user" + userId + "@example.com")
                .setUserType("normal")
                .setStatus("active")
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
    }
    
    /**
     * 条件日志记录 - 只有VIP用户才记录详细日志
     */
    @Log(
        value = "VIP用户查询: 用户ID=#{args[0]}",
        condition = "#{@userService.isVipUser(args[0])}",
        tags = {"vip", "query"},
        logArgs = true,
        logResult = true
    )
    public User getVipUserById(Long userId) {
        User user = getUserById(userId);
        if ("vip".equals(user.getUserType())) {
            // VIP用户额外处理
            log.info("处理VIP用户特殊逻辑: {}", userId);
        }
        return user;
    }
    
    /**
     * 敏感数据处理 - 登录功能
     */
    @Log(
        value = "用户登录: 用户名=#{args[0]}",
        logArgs = true,
        logResult = false,
        excludeArgs = {1} // 排除密码参数
    )
    public boolean login(String username, @LogIgnore String password) {
        // 模拟登录验证
        if ("admin".equals(username) && "123456".equals(password)) {
            return true;
        }
        throw new RuntimeException("用户名或密码错误");
    }
    
    /**
     * 多异常处理演示
     */
    @Log(
        value = "更新用户状态: 用户ID=#{args[0]}, 状态=#{args[1]}",
        exceptionHandlers = {
            @ExceptionHandler(
                exception = IllegalArgumentException.class,
                level = LogLevel.WARN,
                message = "参数错误: #{exception.message}",
                logStackTrace = false
            ),
            @ExceptionHandler(
                exception = RuntimeException.class,
                level = LogLevel.ERROR,
                message = "更新用户状态失败: 用户ID=#{args[0]}, 错误=#{exception.message}"
            )
        }
    )
    public void updateUserStatus(Long userId, String status) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (!"active".equals(status) && !"inactive".equals(status) && !"locked".equals(status)) {
            throw new IllegalArgumentException("无效的用户状态: " + status);
        }
        
        // 模拟业务异常
        if (userId == 999L) {
            throw new RuntimeException("用户不存在");
        }
        
        log.info("用户状态更新成功: userId={}, status={}", userId, status);
    }
    
    /**
     * 条件日志记录 - 管理员操作审计
     */
    @Log(
        value = "管理员操作审计",
        condition = "#{args[0].userType == 'admin'}",
        tags = {"admin", "audit"},
        level = LogLevel.WARN
    )
    public void adminOperation(User user, String operation) {
        log.info("执行管理员操作: user={}, operation={}", user.getUid(), operation);
    }
    
    /**
     * 批量操作 - 演示SpEL表达式访问集合
     */
    @Log(
        value = "批量删除用户: 用户数量=#{args[0].size()}",
        condition = "#{args[0].size() > 0}",
        enterMessage = "开始批量删除用户: #{args[0].size()}个",
        exitMessage = "批量删除完成: 成功删除#{result}个用户"
    )
    public int batchDeleteUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        
        // 模拟批量删除
        int deletedCount = userIds.size();
        log.info("批量删除用户: {}", userIds);
        return deletedCount;
    }
    
    /**
     * 判断是否为VIP用户（供SpEL表达式调用）
     */
    public boolean isVipUser(Long userId) {
        // 模拟VIP用户判断逻辑
        return userId != null && userId % 10 == 0; // 假设ID为10的倍数的是VIP用户
    }
    
    /**
     * 搜索用户 - 演示复杂条件
     */
    @Log(
        value = "搜索用户: 关键词=#{args[0]}",
        condition = "#{args[0] != null and args[0].length() >= 2}",
        tags = {"search"}
    )
    public List<User> searchUsers(String keyword) {
        // 模拟搜索
        return Arrays.asList(
            new User().setId(1L).setUsername("用户1").setUid("user_1"),
            new User().setId(2L).setUsername("用户2").setUid("user_2")
        );
    }
}