package io.github.nemoob.atlas.log.context;

import java.util.UUID;

/**
 * 链路追踪ID持有者
 * 使用InheritableThreadLocal在当前线程及其子线程中存储和传递TraceId
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public class TraceIdHolder {
    
    private static final InheritableThreadLocal<String> TRACE_ID_HOLDER = new InheritableThreadLocal<>();
    
    /**
     * 设置当前线程的TraceId
     * 
     * @param traceId 链路追踪ID
     */
    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }
    
    /**
     * 获取当前线程的TraceId
     * 如果不存在则自动生成一个
     * 
     * @return 链路追踪ID
     */
    public static String getTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (traceId == null) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }
    
    /**
     * 获取当前线程的TraceId（不自动生成）
     * 
     * @return 链路追踪ID，可能为null
     */
    public static String getTraceIdIfPresent() {
        return TRACE_ID_HOLDER.get();
    }
    
    /**
     * 清除当前线程的TraceId
     */
    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
    
    /**
     * 生成新的TraceId
     * 
     * @return 生成的TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 在指定的上下文中执行操作
     * 
     * @param traceId 链路追踪ID
     * @param runnable 要执行的操作
     */
    public static void runWithTraceId(String traceId, Runnable runnable) {
        String originalTraceId = getTraceIdIfPresent();
        try {
            setTraceId(traceId);
            runnable.run();
        } finally {
            if (originalTraceId != null) {
                setTraceId(originalTraceId);
            } else {
                clear();
            }
        }
    }
}