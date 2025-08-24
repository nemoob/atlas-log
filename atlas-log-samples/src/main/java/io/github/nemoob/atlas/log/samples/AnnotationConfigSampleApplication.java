package io.github.nemoob.atlas.log.samples;

import io.github.nemoob.atlas.log.annotation.EnableAtlasLog;
import io.github.nemoob.atlas.log.annotation.AtlasLogTrace;
import io.github.nemoob.atlas.log.annotation.AtlasLogPerformance;
import io.github.nemoob.atlas.log.annotation.AtlasLogCondition;
import io.github.nemoob.atlas.log.annotation.AtlasLogSensitive;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Atlas Log 注解配置示例应用
 * <p>
 * 演示如何使用 @EnableAtlasLog 注解来配置 Atlas Log，
 * 替代传统的 application.yml 配置方式。
 * </p>
 * 
 * <h3>配置说明：</h3>
 * <ul>
 *   <li>启用 Atlas Log 并设置为 DEBUG 级别</li>
 *   <li>配置多种日志标签：business, security, api, vip, admin, audit</li>
 *   <li>启用链路追踪功能</li>
 *   <li>配置性能监控（慢方法阈值：500ms）</li>
 *   <li>启用敏感数据脱敏</li>
 *   <li>排除特定方法的日志记录</li>
 * </ul>
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAtlasLog(
    enabled = true,
    defaultLevel = "DEBUG",
    dateFormat = "yyyy-MM-dd HH:mm:ss.SSS",
    prettyPrint = true,
    maxMessageLength = 3000,
    spelEnabled = true,
    conditionEnabled = true,
    enabledTags = {
        "business",   // 业务日志
        "security",   // 安全日志
        "api",        // API调用日志
        "vip",        // VIP用户日志
        "admin",      // 管理员操作日志
        "audit",      // 审计日志
        "urgent"      // 紧急日志
    },
    enabledGroups = {
        "default",    // 默认组
        "business",   // 业务组
        "security",   // 安全组
        "urgent"      // 紧急组
    },
    exclusions = {
        "*.toString",
        "*.hashCode",
        "*.equals",
        "*.getClass"
    },
    trace = @AtlasLogTrace(
        enabled = true,
        headerName = "X-Request-Trace-Id",
        generator = "uuid"
    ),
    performance = @AtlasLogPerformance(
        enabled = true,
        slowThreshold = 500L,  // 500ms
        logSlowMethods = true
    ),
    condition = @AtlasLogCondition(
        cacheEnabled = true,
        timeoutMs = 800L,
        failSafe = true
    ),
    sensitive = @AtlasLogSensitive(
        enabled = true,
        customFields = {
            "password",
            "bankCard", 
            "idCard", 
            "phone",
            "email",
            "socialSecurityNumber"
        },
        maskValue = "***"
    )
)
public class AnnotationConfigSampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AnnotationConfigSampleApplication.class, args);
        String separator = "";
        for (int i = 0; i < 80; i++) separator += "=";
        System.out.println(separator);
        System.out.println("Atlas Log 注解配置示例应用已启动");
        System.out.println(separator);
        System.out.println("配置说明：");
        System.out.println("• 日志级别：DEBUG");
        System.out.println("• 启用标签：business, security, api, vip, admin, audit, urgent");
        System.out.println("• 启用组：default, business, security, urgent");
        System.out.println("• 链路追踪：启用 (Header: X-Request-Trace-Id)");
        System.out.println("• 性能监控：启用 (慢方法阈值: 500ms)");
        System.out.println("• 敏感数据脱敏：启用");
        System.out.println("• 访问示例接口：");
        System.out.println("  - GET  http://localhost:8080/api/users/1");
        System.out.println("  - POST http://localhost:8080/api/users");
        System.out.println("  - GET  http://localhost:8080/api/orders/user/1");
        System.out.println(separator);
    }
}