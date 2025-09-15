package io.github.nemoob.atlas.log.processor;

import io.github.nemoob.atlas.log.annotation.JsonPathCompare;
import io.github.nemoob.atlas.log.annotation.LogLevel;
import io.github.nemoob.atlas.log.comparator.JsonPathValueExtractor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JsonPath 比较处理器
 * 负责处理 @JsonPathCompare 注解，执行值提取和比较逻辑
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Slf4j
public class JsonPathCompareProcessor {
    
    private final JsonPathValueExtractor valueExtractor;
    
    public JsonPathCompareProcessor(JsonPathValueExtractor valueExtractor) {
        this.valueExtractor = valueExtractor;
    }
    
    /**
     * 处理 JsonPath 比较注解
     * 
     * @param method 方法对象
     * @param args 方法参数
     * @param result 方法返回值
     * @param beforeArgs 方法执行前的参数（用于 BEFORE_VS_AFTER 模式）
     */
    public void processJsonPathCompare(Method method, Object[] args, Object result, Object[] beforeArgs) {
        JsonPathCompare annotation = method.getAnnotation(JsonPathCompare.class);
        if (annotation == null) {
            // 检查类级别的注解
            annotation = method.getDeclaringClass().getAnnotation(JsonPathCompare.class);
        }
        
        if (annotation == null || annotation.value().length == 0) {
            return;
        }
        
        try {
            processComparison(annotation, method, args, result, beforeArgs);
        } catch (Exception e) {
            handleFailure(annotation, method, e);
        }
    }
    
    /**
     * 执行比较逻辑
     */
    private void processComparison(JsonPathCompare annotation, Method method, 
                                 Object[] args, Object result, Object[] beforeArgs) {
        List<String> jsonPaths = Arrays.asList(annotation.value());
        Logger methodLogger = LoggerFactory.getLogger(method.getDeclaringClass());
        
        switch (annotation.mode()) {
            case ARGS_VS_RESULT:
                processArgsVsResult(annotation, method, args, result, jsonPaths, methodLogger);
                break;
            case BEFORE_VS_AFTER:
                processBeforeVsAfter(annotation, method, beforeArgs, args, jsonPaths, methodLogger);
                break;
            case EXTRACT_ONLY:
                processExtractOnly(annotation, method, args, result, jsonPaths, methodLogger);
                break;
        }
    }
    
    /**
     * 处理参数 vs 返回值比较
     */
    private void processArgsVsResult(JsonPathCompare annotation, Method method, 
                                   Object[] args, Object result, List<String> jsonPaths, Logger logger) {
        // 将参数数组转换为单个对象进行处理
        Object argsObject = createArgsObject(args);
        
        List<JsonPathValueExtractor.ValueComparisonResult> results = 
            valueExtractor.compareValues(argsObject, result, jsonPaths);
        
        if (annotation.logComparison()) {
            logComparisonResults(annotation, method, results, logger);
        }
    }
    
    /**
     * 处理执行前 vs 执行后比较
     */
    private void processBeforeVsAfter(JsonPathCompare annotation, Method method, 
                                    Object[] beforeArgs, Object[] afterArgs, 
                                    List<String> jsonPaths, Logger logger) {
        Object beforeObject = createArgsObject(beforeArgs);
        Object afterObject = createArgsObject(afterArgs);
        
        List<JsonPathValueExtractor.ValueComparisonResult> results = 
            valueExtractor.compareValues(beforeObject, afterObject, jsonPaths);
        
        if (annotation.logComparison()) {
            logComparisonResults(annotation, method, results, logger);
        }
    }
    
    /**
     * 处理仅提取模式
     */
    private void processExtractOnly(JsonPathCompare annotation, Method method, 
                                  Object[] args, Object result, List<String> jsonPaths, Logger logger) {
        Object argsObject = createArgsObject(args);
        
        // 提取参数值
        Map<String, Object> argsValues = valueExtractor.extractValues(argsObject, jsonPaths);
        
        // 提取返回值
        Map<String, Object> resultValues = valueExtractor.extractValues(result, jsonPaths);
        
        if (annotation.logComparison()) {
            logExtractedValues(annotation, method, argsValues, resultValues, logger);
        }
    }
    
    /**
     * 创建参数对象
     */
    private Object createArgsObject(Object[] args) {
        if (args == null || args.length == 0) {
            return new Object();
        }
        
        if (args.length == 1) {
            return args[0];
        }
        
        // 多个参数时，创建一个包装对象
        Map<String, Object> argsMap = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            argsMap.put("arg" + i, args[i]);
        }
        return argsMap;
    }
    
    /**
     * 记录比较结果
     */
    private void logComparisonResults(JsonPathCompare annotation, Method method, 
                                    List<JsonPathValueExtractor.ValueComparisonResult> results, Logger logger) {
        for (JsonPathValueExtractor.ValueComparisonResult result : results) {
            String message = formatMessage(annotation.messageTemplate(), method, result);
            
            // 根据是否相等和配置决定日志级别
            LogLevel logLevel = annotation.logLevel();
            if (!result.isEqual() && annotation.logDifferences()) {
                // 值不相等时可能需要更高的日志级别
                logWithLevel(logger, logLevel, message);
            } else if (result.isEqual() || !annotation.logDifferences()) {
                logWithLevel(logger, logLevel, message);
            }
        }
    }
    
    /**
     * 记录提取的值
     */
    private void logExtractedValues(JsonPathCompare annotation, Method method, 
                                  Map<String, Object> argsValues, Map<String, Object> resultValues, 
                                  Logger logger) {
        for (String path : argsValues.keySet()) {
            Object argValue = argsValues.get(path);
            Object resultValue = resultValues.get(path);
            
            String message = String.format("JsonPath提取: %s | 方法: %s | 参数值: %s | 返回值: %s", 
                path, method.getName(), argValue, resultValue);
            
            logWithLevel(logger, annotation.logLevel(), message);
        }
    }
    
    /**
     * 格式化消息
     */
    private String formatMessage(String template, Method method, JsonPathValueExtractor.ValueComparisonResult result) {
        return template
            .replace("{path}", result.getJsonPath())
            .replace("{value1}", String.valueOf(result.getValue1()))
            .replace("{value2}", String.valueOf(result.getValue2()))
            .replace("{equal}", String.valueOf(result.isEqual()))
            .replace("{method}", method.getName());
    }
    
    /**
     * 根据级别记录日志
     */
    private void logWithLevel(Logger logger, LogLevel level, String message) {
        switch (level) {
            case TRACE:
                if (logger.isTraceEnabled()) {
                    logger.trace(message);
                }
                break;
            case DEBUG:
                if (logger.isDebugEnabled()) {
                    logger.debug(message);
                }
                break;
            case INFO:
                if (logger.isInfoEnabled()) {
                    logger.info(message);
                }
                break;
            case WARN:
                if (logger.isWarnEnabled()) {
                    logger.warn(message);
                }
                break;
            case ERROR:
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                break;
        }
    }
    
    /**
     * 处理失败情况
     */
    private void handleFailure(JsonPathCompare annotation, Method method, Exception e) {
        String errorMessage = String.format("JsonPath比较失败: 方法=%s, 错误=%s", 
            method.getName(), e.getMessage());
        
        switch (annotation.onFailure()) {
            case LOG_WARNING:
                log.warn(errorMessage, e);
                break;
            case LOG_ERROR:
                log.error(errorMessage, e);
                break;
            case IGNORE:
                // 静默忽略
                break;
            case THROW_EXCEPTION:
                throw new RuntimeException("JsonPath comparison failed: " + errorMessage, e);
        }
    }
    
    /**
     * 检查是否启用了 JsonPath 比较功能
     */
    public boolean isEnabled() {
        return valueExtractor != null && JsonPathValueExtractor.isJsonPathAvailable();
    }
}