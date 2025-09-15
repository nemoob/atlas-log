package io.github.nemoob.atlas.log.annotation;

import io.github.nemoob.atlas.log.config.AtlasLogAnnotationConfigRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用Atlas Log注解配置
 * <p>
 * 通过在Spring Boot应用主类上添加此注解，可以通过注解方式配置Atlas Log，
 * 提供更加简洁和声明式的配置方式。
 * </p>
 * 
 * <h3>基础使用示例：</h3>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableAtlasLog
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * </pre>
 * 
 * <h3>完整配置示例：</h3>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableAtlasLog(
 *     enabled = true,
 *     defaultLevel = "INFO",
 *     enabledTags = {"business", "security", "api"},
 *     enabledGroups = {"default", "business"},
 *     trace = &#64;AtlasLogTrace(enabled = true, headerName = "X-Trace-Id"),
 *     performance = &#64;AtlasLogPerformance(slowThreshold = 1000L),
 *     sensitive = &#64;AtlasLogSensitive(maskValue = "***")
 * )
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * </pre>
 * 
 * <h3>配置优先级：</h3>
 * <p>注解配置 > application.yml > 环境变量 > 默认值</p>
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AtlasLogAnnotationConfigRegistrar.class)
public @interface EnableAtlasLog {
    
    /**
     * 是否启用Atlas Log
     */
    boolean enabled() default true;
    
    /**
     * 默认日志级别
     */
    String defaultLevel() default "INFO";
    
    /**
     * 日期格式
     */
    String dateFormat() default "yyyy-MM-dd HH:mm:ss.SSS";
    
    /**
     * 是否美化JSON输出
     */
    boolean prettyPrint() default false;
    
    /**
     * 最大消息长度
     */
    int maxMessageLength() default 2000;
    
    /**
     * 是否启用SpEL表达式
     */
    boolean spelEnabled() default true;
    
    /**
     * 是否启用条件评估
     */
    boolean conditionEnabled() default true;
    
    /**
     * 启用的日志标签
     * <p>只有这些标签的日志才会被记录</p>
     */
    String[] enabledTags() default {"business", "security", "api"};
    
    /**
     * 启用的日志组
     */
    String[] enabledGroups() default {"default", "business"};
    
    /**
     * 排除的方法模式
     * <p>匹配这些模式的方法不会记录日志</p>
     */
    String[] exclusions() default {"*.toString", "*.hashCode", "*.equals"};
    
    /**
     * 链路追踪配置
     */
    AtlasLogTrace trace() default @AtlasLogTrace;
    
    /**
     * 性能监控配置
     */
    AtlasLogPerformance performance() default @AtlasLogPerformance;
    
    /**
     * 条件评估配置
     */
    AtlasLogCondition condition() default @AtlasLogCondition;
    
    /**
     * 敏感数据配置
     */
    AtlasLogSensitive sensitive() default @AtlasLogSensitive;
    
    /**
     * HTTP请求日志配置
     */
    AtlasLogHttpLog httpLog() default @AtlasLogHttpLog;
    
    /**
     * 返回值记录配置
     */
    AtlasLogResultLog resultLog() default @AtlasLogResultLog;
}