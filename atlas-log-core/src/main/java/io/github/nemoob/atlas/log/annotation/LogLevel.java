package io.github.nemoob.atlas.log.annotation;

/**
 * 日志级别枚举
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public enum LogLevel {
    
    /**
     * 跟踪级别 - 最详细的日志级别
     */
    TRACE,
    
    /**
     * 调试级别 - 详细的调试信息
     */
    DEBUG,
    
    /**
     * 信息级别 - 一般的信息性消息
     */
    INFO,
    
    /**
     * 警告级别 - 警告信息
     */
    WARN,
    
    /**
     * 错误级别 - 错误信息
     */
    ERROR
}