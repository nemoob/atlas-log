package io.github.nemoob.atlas.log.samples.service;

import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.samples.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 异步用户服务示例
 * 展示 TraceId 在异步执行中的传递
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Service
@Slf4j
public class AsyncUserService {
    
    /**
     * 异步查询用户信息
     * 使用自定义的异步执行器
     */
    @Async("atlasAsyncExecutor")
    @Log(value = "异步查询用户: #{args[0]}", tags = {"async", "user", "query"})
    public CompletableFuture<User> getUserAsync(Long userId) {
        try {
            // 模拟数据库查询延迟
            Thread.sleep(1000);
            
            User user = new User();
            user.setId(userId);
            user.setUsername("异步用户" + userId);
            user.setEmail("async.user" + userId + "@example.com");
            
            return CompletableFuture.completedFuture(user);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CompletableFuture<User> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * 异步处理用户数据
     * 展示多个异步方法的 TraceId 传递
     */
    @Async("atlasAsyncExecutor")
    @Log(value = "异步处理用户数据: #{args[0].username}", tags = {"async", "user", "process"})
    public CompletableFuture<String> processUserAsync(User user) {
        try {
            // 模拟数据处理延迟
            Thread.sleep(500);
            
            String result = "处理完成: " + user.getUsername();
            log.info("User data processing result: {}", result);
            
            return CompletableFuture.completedFuture(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * 批量异步处理
     * 展示多个并发异步任务的 TraceId 传递
     */
    @Log(value = "批量异步处理用户: 数量=#{args[0].length}", tags = {"async", "batch", "user"})
    public CompletableFuture<String> batchProcessUsersAsync(Long[] userIds) {
        CompletableFuture<User>[] futures = new CompletableFuture[userIds.length];
        
        // 启动多个异步任务
        for (int i = 0; i < userIds.length; i++) {
            futures[i] = getUserAsync(userIds[i]);
        }
        
        // 等待所有任务完成
        return CompletableFuture.allOf(futures)
            .thenApply(v -> {
                StringBuilder result = new StringBuilder("批量处理完成: ");
                for (CompletableFuture<User> future : futures) {
                    try {
                        User user = future.get();
                        result.append(user.getUsername()).append(", ");
                    } catch (Exception e) {
                        log.error("Failed to get async result", e);
                    }
                }
                return result.toString();
            });
    }
}