package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.*;

/**
 * Atlas Log HTTP请求日志配置注解
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AtlasLogHttpLog {
    
    /**
     * 是否记录完整的请求参数
     */
    boolean logFullParameters() default true;
    
    /**
     * URL格式化模式
     * 支持占位符：{method}, {uri}, {queryString}, {remoteAddr}
     */
    String urlFormat() default "";
    
    /**
     * 是否包含查询字符串
     */
    boolean includeQueryString() default true;
    
    /**
     * 是否包含请求头信息
     */
    boolean includeHeaders() default false;
    
    /**
     * 排除的请求头（敏感信息）
     */
    String[] excludeHeaders() default {"authorization", "cookie", "x-auth-token"};
}