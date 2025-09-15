package io.github.nemoob.atlas.log.expression;

import java.util.regex.Pattern;

/**
 * 表达式类型检测器
 * 负责检测表达式字符串的类型，以便选择合适的处理策略
 * 
 * @author nemoob  
 * @since 0.2.0
 */
public class ExpressionTypeDetector {
    
    /**
     * SpEL占位符正则模式: #{...}
     * 匹配格式: #{表达式内容}
     */
    private static final Pattern SPEL_PLACEHOLDER_PATTERN = Pattern.compile("#\\{([^}]+)\\}");
    
    /**
     * 纯SpEL表达式正则模式
     * 匹配整个字符串都是 #{...} 格式的表达式
     */
    private static final Pattern PURE_SPEL_PATTERN = Pattern.compile("^#\\{[^}]+\\}$");
    
    /**
     * 检测表达式类型
     * 
     * @param expression 表达式字符串
     * @return 表达式类型枚举
     */
    public static ExpressionType detectType(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return ExpressionType.PLAIN_TEXT;
        }
        
        String trimmed = expression.trim();
        
        // 检测纯SpEL表达式 - 整个字符串都是 #{...}
        if (PURE_SPEL_PATTERN.matcher(trimmed).matches()) {
            return ExpressionType.PURE_SPEL;
        }
        
        // 检测是否包含SpEL占位符
        if (SPEL_PLACEHOLDER_PATTERN.matcher(trimmed).find()) {
            return ExpressionType.TEMPLATE;
        }
        
        // 默认为纯文本
        return ExpressionType.PLAIN_TEXT;
    }
    
    /**
     * 检查是否包含SpEL占位符
     * 
     * @param expression 表达式字符串
     * @return 如果包含SpEL占位符返回true，否则返回false
     */
    public static boolean containsSpelPlaceholder(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        return SPEL_PLACEHOLDER_PATTERN.matcher(expression).find();
    }
    
    /**
     * 检查是否为纯SpEL表达式
     * 
     * @param expression 表达式字符串
     * @return 如果是纯SpEL表达式返回true，否则返回false
     */
    public static boolean isPureSpel(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        return PURE_SPEL_PATTERN.matcher(expression.trim()).matches();
    }
    
    /**
     * 获取SpEL占位符模式
     * 供其他类使用
     * 
     * @return SpEL占位符正则模式
     */
    public static Pattern getSpelPlaceholderPattern() {
        return SPEL_PLACEHOLDER_PATTERN;
    }
}