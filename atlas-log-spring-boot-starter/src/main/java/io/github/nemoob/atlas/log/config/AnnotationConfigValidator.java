package io.github.nemoob.atlas.log.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Atlas Log 注解配置验证器
 * <p>
 * 负责验证注解配置的合法性，确保配置参数符合要求。
 * </p>
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Component
public class AnnotationConfigValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(AnnotationConfigValidator.class);
    
    // 有效的日志级别
    private static final List<String> VALID_LOG_LEVELS = Arrays.asList(
        "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF"
    );
    
    // 有效的生成器类型
    private static final List<String> VALID_GENERATORS = Arrays.asList(
        "uuid", "snowflake"
    );
    
    // 方法模式正则（支持通配符和包路径）
    private static final Pattern METHOD_PATTERN = Pattern.compile("^[*a-zA-Z_$][a-zA-Z0-9_.$]*\\.[*a-zA-Z_$][a-zA-Z0-9_$]*$");
    
    /**
     * 验证配置
     */
    public void validate(LogConfigProperties config) {
        if (config == null) {
            throw new IllegalArgumentException("Atlas Log configuration cannot be null");
        }
        
        logger.debug("Starting Atlas Log configuration validation...");
        
        try {
            // 基础配置验证
            validateBasicConfig(config);
            
            // 标签配置验证
            validateTagsConfig(config);
            
            // 排除配置验证
            validateExclusionsConfig(config);
            
            // 嵌套配置验证
            validateNestedConfigs(config);
            
            logger.info("Atlas Log configuration validation passed successfully");
            
        } catch (Exception e) {
            logger.error("Atlas Log configuration validation failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Atlas Log configuration: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证基础配置
     */
    private void validateBasicConfig(LogConfigProperties config) {
        // 验证日志级别
        validateLogLevel(config.getDefaultLevel());
        
        // 验证日期格式
        validateDateFormat(config.getDateFormat());
        
        // 验证消息长度限制
        if (config.getMaxMessageLength() <= 0) {
            throw new IllegalArgumentException("maxMessageLength must be positive, got: " + config.getMaxMessageLength());
        }
        
        if (config.getMaxMessageLength() > 100000) {
            logger.warn("maxMessageLength is very large ({}), this might affect performance", config.getMaxMessageLength());
        }
        
        logger.debug("Basic configuration validation passed");
    }
    
    /**
     * 验证日志级别
     */
    private void validateLogLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
            throw new IllegalArgumentException("defaultLevel cannot be null or empty");
        }
        
        String upperLevel = level.toUpperCase();
        if (!VALID_LOG_LEVELS.contains(upperLevel)) {
            throw new IllegalArgumentException("Invalid log level: " + level + 
                ". Valid levels are: " + VALID_LOG_LEVELS);
        }
    }
    
    /**
     * 验证日期格式
     */
    private void validateDateFormat(String dateFormat) {
        if (dateFormat == null || dateFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("dateFormat cannot be null or empty");
        }
        
        try {
            new SimpleDateFormat(dateFormat);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateFormat, e);
        }
    }
    
    /**
     * 验证标签配置
     */
    private void validateTagsConfig(LogConfigProperties config) {
        validateStringList(config.getEnabledTags(), "enabledTags");
        validateStringList(config.getEnabledGroups(), "enabledGroups");
        
        // 检查是否有空的标签或组
        validateNonEmptyElements(config.getEnabledTags(), "enabledTags");
        validateNonEmptyElements(config.getEnabledGroups(), "enabledGroups");
        
        logger.debug("Tags configuration validation passed");
    }
    
    /**
     * 验证排除配置
     */
    private void validateExclusionsConfig(LogConfigProperties config) {
        validateStringList(config.getExclusions(), "exclusions");
        
        // 验证排除模式格式
        for (String exclusion : config.getExclusions()) {
            if (exclusion != null && !exclusion.trim().isEmpty()) {
                validateMethodPattern(exclusion);
            }
        }
        
        logger.debug("Exclusions configuration validation passed");
    }
    
    /**
     * 验证方法模式
     */
    private void validateMethodPattern(String pattern) {
        if (!METHOD_PATTERN.matcher(pattern).matches()) {
            throw new IllegalArgumentException("Invalid method exclusion pattern: " + pattern + 
                ". Pattern should be like 'ClassName.methodName', '*.methodName' or 'ClassName.*'");
        }
    }
    
    /**
     * 验证嵌套配置
     */
    private void validateNestedConfigs(LogConfigProperties config) {
        validateTraceConfig(config.getTraceId());
        validatePerformanceConfig(config.getPerformance());
        validateConditionConfig(config.getCondition());
        validateSensitiveConfig(config.getSensitive());
        
        logger.debug("Nested configurations validation passed");
    }
    
    /**
     * 验证链路追踪配置
     */
    private void validateTraceConfig(LogConfigProperties.TraceIdConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("TraceId configuration cannot be null");
        }
        
        // 验证Header名称
        if (config.getHeaderName() == null || config.getHeaderName().trim().isEmpty()) {
            throw new IllegalArgumentException("TraceId headerName cannot be null or empty");
        }
        
        // 验证生成器类型
        if (config.getGenerator() == null || config.getGenerator().trim().isEmpty()) {
            throw new IllegalArgumentException("TraceId generator cannot be null or empty");
        }
        
        if (!VALID_GENERATORS.contains(config.getGenerator().toLowerCase())) {
            throw new IllegalArgumentException("Invalid TraceId generator: " + config.getGenerator() + 
                ". Valid generators are: " + VALID_GENERATORS);
        }
    }
    
    /**
     * 验证性能监控配置
     */
    private void validatePerformanceConfig(LogConfigProperties.PerformanceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Performance configuration cannot be null");
        }
        
        // 验证慢方法阈值
        if (config.getSlowThreshold() < 0) {
            throw new IllegalArgumentException("Performance slowThreshold must be non-negative, got: " + config.getSlowThreshold());
        }
        
        if (config.getSlowThreshold() > 60000) {
            logger.warn("Performance slowThreshold is very large ({}ms), consider reducing it", config.getSlowThreshold());
        }
    }
    
    /**
     * 验证条件评估配置
     */
    private void validateConditionConfig(LogConfigProperties.ConditionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Condition configuration cannot be null");
        }
        
        // 验证超时时间
        if (config.getTimeoutMs() <= 0) {
            throw new IllegalArgumentException("Condition timeoutMs must be positive, got: " + config.getTimeoutMs());
        }
        
        if (config.getTimeoutMs() > 10000) {
            logger.warn("Condition timeoutMs is very large ({}ms), this might affect performance", config.getTimeoutMs());
        }
    }
    
    /**
     * 验证敏感数据配置
     */
    private void validateSensitiveConfig(LogConfigProperties.SensitiveConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Sensitive configuration cannot be null");
        }
        
        // 验证脱敏标记
        if (config.getMaskValue() == null || config.getMaskValue().isEmpty()) {
            throw new IllegalArgumentException("Sensitive maskValue cannot be null or empty");
        }
        
        // 验证自定义字段
        validateStringList(config.getCustomFields(), "sensitive.customFields");
        validateNonEmptyElements(config.getCustomFields(), "sensitive.customFields");
    }
    
    /**
     * 验证字符串列表
     */
    private void validateStringList(List<String> list, String fieldName) {
        if (list == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }
    
    /**
     * 验证列表元素非空
     */
    private void validateNonEmptyElements(List<String> list, String fieldName) {
        for (int i = 0; i < list.size(); i++) {
            String element = list.get(i);
            if (element == null || element.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + "[" + i + "] cannot be null or empty");
            }
        }
    }
}