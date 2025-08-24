package io.github.nemoob.atlas.log.expression;

import io.github.nemoob.atlas.log.context.LogContext;

/**
 * 纯文本表达式处理器
 * 处理不包含任何SpEL表达式的纯文本字符串
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public class PlainTextProcessor implements ExpressionProcessor {
    
    @Override
    public String processExpression(String expression, LogContext logContext) {
        // 纯文本直接返回原文
        return expression != null ? expression : "";
    }
    
    @Override
    public boolean canHandle(String expression) {
        return ExpressionTypeDetector.detectType(expression) == ExpressionType.PLAIN_TEXT;
    }
    
    @Override
    public ExpressionType getSupportedType() {
        return ExpressionType.PLAIN_TEXT;
    }
}