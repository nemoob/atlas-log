package io.github.nemoob.atlas.log.expression;

import io.github.nemoob.atlas.log.context.LogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板表达式处理器
 * 处理包含中文字符和SpEL占位符的混合字符串
 * 例如: "查询用户信息: 用户ID=#{args[0]}"
 * 
 * 核心思路：
 * 1. 提取所有SpEL占位符 #{...}
 * 2. 对每个SpEL表达式单独求值
 * 3. 用求值结果替换原始模板中的占位符
 * 4. 避免将包含中文的整个字符串传递给SpEL解析器
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Slf4j
public class TemplateProcessor implements ExpressionProcessor {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final SpelExpressionEvaluator mainEvaluator;
    private final boolean failSafe;
    
    /**
     * SpEL占位符信息
     */
    private static class SpelPlaceholder {
        final String fullPlaceholder;  // 完整占位符 #{args[0]}
        final String expression;       // 纯表达式部分 args[0] 
        final int startIndex;          // 在原字符串中的开始位置
        final int endIndex;            // 在原字符串中的结束位置
        
        SpelPlaceholder(String fullPlaceholder, String expression, int startIndex, int endIndex) {
            this.fullPlaceholder = fullPlaceholder;
            this.expression = expression;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
    
    public TemplateProcessor(SpelExpressionEvaluator mainEvaluator, boolean failSafe) {
        this.mainEvaluator = mainEvaluator;
        this.failSafe = failSafe;
    }
    
    @Override
    public String processExpression(String expression, LogContext logContext) {
        if (expression == null || expression.trim().isEmpty()) {
            return "";
        }
        
        // 预先验证LogContext状态
        if (logContext == null) {
            log.warn("TemplateProcessor received null LogContext, expression: {}", expression);
            return failSafe ? "[LogContext为null]" : expression;
        }
        
        // 如果表达式包含args引用，验证args是否存在
        if (expression.contains("args") && logContext.getArgs() == null) {
            String warnMsg = String.format("表达式引用args但LogContext.args为null，表达式: %s, method: %s", 
                expression, logContext.getMethodName());
            log.warn(warnMsg);
            
            if (failSafe) {
                return "[" + warnMsg + "]";
            }
        }
        
        try {
            // 提取所有SpEL占位符
            List<SpelPlaceholder> placeholders = extractSpelPlaceholders(expression);
            
            if (placeholders.isEmpty()) {
                // 没有SpEL占位符，当作纯文本处理
                return expression;
            }
            
            // 创建求值上下文前再次验证
            EvaluationContext evaluationContext = createSafeEvaluationContext(logContext);
            
            // 替换占位符
            return replacePlaceholders(expression, placeholders, evaluationContext);
            
        } catch (Exception e) {
            String errorMsg = "模板表达式处理失败: " + expression + ", LogContext: " + 
                (logContext != null ? logContext.getMethodName() : "null");
            log.warn(errorMsg, e);
            
            if (failSafe) {
                return "[" + errorMsg + "]";
            } else {
                throw new RuntimeException(errorMsg, e);
            }
        }
    }
    
    @Override
    public boolean canHandle(String expression) {
        return ExpressionTypeDetector.detectType(expression) == ExpressionType.TEMPLATE;
    }
    
    @Override
    public ExpressionType getSupportedType() {
        return ExpressionType.TEMPLATE;
    }
    
    /**
     * 提取字符串中的所有SpEL占位符
     */
    private List<SpelPlaceholder> extractSpelPlaceholders(String template) {
        List<SpelPlaceholder> placeholders = new ArrayList<>();
        Pattern pattern = ExpressionTypeDetector.getSpelPlaceholderPattern();
        Matcher matcher = pattern.matcher(template);
        
        while (matcher.find()) {
            String fullPlaceholder = matcher.group(0);  // #{args[0]}
            String expression = matcher.group(1);       // args[0] - 这里已经是纯表达式，不包含#{}
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            
            placeholders.add(new SpelPlaceholder(fullPlaceholder, expression, startIndex, endIndex));
            log.debug("Extracted SpEL placeholder: {} -> {}", fullPlaceholder, expression);
        }
        
        return placeholders;
    }
    
    /**
     * 替换模板中的占位符
     */
    private String replacePlaceholders(String template, List<SpelPlaceholder> placeholders, EvaluationContext context) {
        StringBuilder result = new StringBuilder(template);
        
        // 从后往前替换，避免位置偏移问题
        for (int i = placeholders.size() - 1; i >= 0; i--) {
            SpelPlaceholder placeholder = placeholders.get(i);
            
            try {
                // 确保变量引用使用正确的语法
                String processedExpression = ensureVariableSyntax(placeholder.expression);
                
                // 对单个SpEL表达式求值
                Expression expression = parser.parseExpression(processedExpression);
                Object value = expression.getValue(context);
                String valueStr = value != null ? value.toString() : "";
                
                // 替换占位符
                result.replace(placeholder.startIndex, placeholder.endIndex, valueStr);
                
                log.debug("Replaced placeholder: {} -> {}", placeholder.fullPlaceholder, valueStr);
                
            } catch (Exception e) {
                String errorValue = failSafe ? "[表达式错误: " + placeholder.expression + "]" : placeholder.fullPlaceholder;
                result.replace(placeholder.startIndex, placeholder.endIndex, errorValue);
                
                log.warn("SpEL placeholder evaluation failed: {}", placeholder.fullPlaceholder, e);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 新增：安全的求值上下文创建
     */
    private EvaluationContext createSafeEvaluationContext(LogContext logContext) {
        EvaluationContext context = mainEvaluator.createEvaluationContext(logContext);
        
        // 验证关键变量是否正确设置
        try {
            Object args = context.lookupVariable("args");
            log.debug("TemplateProcessor中args变量状态: {}, LogContext.args: {}", 
                args != null ? "non-null" : "null",
                logContext != null && logContext.getArgs() != null ? "non-null" : "null");
            
            if (args == null && logContext != null && logContext.getArgs() != null) {
                log.warn("args is null in EvaluationContext but LogContext.args is not null, resetting");
                ((StandardEvaluationContext) context).setVariable("args", logContext.getArgs());
            }
        } catch (Exception e) {
            log.debug("Exception occurred while validating EvaluationContext state", e);
        }
        
        return context;
    }
    
    /**
     * 确保变量引用使用正确的语法
     * 将变量名转换为#variableName格式
     */
    private String ensureVariableSyntax(String expression) {
        // 处理常见的变量引用
        String processed = expression;
        
        // 如果表达式以变量名开头且没有#前缀，添加#前缀
        if (processed.matches("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\[.*\\].*")) {
            // 处理数组/列表访问，如 args[0]
            processed = processed.replaceFirst("^([a-zA-Z_][a-zA-Z0-9_]*)", "#$1");
        } else if (processed.matches("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\..*")) {
            // 处理属性访问，如 result.success
            processed = processed.replaceFirst("^([a-zA-Z_][a-zA-Z0-9_]*)", "#$1");
        } else if (processed.matches("^([a-zA-Z_][a-zA-Z0-9_]*)$")) {
            // 处理简单变量引用，如 args
            processed = "#" + processed;
        }
        
        log.debug("Expression syntax processing: {} -> {}", expression, processed);
        return processed;
    }
}