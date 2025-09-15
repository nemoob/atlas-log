package io.github.nemoob.atlas.log.expression;

import io.github.nemoob.atlas.log.context.LogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 纯SpEL表达式处理器
 * 处理整个字符串都是SpEL表达式的情况，如: #{args[0]}
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Slf4j
public class PureSpelProcessor implements ExpressionProcessor {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final SpelExpressionEvaluator mainEvaluator;
    private final boolean failSafe;
    
    public PureSpelProcessor(SpelExpressionEvaluator mainEvaluator, boolean failSafe) {
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
            log.warn("PureSpelProcessor received null LogContext, expression: {}", expression);
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
            // 对于纯SpEL表达式，需要移除#{...}包装
            String cleanExpression = expression.trim();
            if (cleanExpression.startsWith("#{") && cleanExpression.endsWith("}")) {
                cleanExpression = cleanExpression.substring(2, cleanExpression.length() - 1);
            }
            
            // 确保变量引用使用正确的语法 (#variableName)
            String processedExpression = ensureVariableSyntax(cleanExpression);
            
            // 解析并求值SpEL表达式
            Expression spelExpression = parser.parseExpression(processedExpression);
            EvaluationContext evaluationContext = createSafeEvaluationContext(logContext);
            
            Object result = spelExpression.getValue(evaluationContext);
            return result != null ? result.toString() : "";
            
        } catch (Exception e) {
            String errorMsg = "纯SpEL表达式处理失败: " + expression;
            log.warn(errorMsg, e);
            
            if (failSafe) {
                return "[" + errorMsg + "]";
            } else {
                // 重新抛出异常让上层处理
                throw new RuntimeException(errorMsg, e);
            }
        }
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
    
    /**
     * 新增：安全的求值上下文创建
     */
    private EvaluationContext createSafeEvaluationContext(LogContext logContext) {
        EvaluationContext context = mainEvaluator.createEvaluationContext(logContext);
        
        // 验证关键变量是否正确设置
        try {
            Object args = context.lookupVariable("args");
            log.debug("PureSpelProcessor中args变量状态: {}, LogContext.args: {}", 
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
    
    @Override
    public boolean canHandle(String expression) {
        return ExpressionTypeDetector.detectType(expression) == ExpressionType.PURE_SPEL;
    }
    
    @Override
    public ExpressionType getSupportedType() {
        return ExpressionType.PURE_SPEL;
    }
}