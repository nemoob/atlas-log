package io.github.nemoob.atlas.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JsonPath 值比较注解
 * 用于指定需要使用 JsonPath 提取和比较的字段路径
 * 
 * 使用场景：
 * 1. 方法执行前后的值比较
 * 2. 参数和返回值的特定字段比较
 * 3. 复杂对象的深度字段比较
 * 
 * 示例：
 * ```java
 * @Log
 * @JsonPathCompare({
 *     "$.user.id",           // 比较用户ID
 *     "$.order.amount",      // 比较订单金额
 *     "$.status",            // 比较状态
 *     "$..payment.method"    // 比较所有支付方式
 * })
 * public Order processOrder(OrderRequest request) {
 *     // 方法实现
 * }
 * ```
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPathCompare {
    
    /**
     * JsonPath 表达式数组
     * 指定需要提取和比较的字段路径
     * 
     * 支持的 JsonPath 语法：
     * - $.field - 根级字段
     * - $.object.field - 嵌套字段
     * - $.array[0] - 数组元素
     * - $.array[*] - 所有数组元素
     * - $..field - 递归搜索字段
     * - $[?(@.condition)] - 条件过滤
     * 
     * @return JsonPath 表达式数组
     */
    String[] value() default {};
    
    /**
     * 比较模式
     * 
     * @return 比较模式
     */
    CompareMode mode() default CompareMode.ARGS_VS_RESULT;
    
    /**
     * 是否记录比较结果到日志
     * 
     * @return 是否记录比较结果
     */
    boolean logComparison() default true;
    
    /**
     * 比较结果的日志级别
     * 
     * @return 日志级别
     */
    LogLevel logLevel() default LogLevel.INFO;
    
    /**
     * 当值不相等时是否记录详细信息
     * 
     * @return 是否记录详细信息
     */
    boolean logDifferences() default true;
    
    /**
     * 自定义比较消息模板
     * 支持占位符：
     * - {path} - JsonPath 路径
     * - {value1} - 第一个值
     * - {value2} - 第二个值
     * - {equal} - 是否相等
     * - {method} - 方法名
     * 
     * @return 消息模板
     */
    String messageTemplate() default "JsonPath比较: {path} | 参数值: {value1} | 返回值: {value2} | 相等: {equal}";
    
    /**
     * 比较失败时的处理策略
     * 
     * @return 失败处理策略
     */
    FailureStrategy onFailure() default FailureStrategy.LOG_WARNING;
    
    /**
     * 比较模式枚举
     */
    enum CompareMode {
        /**
         * 比较参数和返回值
         */
        ARGS_VS_RESULT,
        
        /**
         * 比较方法执行前后的参数值（需要参数是可变对象）
         */
        BEFORE_VS_AFTER,
        
        /**
         * 只提取值，不进行比较（用于值记录）
         */
        EXTRACT_ONLY
    }
    
    /**
     * 失败处理策略
     */
    enum FailureStrategy {
        /**
         * 记录警告日志
         */
        LOG_WARNING,
        
        /**
         * 记录错误日志
         */
        LOG_ERROR,
        
        /**
         * 静默忽略
         */
        IGNORE,
        
        /**
         * 抛出异常
         */
        THROW_EXCEPTION
    }
}