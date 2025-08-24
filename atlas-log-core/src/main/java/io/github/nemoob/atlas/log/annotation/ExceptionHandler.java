package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异常处理注解
 * 用于定义特定异常类型的日志处理方式
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionHandler {
    
    /**
     * 异常类型
     * 
     * @return 异常类
     */
    Class<? extends Throwable> exception();
    
    /**
     * 该异常的日志级别
     * 
     * @return 日志级别
     */
    LogLevel level() default LogLevel.ERROR;
    
    /**
     * 自定义异常日志模板，支持SpEL表达式
     * 支持的变量：
     * - #{args[n]} - 方法参数
     * - #{exception} - 异常对象
     * - #{exception.message} - 异常消息
     * - #{methodName} - 方法名称
     * 
     * @return 异常日志模板
     */
    String message() default "";
    
    /**
     * 是否记录异常堆栈
     * 
     * @return true-记录堆栈，false-不记录堆栈
     */
    boolean logStackTrace() default true;
}