package io.github.nemoob.atlas.log.samples.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置示例
 * 展示如何配置 TraceId 在异步执行中的传递
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Autowired
    private TaskDecorator traceIdTaskDecorator;
    
    /**
     * 配置异步执行器，支持 TraceId 传递
     */
    @Bean(name = "atlasAsyncExecutor")
    public Executor atlasAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 线程池配置
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("atlas-async-");
        
        // 设置 TaskDecorator 来传递 TraceId
        executor.setTaskDecorator(traceIdTaskDecorator);
        
        // 拒绝策略：由调用者线程执行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}