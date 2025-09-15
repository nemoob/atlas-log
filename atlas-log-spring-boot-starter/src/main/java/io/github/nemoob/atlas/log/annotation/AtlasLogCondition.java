package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.*;

/**
 * Atlas Log 条件评估配置注解
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AtlasLogCondition {
    
    /**
     * 是否启用表达式缓存
     */
    boolean cacheEnabled() default true;
    
    /**
     * 表达式执行超时时间（毫秒）
     */
    long timeoutMs() default 1000L;
    
    /**
     * 表达式执行失败时是否仍然记录日志
     */
    boolean failSafe() default true;
}