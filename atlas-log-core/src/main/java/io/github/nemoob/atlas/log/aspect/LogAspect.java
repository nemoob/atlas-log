package io.github.nemoob.atlas.log.aspect;

import io.github.nemoob.atlas.log.annotation.ExceptionHandler;
import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.annotation.LogIgnore;
import io.github.nemoob.atlas.log.annotation.Logs;
import io.github.nemoob.atlas.log.context.LogContext;
import io.github.nemoob.atlas.log.context.TraceIdHolder;
import io.github.nemoob.atlas.log.expression.SpelExpressionEvaluator;
import io.github.nemoob.atlas.log.serializer.JsonArgumentSerializer;
import io.github.nemoob.atlas.log.util.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 日志切面处理器
 * 拦截带有@Log注解的方法，实现自动日志记录
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Aspect
@Component
@Slf4j
public class LogAspect {
    
    private final SpelExpressionEvaluator spelExpressionEvaluator;
    private final JsonArgumentSerializer argumentSerializer;
    
    public LogAspect(SpelExpressionEvaluator spelExpressionEvaluator,
                     JsonArgumentSerializer argumentSerializer) {
        this.spelExpressionEvaluator = spelExpressionEvaluator;
        this.argumentSerializer = argumentSerializer;
    }
    
    /**
     * 环绕通知：拦截带有@Log注解的方法
     */
    @Around("@annotation(io.github.nemoob.atlas.log.annotation.Log) || @annotation(io.github.nemoob.atlas.log.annotation.Logs)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        
        // 获取所有Log注解
        List<Log> logAnnotations = getAllLogAnnotations(method);
        if (logAnnotations.isEmpty()) {
            return joinPoint.proceed();
        }
        
        // 检查方法是否被@LogIgnore标记
        if (method.isAnnotationPresent(LogIgnore.class)) {
            return joinPoint.proceed();
        }
        
        long startTime = System.currentTimeMillis();
        // AOP 只获取 Filter 设置的 TraceId，不生成新的
        String traceId = TraceIdHolder.getTraceIdIfPresent();
        if (traceId == null) {
            log.warn("TraceId is null in AOP, Filter may not be working properly, method: {}", method.getName());
            traceId = "MISSING-TRACE-ID";
        }
        Object result = null;
        Throwable exception = null;
        
        try {
            // 记录进入日志
            for (Log logAnnotation : logAnnotations) {
                recordEnterLog(logAnnotation, method, args, traceId);
            }
            
            // 执行目标方法
            result = joinPoint.proceed();
            
            return result;
            
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录退出或异常日志
            for (Log logAnnotation : logAnnotations) {
                if (exception != null) {
                    recordExceptionLog(logAnnotation, method, args, exception, executionTime, traceId);
                } else {
                    recordExitLog(logAnnotation, method, args, result, executionTime, traceId);
                }
            }
        }
    }
    
    /**
     * 记录方法进入日志
     */
    private void recordEnterLog(Log logAnnotation, Method method, Object[] args, String traceId) {
        if (!shouldLog(logAnnotation, method, args, null, null)) {
            return;
        }
        
        // 如果没有自定义进入消息，跳过进入日志
        if (logAnnotation.enterMessage().isEmpty()) {
            return;
        }
        
        LogContext logContext = createLogContext(method, args, null, null, 0, traceId);
        
        try {
            String message = spelExpressionEvaluator.evaluateExpression(
                logAnnotation.enterMessage(), logContext);
            
            Logger logger = getLogger(method);
            logWithLevel(logger, logAnnotation.level(), message, buildLogDetails(logAnnotation, logContext));
            
        } catch (Exception e) {
            log.warn("Failed to record enter log: {}", method.getName(), e);
        }
    }
    
    /**
     * 记录方法退出日志
     */
    private void recordExitLog(Log logAnnotation, Method method, Object[] args, 
                              Object result, long executionTime, String traceId) {
        
        LogContext logContext = createLogContext(method, args, result, null, executionTime, traceId);
        
        if (!shouldLog(logAnnotation, method, args, result, null)) {
            return;
        }
        
        try {
            String message = buildLogMessage(logAnnotation, logContext, false);
            Logger logger = getLogger(method);
            logWithLevel(logger, logAnnotation.level(), message, buildLogDetails(logAnnotation, logContext));
            
        } catch (Exception e) {
            log.warn("Failed to record exit log: {}", method.getName(), e);
        }
    }
    
    /**
     * 记录异常日志
     */
    private void recordExceptionLog(Log logAnnotation, Method method, Object[] args, 
                                   Throwable exception, long executionTime, String traceId) {
        
        if (!logAnnotation.logException()) {
            return;
        }
        
        LogContext logContext = createLogContext(method, args, null, exception, executionTime, traceId);
        
        try {
            // 查找匹配的异常处理器
            ExceptionHandler exceptionHandler = findExceptionHandler(exception, logAnnotation.exceptionHandlers());
            
            String message;
            io.github.nemoob.atlas.log.annotation.LogLevel logLevel;
            boolean logStackTrace;
            
            if (exceptionHandler != null) {
                // 使用自定义异常处理器
                message = exceptionHandler.message().isEmpty() ? 
                    buildLogMessage(logAnnotation, logContext, true) :
                    spelExpressionEvaluator.evaluateExpression(exceptionHandler.message(), logContext);
                logLevel = exceptionHandler.level();
                logStackTrace = exceptionHandler.logStackTrace();
            } else {
                // 使用默认异常处理
                message = logAnnotation.exceptionMessage().isEmpty() ?
                    buildLogMessage(logAnnotation, logContext, true) :
                    spelExpressionEvaluator.evaluateExpression(logAnnotation.exceptionMessage(), logContext);
                logLevel = io.github.nemoob.atlas.log.annotation.LogLevel.ERROR;
                logStackTrace = true;
            }
            
            Logger logger = getLogger(method);
            String logDetails = buildLogDetails(logAnnotation, logContext);
            
            if (logStackTrace) {
                logWithLevel(logger, logLevel, message, logDetails, exception);
            } else {
                logWithLevel(logger, logLevel, message, logDetails);
            }
            
        } catch (Exception e) {
            log.warn("Failed to record exception log: {}", method.getName(), e);
        }
    }
    
    /**
     * 判断是否应该记录日志
     */
    private boolean shouldLog(Log logAnnotation, Method method, Object[] args, Object result, Throwable exception) {
        // 检查条件表达式
        if (!logAnnotation.condition().isEmpty()) {
            LogContext logContext = createLogContext(method, args, result, exception, 0, TraceIdHolder.getTraceId());
            try {
                return spelExpressionEvaluator.evaluateCondition(logAnnotation.condition(), logContext);
            } catch (Exception e) {
                log.warn("Condition expression evaluation failed, logging by default: {}", logAnnotation.condition(), e);
                return true;
            }
        }
        
        return true;
    }
    
    /**
     * 构建日志消息
     */
    private String buildLogMessage(Log logAnnotation, LogContext logContext, boolean isException) {
        // 在调用SpEL求值前验证LogContext状态
        if (logContext == null) {
            log.warn("buildLogMessage received null LogContext");
            return "[LogContext为null]";
        }
        
        if (logContext.getArgs() == null) {
            log.warn("LogContext.args is null in buildLogMessage, method: {}", logContext.getMethodName());
        }
        
        if (isException && !logAnnotation.exceptionMessage().isEmpty()) {
            return spelExpressionEvaluator.evaluateExpression(logAnnotation.exceptionMessage(), logContext);
        } else if (!isException && !logAnnotation.exitMessage().isEmpty()) {
            return spelExpressionEvaluator.evaluateExpression(logAnnotation.exitMessage(), logContext);
        } else if (!logAnnotation.value().isEmpty()) {
            return spelExpressionEvaluator.evaluateExpression(logAnnotation.value(), logContext);
        } else {
            // 默认消息
            String action = isException ? "执行异常" : "执行完成";
            return String.format("%s: %s", action, logContext.getMethodName());
        }
    }
    
    /**
     * 构建日志详细信息
     */
    private String buildLogDetails(Log logAnnotation, LogContext logContext) {
        StringBuilder details = new StringBuilder();
        
        // TraceId
        if (logContext.getTraceId() != null) {
            details.append("TraceId: ").append(logContext.getTraceId()).append(" | ");
        }
        
        // 标签
        if (logAnnotation.tags().length > 0) {
            details.append("Tags: ").append(Arrays.toString(logAnnotation.tags())).append(" | ");
        }
        
        // 参数
        if (logAnnotation.logArgs() && logContext.getArgs() != null) {
            try {
                String argsStr = serializeArgs(logContext.getArgs(), logAnnotation, 
                    getMethodParameters(logContext.getMethodSignature()));
                details.append("Args: ").append(argsStr).append(" | ");
            } catch (Exception e) {
                details.append("Args: [序列化失败] | ");
            }
        }
        
        // 返回值
        if (logAnnotation.logResult() && logContext.getResult() != null) {
            try {
                String resultStr = argumentSerializer.serializeResult(logContext.getResult(), logAnnotation);
                details.append("Result: ").append(resultStr).append(" | ");
            } catch (Exception e) {
                details.append("Result: [序列化失败] | ");
            }
        }
        
        // 执行时间
        if (logAnnotation.logExecutionTime()) {
            details.append("ExecutionTime: ").append(logContext.getExecutionTime()).append("ms | ");
        }
        
        // 异常信息
        if (logContext.getException() != null) {
            details.append("Exception: ").append(logContext.getException().getClass().getSimpleName())
                   .append(": ").append(logContext.getException().getMessage()).append(" | ");
        }
        
        // 移除最后的分隔符
        if (details.length() > 3) {
            details.setLength(details.length() - 3);
        }
        
        return details.toString();
    }
    
    /**
     * 序列化参数（考虑@LogIgnore注解）
     */
    private String serializeArgs(Object[] args, Log logAnnotation, Parameter[] parameters) {
        if (argumentSerializer instanceof JsonArgumentSerializer) {
            JsonArgumentSerializer jsonSerializer = (JsonArgumentSerializer) argumentSerializer;
            return jsonSerializer.serializeArgsWithParameterInfo(args, logAnnotation, parameters);
        } else {
            return argumentSerializer.serializeArgs(args, logAnnotation);
        }
    }
    
    /**
     * 获取方法参数信息
     */
    private Parameter[] getMethodParameters(String methodSignature) {
        // 这里需要根据方法签名获取Method对象，然后获取参数
        // 简化实现，实际使用中可以通过缓存优化
        return new Parameter[0];
    }
    
    /**
     * 创建日志上下文
     */
    private LogContext createLogContext(Method method, Object[] args, Object result, 
                                       Throwable exception, long executionTime, String traceId) {
        // 确保args参数的原子性设置
        Object[] safeArgs;
        if (args != null) {
            // 创建防御性副本，避免并发修改
            safeArgs = Arrays.copyOf(args, args.length);
        } else {
            safeArgs = new Object[0];
        }
        
        LogContext context = new LogContext()
                .setTraceId(traceId != null ? traceId : "")
                .setClassName(method.getDeclaringClass().getSimpleName())
                .setMethodName(method.getName())
                .setMethodSignature(ReflectionUtils.formatMethodSignature(method))
                .setArgs(safeArgs)  // 使用安全副本
                .setResult(result)
                .setException(exception)
                .setExecutionTime(executionTime);
        
        // 验证创建的LogContext状态
        log.debug("创建LogContext: method={}, args={}, argsLength={}", 
            method.getName(), 
            safeArgs != null ? "non-null" : "null",
            safeArgs != null ? safeArgs.length : 0);
        
        return context;
    }
    
    /**
     * 获取所有Log注解
     */
    private List<Log> getAllLogAnnotations(Method method) {
        List<Log> annotations = new ArrayList<>();
        
        // 检查方法级别的注解
        Log singleLog = AnnotationUtils.findAnnotation(method, Log.class);
        if (singleLog != null) {
            annotations.add(singleLog);
        }
        
        Logs multiLogs = AnnotationUtils.findAnnotation(method, Logs.class);
        if (multiLogs != null) {
            annotations.addAll(Arrays.asList(multiLogs.value()));
        }
        
        // 检查类级别的注解
        Class<?> clazz = method.getDeclaringClass();
        Log classSingleLog = AnnotationUtils.findAnnotation(clazz, Log.class);
        if (classSingleLog != null) {
            annotations.add(classSingleLog);
        }
        
        Logs classMultiLogs = AnnotationUtils.findAnnotation(clazz, Logs.class);
        if (classMultiLogs != null) {
            annotations.addAll(Arrays.asList(classMultiLogs.value()));
        }
        
        return annotations;
    }
    
    /**
     * 查找匹配的异常处理器
     */
    private ExceptionHandler findExceptionHandler(Throwable exception, ExceptionHandler[] handlers) {
        if (handlers == null || handlers.length == 0) {
            return null;
        }
        
        Class<?> exceptionClass = exception.getClass();
        
        // 精确匹配
        for (ExceptionHandler handler : handlers) {
            if (handler.exception().equals(exceptionClass)) {
                return handler;
            }
        }
        
        // 继承匹配
        for (ExceptionHandler handler : handlers) {
            if (handler.exception().isAssignableFrom(exceptionClass)) {
                return handler;
            }
        }
        
        return null;
    }
    
    /**
     * 获取日志记录器
     */
    private Logger getLogger(Method method) {
        return LoggerFactory.getLogger(method.getDeclaringClass());
    }
    
    /**
     * 根据级别记录日志
     */
    private void logWithLevel(Logger logger, io.github.nemoob.atlas.log.annotation.LogLevel level, 
                             String message, String details) {
        // 统一格式：details 包含 TraceId 和条件信息在前，message 是用户具体内容在后
        String fullMessage = String.format("%s | %s", details, message);
        
        switch (level) {
            case TRACE:
                logger.trace(fullMessage);
                break;
            case DEBUG:
                logger.debug(fullMessage);
                break;
            case INFO:
                logger.info(fullMessage);
                break;
            case WARN:
                logger.warn(fullMessage);
                break;
            case ERROR:
                logger.error(fullMessage);
                break;
        }
    }
    
    /**
     * 根据级别记录日志（带异常）
     */
    private void logWithLevel(Logger logger, io.github.nemoob.atlas.log.annotation.LogLevel level, 
                             String message, String details, Throwable exception) {
        // 统一格式：details 包含 TraceId 和条件信息在前，message 是用户具体内容在后
        String fullMessage = String.format("%s | %s", details, message);
        
        switch (level) {
            case TRACE:
                logger.trace(fullMessage, exception);
                break;
            case DEBUG:
                logger.debug(fullMessage, exception);
                break;
            case INFO:
                logger.info(fullMessage, exception);
                break;
            case WARN:
                logger.warn(fullMessage, exception);
                break;
            case ERROR:
                logger.error(fullMessage, exception);
                break;
        }
    }
}