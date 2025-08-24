package io.github.nemoob.atlas.log.exception;

/**
 * SpEL表达式评估异常
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public class ExpressionEvaluationException extends LogException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 表达式内容
     */
    private final String expression;
    
    public ExpressionEvaluationException(String expression, String message) {
        super("表达式评估失败: " + expression + ", 原因: " + message);
        this.expression = expression;
    }
    
    public ExpressionEvaluationException(String expression, String message, Throwable cause) {
        super("表达式评估失败: " + expression + ", 原因: " + message, cause);
        this.expression = expression;
    }
    
    public ExpressionEvaluationException(String expression, Throwable cause) {
        super("表达式评估失败: " + expression, cause);
        this.expression = expression;
    }
    
    /**
     * 获取出错的表达式
     * 
     * @return 表达式内容
     */
    public String getExpression() {
        return expression;
    }
}