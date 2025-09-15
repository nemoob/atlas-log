package io.github.nemoob.atlas.log.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Atlas Log 注解配置处理器
 * <p>
 * 负责解析 @EnableAtlasLog 注解的属性，并将其转换为 LogConfigProperties 对象。
 * 处理器在Spring容器初始化时自动执行，将注解配置注册到容器中。
 * </p>
 * 
 * @author nemoob
 * @since 0.2.0
 */
public class AtlasLogAnnotationConfigProcessor implements BeanPostProcessor, ApplicationContextAware {
    
    private static final Logger logger = LoggerFactory.getLogger(AtlasLogAnnotationConfigProcessor.class);
    
    private final Map<String, Object> annotationAttributes;
    private ApplicationContext applicationContext;
    private LogConfigProperties annotationConfig;
    
    public AtlasLogAnnotationConfigProcessor(Map<String, Object> annotationAttributes) {
        this.annotationAttributes = annotationAttributes;
        logger.debug("=== AtlasLogAnnotationConfigProcessor Constructor Debug ===");
        logger.debug("Received annotationAttributes: {}", annotationAttributes);
        if (annotationAttributes != null) {
            logger.debug("annotationAttributes keys: {}", annotationAttributes.keySet());
            Object httpLog = annotationAttributes.get("httpLog");
            logger.debug("httpLog attribute: {}", httpLog);
            logger.debug("httpLog type: {}", httpLog != null ? httpLog.getClass() : "null");
            if (httpLog instanceof Map) {
                Map<String, Object> httpLogMap = (Map<String, Object>) httpLog;
                logger.debug("httpLog map keys: {}", httpLogMap.keySet());
                Object urlFormat = httpLogMap.get("urlFormat");
                logger.debug("urlFormat from httpLog: '{}'", urlFormat);
            }
        } else {
            logger.debug("annotationAttributes is null!");
        }
        logger.debug("============================================================");
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @PostConstruct
    public void processAnnotationConfig() {
        logger.info("Processing Atlas Log annotation configuration...");
        
        try {
            // 解析注解配置
            this.annotationConfig = parseAnnotationConfig();
            logger.info("Annotation configuration processed successfully");
            logger.debug("Parsed configuration: {}", this.annotationConfig);
        } catch (Exception e) {
            logger.error("Failed to process annotation configuration", e);
            throw new IllegalStateException("Failed to process Atlas Log annotation configuration", e);
        }
    }
    
    /**
     * 获取解析后的注解配置
     */
    public LogConfigProperties getAnnotationConfig() {
        return this.annotationConfig;
    }
    
    /**
     * 解析注解配置
     */
    private LogConfigProperties parseAnnotationConfig() {
        LogConfigProperties config = new LogConfigProperties();
        
        // 基础配置解析
        parseBasicConfig(config);
        
        // 标签和组配置解析
        parseTagsAndGroupsConfig(config);
        
        // 排除配置解析
        parseExclusionsConfig(config);
        
        // 嵌套配置解析
        parseNestedConfigs(config);
        
        return config;
    }
    
    /**
     * 解析基础配置
     */
    private void parseBasicConfig(LogConfigProperties config) {
        config.setEnabled(getAttributeValue("enabled", Boolean.class, true));
        config.setDefaultLevel(getAttributeValue("defaultLevel", String.class, "INFO"));
        config.setDateFormat(getAttributeValue("dateFormat", String.class, "yyyy-MM-dd HH:mm:ss.SSS"));
        config.setPrettyPrint(getAttributeValue("prettyPrint", Boolean.class, false));
        config.setMaxMessageLength(getAttributeValue("maxMessageLength", Integer.class, 2000));
        config.setSpelEnabled(getAttributeValue("spelEnabled", Boolean.class, true));
        config.setConditionEnabled(getAttributeValue("conditionEnabled", Boolean.class, true));
        
        logger.debug("Parsed basic configuration: enabled={}, defaultLevel={}, dateFormat={}", 
                    config.isEnabled(), config.getDefaultLevel(), config.getDateFormat());
    }
    
    /**
     * 解析标签和组配置
     */
    private void parseTagsAndGroupsConfig(LogConfigProperties config) {
        String[] enabledTags = getAttributeValue("enabledTags", String[].class, 
                                                new String[]{"business", "security", "api"});
        config.setEnabledTags(Arrays.asList(enabledTags));
        
        String[] enabledGroups = getAttributeValue("enabledGroups", String[].class, 
                                                  new String[]{"default", "business"});
        config.setEnabledGroups(Arrays.asList(enabledGroups));
        
        logger.debug("Parsed tags and groups: enabledTags={}, enabledGroups={}", 
                    config.getEnabledTags(), config.getEnabledGroups());
    }
    
    /**
     * 解析排除配置
     */
    private void parseExclusionsConfig(LogConfigProperties config) {
        String[] exclusions = getAttributeValue("exclusions", String[].class, 
                                               new String[]{"*.toString", "*.hashCode", "*.equals"});
        config.setExclusions(Arrays.asList(exclusions));
        
        logger.debug("Parsed exclusions: {}", config.getExclusions());
    }
    
    /**
     * 解析嵌套配置
     */
    private void parseNestedConfigs(LogConfigProperties config) {
        parseTraceConfig(config);
        parsePerformanceConfig(config);
        parseConditionConfig(config);
        parseSensitiveConfig(config);
        parseHttpLogConfig(config);
        parseResultLogConfig(config);
    }
    
    /**
     * 解析链路追踪配置
     */
    private void parseTraceConfig(LogConfigProperties config) {
        Map<String, Object> traceAttrs = getAttributeValue("trace", Map.class, null);
        if (traceAttrs != null) {
            LogConfigProperties.TraceIdConfig traceConfig = config.getTraceId();
            traceConfig.setEnabled(getNestedAttributeValue(traceAttrs, "enabled", Boolean.class, true));
            traceConfig.setHeaderName(getNestedAttributeValue(traceAttrs, "headerName", String.class, "X-Trace-Id"));
            traceConfig.setGenerator(getNestedAttributeValue(traceAttrs, "generator", String.class, "uuid"));
            
            logger.debug("Parsed trace configuration: {}", traceConfig);
        }
    }
    
    /**
     * 解析性能监控配置
     */
    private void parsePerformanceConfig(LogConfigProperties config) {
        Map<String, Object> perfAttrs = getAttributeValue("performance", Map.class, null);
        if (perfAttrs != null) {
            LogConfigProperties.PerformanceConfig perfConfig = config.getPerformance();
            perfConfig.setEnabled(getNestedAttributeValue(perfAttrs, "enabled", Boolean.class, true));
            perfConfig.setSlowThreshold(getNestedAttributeValue(perfAttrs, "slowThreshold", Long.class, 1000L));
            perfConfig.setLogSlowMethods(getNestedAttributeValue(perfAttrs, "logSlowMethods", Boolean.class, true));
            
            logger.debug("Parsed performance configuration: {}", perfConfig);
        }
    }
    
    /**
     * 解析条件评估配置
     */
    private void parseConditionConfig(LogConfigProperties config) {
        Map<String, Object> conditionAttrs = getAttributeValue("condition", Map.class, null);
        if (conditionAttrs != null) {
            LogConfigProperties.ConditionConfig conditionConfig = config.getCondition();
            conditionConfig.setCacheEnabled(getNestedAttributeValue(conditionAttrs, "cacheEnabled", Boolean.class, true));
            conditionConfig.setTimeoutMs(getNestedAttributeValue(conditionAttrs, "timeoutMs", Long.class, 1000L));
            conditionConfig.setFailSafe(getNestedAttributeValue(conditionAttrs, "failSafe", Boolean.class, true));
            
            logger.debug("Parsed condition configuration: {}", conditionConfig);
        }
    }
    
    /**
     * 解析敏感数据配置
     */
    private void parseSensitiveConfig(LogConfigProperties config) {
        Map<String, Object> sensitiveAttrs = getAttributeValue("sensitive", Map.class, null);
        if (sensitiveAttrs != null) {
            LogConfigProperties.SensitiveConfig sensitiveConfig = config.getSensitive();
            sensitiveConfig.setEnabled(getNestedAttributeValue(sensitiveAttrs, "enabled", Boolean.class, true));
            sensitiveConfig.setMaskValue(getNestedAttributeValue(sensitiveAttrs, "maskValue", String.class, "***"));
            
            String[] customFields = getNestedAttributeValue(sensitiveAttrs, "customFields", String[].class, 
                                                          new String[]{"bankCard", "idCard", "socialSecurityNumber"});
            sensitiveConfig.setCustomFields(Arrays.asList(customFields));
            
            logger.debug("Parsed sensitive configuration: {}", sensitiveConfig);
        }
    }
    
    /**
     * 解析HTTP日志配置
     */
    private void parseHttpLogConfig(LogConfigProperties config) {
        Map<String, Object> httpLogAttrs = getAttributeValue("httpLog", Map.class, null);
        logger.debug("parseHttpLogConfig - httpLogAttrs: {}", httpLogAttrs);
        
        if (httpLogAttrs != null) {
            LogConfigProperties.HttpLogConfig httpLogConfig = config.getHttpLog();
            
            // 解析 urlFormat
            String urlFormat = getNestedAttributeValue(httpLogAttrs, "urlFormat", String.class, "");
            logger.debug("parseHttpLogConfig - urlFormat from annotation: '{}'", urlFormat);
            httpLogConfig.setUrlFormat(urlFormat);
            
            // 解析其他属性
            httpLogConfig.setLogFullParameters(getNestedAttributeValue(httpLogAttrs, "logFullParameters", Boolean.class, true));
            httpLogConfig.setIncludeQueryString(getNestedAttributeValue(httpLogAttrs, "includeQueryString", Boolean.class, true));
            httpLogConfig.setIncludeHeaders(getNestedAttributeValue(httpLogAttrs, "includeHeaders", Boolean.class, false));
            
            String[] excludeHeaders = getNestedAttributeValue(httpLogAttrs, "excludeHeaders", String[].class, 
                                                             new String[]{"authorization", "cookie", "x-auth-token"});
            httpLogConfig.setExcludeHeaders(Arrays.asList(excludeHeaders));
            
            logger.debug("parseHttpLogConfig - Final HTTP log configuration: urlFormat='{}', includeQueryString={}, logFullParameters={}", 
                        httpLogConfig.getUrlFormat(), httpLogConfig.isIncludeQueryString(), httpLogConfig.isLogFullParameters());
        } else {
            logger.debug("parseHttpLogConfig - No httpLog annotation found, using defaults");
        }
    }
    
    /**
     * 解析返回值记录配置
     */
    private void parseResultLogConfig(LogConfigProperties config) {
        Map<String, Object> resultLogAttrs = getAttributeValue("resultLog", Map.class, null);
        if (resultLogAttrs != null) {
            LogConfigProperties.ResultLogConfig resultLogConfig = config.getResultLog();
            resultLogConfig.setEnabled(getNestedAttributeValue(resultLogAttrs, "enabled", Boolean.class, true));
            resultLogConfig.setMaxLength(getNestedAttributeValue(resultLogAttrs, "maxLength", Integer.class, 1000));
            resultLogConfig.setPrintAll(getNestedAttributeValue(resultLogAttrs, "printAll", Boolean.class, false));
            resultLogConfig.setTruncateMessage(getNestedAttributeValue(resultLogAttrs, "truncateMessage", String.class, "[TRUNCATED]"));
            
            logger.debug("Parsed result log configuration: {}", resultLogConfig);
        }
    }
    
    /**
     * 获取注解属性值
     */
    @SuppressWarnings("unchecked")
    private <T> T getAttributeValue(String attributeName, Class<T> expectedType, T defaultValue) {
        Object value = annotationAttributes.get(attributeName);
        if (value == null) {
            return defaultValue;
        }
        
        if (expectedType.isInstance(value)) {
            return (T) value;
        }
        
        logger.warn("Attribute '{}' expected type {} but got {}, using default value", 
                   attributeName, expectedType.getSimpleName(), value.getClass().getSimpleName());
        return defaultValue;
    }
    
    /**
     * 获取嵌套注解属性值
     */
    @SuppressWarnings("unchecked")
    private <T> T getNestedAttributeValue(Map<String, Object> nestedAttrs, String attributeName, 
                                        Class<T> expectedType, T defaultValue) {
        Object value = nestedAttrs.get(attributeName);
        if (value == null) {
            return defaultValue;
        }
        
        if (expectedType.isInstance(value)) {
            return (T) value;
        }
        
        logger.warn("Nested attribute '{}' expected type {} but got {}, using default value", 
                   attributeName, expectedType.getSimpleName(), value.getClass().getSimpleName());
        return defaultValue;
    }
}