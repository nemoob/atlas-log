package io.github.nemoob.atlas.log.expression;

import io.github.nemoob.atlas.log.context.LogContext;
import io.github.nemoob.atlas.log.exception.ExpressionEvaluationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpEL表达式评估器 (增强版)
 * 支持在日志注解中使用SpEL表达式来动态构建日志内容
 * 
 * 版本2.0新特性：
 * - 支持中文字符的模板表达式处理
 * - 智能表达式类型检测
 * - 多种表达式处理策略
 * - 更好的错误处理和降级机制
 * 
 * 支持的表达式类型：
 * 1. 纯文本: "查询用户信息", "Get_user_information"
 * 2. 纯SpEL: "#{args[0]}", "#{result.success}"
 * 3. 模板混合: "查询用户信息: 用户ID=#{args[0]}"
 * 
 * 支持的变量：
 * - args: 方法参数数组
 * - result: 方法返回值
 * - exception: 异常对象
 * - methodName: 方法名称
 * - className: 类名称
 * - executionTime: 方法执行时间
 * - 自定义变量通过LogContext传递
 * 
 * @author Atlas Team
 * @since 1.0.0
 * @version 2.0.0
 */
@Slf4j
public class SpelExpressionEvaluator {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    private final boolean cacheEnabled;
    private final long timeoutMs;
    private final boolean failSafe;
    
    // 新增：表达式处理策略映射
    private final Map<ExpressionType, ExpressionProcessor> processors = new HashMap<>();
    
    public SpelExpressionEvaluator(ApplicationContext applicationContext, 
                                   boolean cacheEnabled, 
                                   long timeoutMs, 
                                   boolean failSafe) {
        this.applicationContext = applicationContext;
        this.cacheEnabled = cacheEnabled;
        this.timeoutMs = timeoutMs;
        this.failSafe = failSafe;
        
        // 初始化表达式处理策略
        initializeProcessors();
    }
    
    /**
     * 初始化表达式处理器
     */
    private void initializeProcessors() {
        processors.put(ExpressionType.PLAIN_TEXT, new PlainTextProcessor());
        processors.put(ExpressionType.PURE_SPEL, new PureSpelProcessor(this, failSafe));
        processors.put(ExpressionType.TEMPLATE, new TemplateProcessor(this, failSafe));
    }
    
    /**
     * 评估SpEL表达式 (增强版)
     * 支持中文字符和多种表达式类型
     * 
     * @param expressionString 表达式字符串
     * @param logContext 日志上下文
     * @return 评估结果的字符串表示
     */
    public String evaluateExpression(String expressionString, LogContext logContext) {
        if (expressionString == null || expressionString.trim().isEmpty()) {
            return "";
        }
        
        // 关键：验证LogContext状态完整性
        validateLogContextState(logContext, expressionString);
        
        try {
            // 检测表达式类型
            ExpressionType type = ExpressionTypeDetector.detectType(expressionString);
            log.debug("表达式类型检测: {} -> {}, LogContext.args: {}", 
                expressionString, type, 
                logContext != null && logContext.getArgs() != null ? 
                    Arrays.toString(logContext.getArgs()) : "null");
            
            // 选择对应的处理器
            ExpressionProcessor processor = processors.get(type);
            if (processor == null) {
                throw new IllegalStateException("未找到支持的表达式处理器: " + type);
            }
            
            // 处理表达式前再次验证状态
            validateLogContextState(logContext, expressionString);
            
            String result = processor.processExpression(expressionString, logContext);
            log.debug("表达式处理结果: {} -> {}", expressionString, result);
            
            return result;
            
        } catch (Exception e) {
            String errorMsg = "SpEL表达式评估失败: " + expressionString;
            log.warn(errorMsg + ", LogContext状态: " + describeLogContextState(logContext), e);
            
            if (failSafe) {
                return "[" + errorMsg + "]";
            } else {
                throw new ExpressionEvaluationException(expressionString, errorMsg, e);
            }
        }
    }
    
    /**
     * 评估条件表达式
     * 条件表达式通常是纯SpEL表达式，但也支持模板格式
     * 
     * @param conditionString 条件表达式字符串
     * @param logContext 日志上下文
     * @return 条件评估结果
     */
    public boolean evaluateCondition(String conditionString, LogContext logContext) {
        if (conditionString == null || conditionString.trim().isEmpty()) {
            return true; // 空条件认为总是满足
        }
        
        try {
            // 先尝试使用新的处理逻辑
            String result = evaluateExpression(conditionString, logContext);
            
            // 将结果转换为布尔值
            return convertToBoolean(result);
            
        } catch (Exception e) {
            // 如果新逻辑失败，尝试直接使用SpEL解析(向后兼容)
            try {
                Expression expression = getExpression(conditionString);
                EvaluationContext evaluationContext = createEvaluationContext(logContext);
                
                Object result = evaluateWithTimeout(expression, evaluationContext);
                
                if (result instanceof Boolean) {
                    return (Boolean) result;
                } else if (result != null) {
                    // 非null值认为是true
                    return true;
                } else {
                    return false;
                }
                
            } catch (Exception fallbackException) {
                String errorMsg = "条件表达式评估失败: " + conditionString;
                log.warn(errorMsg, fallbackException);
                
                if (failSafe) {
                    return true; // 安全模式下，条件失败时默认记录日志
                } else {
                    throw new ExpressionEvaluationException(conditionString, errorMsg, fallbackException);
                }
            }
        }
    }
    
    /**
     * 将字符串结果转换为布尔值
     */
    private boolean convertToBoolean(String result) {
        if (result == null || result.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = result.trim().toLowerCase();
        
        // 布尔值字符串
        if ("true".equals(trimmed)) {
            return true;
        }
        if ("false".equals(trimmed)) {
            return false;
        }
        
        // 数字判断
        try {
            double number = Double.parseDouble(trimmed);
            return number != 0.0;
        } catch (NumberFormatException ignored) {
            // 非数字
        }
        
        // 其他非null、非空字符串认为是true
        return true;
    }
    
    /**
     * 获取表达式对象（支持缓存）
     */
    private Expression getExpression(String expressionString) {
        if (cacheEnabled) {
            return expressionCache.computeIfAbsent(expressionString, parser::parseExpression);
        } else {
            return parser.parseExpression(expressionString);
        }
    }
    
    /**
     * 创建SpEL评估上下文
     * package-private 供处理器使用
     */
    EvaluationContext createEvaluationContext(LogContext logContext) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 设置Spring ApplicationContext，支持通过@beanName访问Bean
        if (applicationContext != null) {
            context.setBeanResolver((evaluationContext, beanName) -> {
                try {
                    return applicationContext.getBean(beanName);
                } catch (Exception e) {
                    log.debug("获取Bean失败: {}", beanName, e);
                    return null;
                }
            });
        }
        
        // 设置标准变量
        if (logContext != null) {
            // 线程安全的变量设置，确保LogContext读取的一致性
            synchronized (logContext) {
                Object[] args = logContext.getArgs();
                // 对args进行严格验证和防护
                context.setVariable("args", args != null ? args : new Object[0]);
                
                context.setVariable("result", logContext.getResult());
                context.setVariable("exception", logContext.getException());
                context.setVariable("methodName", 
                    logContext.getMethodName() != null ? logContext.getMethodName() : "unknown");
                context.setVariable("className", 
                    logContext.getClassName() != null ? logContext.getClassName() : "unknown");
                context.setVariable("executionTime", logContext.getExecutionTime());
                context.setVariable("traceId", 
                    logContext.getTraceId() != null ? logContext.getTraceId() : "");
                
                // 设置自定义变量
                Map<String, Object> variables = logContext.getVariables();
                if (variables != null) {
                    variables.forEach(context::setVariable);
                }
                
                // 记录调试信息
                log.debug("创建EvaluationContext - method: {}, args: {}, argsLength: {}", 
                    logContext.getMethodName(),
                    args != null ? "non-null" : "null",
                    args != null ? args.length : 0);
            }
        } else {
            // 当logContext为null时，提供默认值
            log.warn("LogContext为null，使用默认值创建EvaluationContext");
            context.setVariable("args", new Object[0]);
            context.setVariable("result", null);
            context.setVariable("exception", null);
            context.setVariable("methodName", "unknown");
            context.setVariable("className", "unknown");
            context.setVariable("executionTime", 0L);
            context.setVariable("traceId", "");
        }
        
        return context;
    }
    
    /**
     * 带超时的表达式评估
     */
    private Object evaluateWithTimeout(Expression expression, EvaluationContext context) {
        if (timeoutMs <= 0) {
            return expression.getValue(context);
        }
        
        // 简单的超时实现，实际生产环境可能需要更复杂的线程池管理
        long startTime = System.currentTimeMillis();
        Object result = expression.getValue(context);
        long duration = System.currentTimeMillis() - startTime;
        
        if (duration > timeoutMs) {
            log.warn("SpEL表达式执行超时: {}ms > {}ms", duration, timeoutMs);
        }
        
        return result;
    }
    
    /**
     * 清空表达式缓存
     */
    public void clearCache() {
        expressionCache.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return expressionCache.size();
    }
    
    /**
     * 获取支持的表达式类型
     */
    public String[] getSupportedExpressionTypes() {
        return processors.keySet().stream()
                .map(Enum::name)
                .toArray(String[]::new);
    }
    
    /**
     * 测试表达式类型检测(供调试使用)
     */
    public ExpressionType detectExpressionType(String expression) {
        return ExpressionTypeDetector.detectType(expression);
    }
    
    /**
     * 新增：LogContext状态验证方法
     */
    private void validateLogContextState(LogContext logContext, String expression) {
        if (logContext == null) {
            log.warn("LogContext为null，表达式: {}", expression);
            return;
        }
        
        if (expression.contains("args") && logContext.getArgs() == null) {
            log.warn("表达式引用args但LogContext.args为null，表达式: {}, LogContext: {}", 
                expression, describeLogContextState(logContext));
        }
    }
    
    /**
     * 新增：LogContext状态描述方法
     */
    private String describeLogContextState(LogContext logContext) {
        if (logContext == null) {
            return "null";
        }
        
        return String.format("LogContext{methodName='%s', args=%s, argsLength=%d}",
            logContext.getMethodName(),
            logContext.getArgs() != null ? "non-null" : "null",
            logContext.getArgs() != null ? logContext.getArgs().length : 0);
    }
}