package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.*;

/**
 * Atlas Log 性能监控配置注解
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AtlasLogPerformance {
    
    /**
     * 是否启用性能监控
     */
    boolean enabled() default true;
    
    /**
     * 慢方法阈值（毫秒）
     */
    long slowThreshold() default 1000L;
    
    /**
     * 是否记录慢方法日志
     */
    boolean logSlowMethods() default true;
}