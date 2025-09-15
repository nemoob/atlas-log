package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.*;

/**
 * Atlas Log 返回值记录配置注解
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AtlasLogResultLog {
    
    /**
     * 是否全局启用返回值记录
     * 当此项为 false 时，即使 @Log 注解中 logResult=true 也不会记录返回值
     */
    boolean enabled() default true;
    
    /**
     * 返回值最大长度限制
     * -1 表示不限制长度（全部打印）
     */
    int maxLength() default 1000;
    
    /**
     * 是否打印完整返回值（忽略长度限制）
     */
    boolean printAll() default false;
    
    /**
     * 返回值被截断时的提示信息
     */
    String truncateMessage() default "[TRUNCATED]";
}