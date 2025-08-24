package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志忽略注解
 * 用于标记不应该记录到日志中的敏感参数或方法
 * 
 * 可以用于：
 * 1. 方法参数 - 忽略敏感参数的记录（如密码、token等）
 * 2. 方法 - 完全忽略该方法的日志记录
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogIgnore {
    
    /**
     * 忽略原因描述
     * 可用于说明为什么忽略此参数或方法的日志记录
     * 
     * @return 忽略原因
     */
    String reason() default "sensitive data";
}