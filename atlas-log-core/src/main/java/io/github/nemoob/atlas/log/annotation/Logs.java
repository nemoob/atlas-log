package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多个日志注解的容器
 * 当在同一个方法或类上使用多个@Log注解时，会自动使用此容器
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Logs {
    
    /**
     * 日志注解数组
     * 
     * @return Log注解数组
     */
    Log[] value();
}