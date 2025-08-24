package io.github.nemoob.atlas.log.expression;

import io.github.nemoob.atlas.log.context.LogContext;

/**
 * 表达式处理策略接口
 * 定义了不同类型表达式的处理方式
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public interface ExpressionProcessor {
    
    /**
     * 处理表达式
     * 
     * @param expression 表达式字符串
     * @param logContext 日志上下文
     * @return 处理后的字符串结果
     */
    String processExpression(String expression, LogContext logContext);
    
    /**
     * 检查是否能处理指定类型的表达式
     * 
     * @param expression 表达式字符串
     * @return 如果能处理返回true，否则返回false
     */
    boolean canHandle(String expression);
    
    /**
     * 获取支持的表达式类型
     * 
     * @return 表达式类型
     */
    ExpressionType getSupportedType();
}