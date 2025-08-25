package io.github.nemoob.atlas.log.async;

import io.github.nemoob.atlas.log.context.TraceIdHolder;
import org.springframework.core.task.TaskDecorator;

/**
 * TraceId 任务装饰器
 * 用于在异步执行时传递 TraceId 到新线程
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public class TraceIdTaskDecorator implements TaskDecorator {
    
    @Override
    public Runnable decorate(Runnable runnable) {
        // 获取当前线程的 TraceId
        String traceId = TraceIdHolder.getTraceIdIfPresent();
        
        return () -> {
            String originalTraceId = null;
            try {
                // 保存原始 TraceId（如果有的话）
                originalTraceId = TraceIdHolder.getTraceIdIfPresent();
                
                // 在新线程中设置 TraceId
                if (traceId != null) {
                    TraceIdHolder.setTraceId(traceId);
                }
                
                // 执行原始任务
                runnable.run();
            } finally {
                // 恢复或清理 TraceId
                if (originalTraceId != null) {
                    TraceIdHolder.setTraceId(originalTraceId);
                } else {
                    TraceIdHolder.clear();
                }
            }
        };
    }
}