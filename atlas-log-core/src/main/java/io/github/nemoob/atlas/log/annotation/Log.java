package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志记录注解
 * 支持在方法或类上使用，提供丰富的日志配置选项
 * 
 * 支持的SpEL表达式变量：
 * - #{args[n]} - 方法参数，n为参数索引
 * - #{result} - 方法返回值（仅在退出时可用）
 * - #{exception} - 异常对象（仅在异常时可用）
 * - #{methodName} - 方法名称
 * - #{className} - 类名称
 * - #{executionTime} - 方法执行时间（毫秒）
 * - #{@beanName} - Spring容器中的Bean
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Logs.class)
public @interface Log {
    
    /**
     * 日志级别
     * 
     * @return 日志级别
     */
    LogLevel level() default LogLevel.INFO;
    
    /**
     * 自定义日志描述，支持SpEL表达式
     * 
     * 示例：
     * - "查询用户信息: #{args[0]}"
     * - "创建订单: 用户ID=#{args[0].userId}, 金额=#{result.amount}"
     * - "处理支付: 订单ID=#{args[0]}, 结果=#{result.success}"
     * 
     * @return 日志描述
     */
    String value() default "";
    
    /**
     * 条件表达式，支持SpEL，只有满足条件时才记录日志
     * 
     * 示例：
     * - "#{args[0] != null and args[0] > 0}" - 第一个参数大于0时记录
     * - "#{args[0].uid == 'admin'}" - 当uid为admin时记录
     * - "#{result.success == true}" - 当结果成功时记录
     * - "#{@userService.isVipUser(args[0].uid)}" - 调用服务判断VIP用户
     * 
     * @return 条件表达式
     */
    String condition() default "";
    
    /**
     * 标签，用于分类和过滤日志
     * 
     * 示例：{"admin", "vip"} - 标记为管理员和VIP相关日志
     * 
     * @return 日志标签数组
     */
    String[] tags() default {};
    
    /**
     * 日志组名，相同组的日志可以统一控制
     * 可在配置文件中启用或禁用特定组的日志
     * 
     * @return 日志组名
     */
    String group() default "default";
    
    /**
     * 是否记录方法参数
     * 
     * @return true-记录参数，false-不记录参数
     */
    boolean logArgs() default false;
    
    /**
     * 是否记录返回值
     * 
     * @return true-记录返回值，false-不记录返回值
     */
    boolean logResult() default false;
    
    /**
     * 是否记录执行时间
     * 
     * @return true-记录执行时间，false-不记录执行时间
     */
    boolean logExecutionTime() default true;
    
    /**
     * 是否记录异常
     * 
     * @return true-记录异常，false-不记录异常
     */
    boolean logException() default true;
    
    /**
     * 排除记录的参数索引
     * 
     * 示例：{0, 2} - 排除第1个和第3个参数
     * 
     * @return 排除的参数索引数组
     */
    int[] excludeArgs() default {};
    
    /**
     * 参数最大长度限制
     * 超过此长度的参数将被截断
     * 
     * @return 参数最大长度
     */
    int maxArgLength() default 1000;
    
    /**
     * 返回值最大长度限制
     * 超过此长度的返回值将被截断
     * 
     * @return 返回值最大长度
     */
    int maxResultLength() default 1000;
    
    /**
     * 进入方法时的日志模板，支持SpEL表达式
     * 
     * 示例："开始执行: #{methodName}, 参数: #{args}"
     * 
     * @return 进入方法时的日志模板
     */
    String enterMessage() default "";
    
    /**
     * 退出方法时的日志模板，支持SpEL表达式
     * 
     * 示例："完成执行: #{methodName}, 结果: #{result}, 耗时: #{executionTime}ms"
     * 
     * @return 退出方法时的日志模板
     */
    String exitMessage() default "";
    
    /**
     * 异常日志模板，支持SpEL表达式
     * 
     * 示例："执行异常: #{methodName}, 错误: #{exception.message}"
     * 
     * @return 异常日志模板
     */
    String exceptionMessage() default "";
    
    /**
     * 需要特别处理的异常类型及其日志级别
     * 
     * @return 异常处理器数组
     */
    ExceptionHandler[] exceptionHandlers() default {};
    
    /**
     * 参数格式化器名称
     * 指定用于格式化方法参数的格式化器
     * 
     * 示例：
     * - "json" - 使用 JSON 格式化器
     * - "key-value" - 使用 key=value 格式化器
     * - "custom" - 使用自定义格式化器
     * - "" - 使用全局配置的格式化器（默认）
     * 
     * @return 参数格式化器名称
     */
    String argumentFormatter() default "";
    
    /**
     * 返回值格式化器名称
     * 指定用于格式化方法返回值的格式化器
     * 
     * 示例：
     * - "json" - 使用 JSON 格式化器
     * - "key-value" - 使用 key=value 格式化器
     * - "custom" - 使用自定义格式化器
     * - "" - 使用全局配置的格式化器（默认）
     * 
     * @return 返回值格式化器名称
     */
    String resultFormatter() default "";
}