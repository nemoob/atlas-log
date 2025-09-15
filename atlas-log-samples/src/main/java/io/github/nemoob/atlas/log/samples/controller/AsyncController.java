package io.github.nemoob.atlas.log.samples.controller;

import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.samples.model.User;
import io.github.nemoob.atlas.log.samples.service.AsyncUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 异步控制器示例
 * 展示 TraceId 在异步调用中的传递
 * 
 * @author nemoob
 * @since 0.2.0
 */
@RestController
@RequestMapping("/api/async")
@Slf4j
public class AsyncController {
    
    @Autowired
    private AsyncUserService asyncUserService;
    
    /**
     * 异步查询用户
     * 测试 TraceId 在异步方法中的传递
     */
    @GetMapping("/users/{userId}")
    @Log(value = "异步查询用户接口: #{args[0]}", tags = {"api", "async", "user"})
    public CompletableFuture<User> getUserAsync(@PathVariable Long userId) {
        log.info("Starting async user query: {}", userId);
        
        return asyncUserService.getUserAsync(userId)
            .thenApply(user -> {
                log.info("Async query completed: {}", user.getUsername());
                return user;
            });
    }
    
    /**
     * 异步处理用户数据
     * 测试多个异步方法的链式调用
     */
    @PostMapping("/users/{userId}/process")
    @Log(value = "异步处理用户接口: #{args[0]}", tags = {"api", "async", "process"})
    public CompletableFuture<String> processUserAsync(@PathVariable Long userId) {
        log.info("Starting async user processing: {}", userId);
        
        return asyncUserService.getUserAsync(userId)
            .thenCompose(user -> {
                log.info("User query completed, starting processing: {}", user.getUsername());
                return asyncUserService.processUserAsync(user);
            })
            .thenApply(result -> {
                log.info("Async processing completed: {}", result);
                return result;
            });
    }
    
    /**
     * 批量异步处理
     * 测试多个并发异步任务的 TraceId 传递
     */
    @PostMapping("/users/batch")
    @Log(value = "批量异步处理接口: 数量=#{args[0].length}", tags = {"api", "async", "batch"})
    public CompletableFuture<String> batchProcessAsync(@RequestBody Long[] userIds) {
        log.info("Starting batch async processing, user count: {}", userIds.length);
        
        return asyncUserService.batchProcessUsersAsync(userIds)
            .thenApply(result -> {
                log.info("Batch async processing completed: {}", result);
                return result;
            });
    }
    
    /**
     * 测试 TraceId 传递
     * 专门用于验证 TraceId 在异步调用中的一致性
     */
    @GetMapping("/trace-test/{userId}")
    @Log(value = "TraceId传递测试: #{args[0]}", tags = {"api", "test", "trace"})
    public CompletableFuture<String> traceIdTest(@PathVariable Long userId) {
        log.info("=== TraceId propagation test started ===");
        
        return asyncUserService.getUserAsync(userId)
            .thenCompose(user -> {
                log.info("=== First async method completed ===");
                return asyncUserService.processUserAsync(user);
            })
            .thenApply(result -> {
                log.info("=== TraceId propagation test completed ===");
                return "TraceId传递测试完成: " + result;
            });
    }
}