package io.github.nemoob.atlas.log.context;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志上下文
 * 在日志处理过程中传递各种上下文信息，供SpEL表达式使用
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Data
@Accessors(chain = true)
public class LogContext {
    
    /**
     * 链路追踪ID
     */
    private String traceId;
    
    /**
     * 方法开始执行时间（毫秒）
     */
    private long startTime;
    
    /**
     * 方法执行时间（毫秒）
     */
    private long executionTime;
    
    /**
     * 类名
     */
    private String className;
    
    /**
     * 方法名
     */
    private String methodName;
    
    /**
     * 方法签名
     */
    private String methodSignature;
    
    /**
     * 方法参数
     */
    private Object[] args;
    
    /**
     * 方法返回值
     */
    private Object result;
    
    /**
     * 异常对象
     */
    private Throwable exception;
    
    /**
     * 自定义变量
     * 可用于存储额外的上下文信息
     */
    private Map<String, Object> variables = new HashMap<>();
    
    /**
     * 添加自定义变量
     * 
     * @param key 变量名
     * @param value 变量值
     * @return 当前对象
     */
    public LogContext addVariable(String key, Object value) {
        this.variables.put(key, value);
        return this;
    }
    
    /**
     * 获取自定义变量
     * 
     * @param key 变量名
     * @return 变量值
     */
    public Object getVariable(String key) {
        return this.variables.get(key);
    }
    
    /**
     * 移除自定义变量
     * 
     * @param key 变量名
     * @return 移除的变量值
     */
    public Object removeVariable(String key) {
        return this.variables.remove(key);
    }
    
    /**
     * 清空所有自定义变量
     */
    public void clearVariables() {
        this.variables.clear();
    }
}