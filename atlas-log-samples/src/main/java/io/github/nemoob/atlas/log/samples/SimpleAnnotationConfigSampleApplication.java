package io.github.nemoob.atlas.log.samples;

import io.github.nemoob.atlas.log.annotation.EnableAtlasLog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Atlas Log 简化注解配置示例应用
 * <p>
 * 演示如何使用最少的配置来启用 Atlas Log。
 * 使用默认配置，只需添加 @EnableAtlasLog 注解即可。
 * </p>
 * 
 * <h3>默认配置：</h3>
 * <ul>
 *   <li>日志级别：INFO</li>
 *   <li>启用标签：business, security, api</li>
 *   <li>启用组：default, business</li>
 *   <li>链路追踪：启用 (Header: X-Trace-Id)</li>
 *   <li>性能监控：启用 (慢方法阈值: 1000ms)</li>
 *   <li>敏感数据脱敏：启用</li>
 * </ul>
 * 
 * @author nemoob
 * @since 0.2.0
 */
@SpringBootApplication
@EnableAtlasLog  // 使用默认配置
public class SimpleAnnotationConfigSampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SimpleAnnotationConfigSampleApplication.class, args);
        String separator = "";
        for (int i = 0; i < 60; i++) separator += "=";
        System.out.println(separator);
        System.out.println("Atlas Log 简化配置示例应用已启动");
        System.out.println(separator);
        System.out.println("使用默认配置：");
        System.out.println("• 日志级别：INFO");
        System.out.println("• 启用标签：business, security, api");
        System.out.println("• 启用组：default, business");
        System.out.println("• 链路追踪：启用");
        System.out.println("• 性能监控：启用");
        System.out.println("• 敏感数据脱敏：启用");
        System.out.println(separator);
    }
}