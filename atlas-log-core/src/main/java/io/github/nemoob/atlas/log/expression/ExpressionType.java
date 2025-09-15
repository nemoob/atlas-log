package io.github.nemoob.atlas.log.expression;

/**
 * 表达式类型枚举
 * 用于区分不同类型的表达式以便选择合适的处理策略
 * 
 * @author nemoob
 * @since 0.2.0
 */
public enum ExpressionType {
    
    /**
     * 纯文本 - 不包含任何SpEL表达式的字符串
     * 例如: "查询用户信息", "Get_user_information"
     */
    PLAIN_TEXT,
    
    /**
     * 纯SpEL表达式 - 整个字符串都是SpEL表达式
     * 例如: "#{args[0]}", "#{result.success}"
     */
    PURE_SPEL,
    
    /**
     * 模板表达式 - 包含文本和SpEL占位符的混合字符串
     * 例如: "查询用户信息: 用户ID=#{args[0]}", "User: #{args[0].name} logged in"
     */
    TEMPLATE
}