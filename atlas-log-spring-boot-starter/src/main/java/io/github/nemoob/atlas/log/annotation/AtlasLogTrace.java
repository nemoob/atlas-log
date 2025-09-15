package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.*;

/**
 * Atlas Log 链路追踪配置注解
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AtlasLogTrace {
    
    /**
     * 是否启用链路追踪
     */
    boolean enabled() default true;
    
    /**
     * HTTP头名称
     */
    String headerName() default "X-Trace-Id";
    
    /**
     * 生成器类型：uuid, snowflake
     */
    String generator() default "uuid";
}