package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.*;

/**
 * Atlas Log 敏感数据配置注解
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AtlasLogSensitive {
    
    /**
     * 是否启用敏感数据脱敏
     */
    boolean enabled() default true;
    
    /**
     * 自定义敏感字段
     */
    String[] customFields() default {"bankCard", "idCard", "socialSecurityNumber"};
    
    /**
     * 脱敏标记
     */
    String maskValue() default "***";
}