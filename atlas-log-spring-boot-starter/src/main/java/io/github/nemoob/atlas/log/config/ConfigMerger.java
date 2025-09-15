package io.github.nemoob.atlas.log.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Atlas Log 配置合并器
 * <p>
 * 负责合并注解配置和属性文件配置，按照优先级策略进行配置合并。
 * 优先级：注解配置 > application.yml > 环境变量 > 默认值
 * </p>
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConfigMerger implements BeanFactoryPostProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigMerger.class);
    
    private static final String ANNOTATION_CONFIG_PROCESSOR_BEAN_NAME = "atlasLogAnnotationConfigProcessor";
    private static final String LOG_CONFIG_PROPERTIES_BEAN_NAME = "atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties";
    
    @Autowired(required = false)
    private AnnotationConfigValidator configValidator;
    
    @Autowired(required = false)
    private ConfigConflictDetector conflictDetector;
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        logger.info("Starting Atlas Log configuration merge process...");
        
        try {
            logger.debug("=== Atlas Log Configuration Processing Started ===");
            
            // 获取注解配置
            LogConfigProperties annotationConfig = getAnnotationConfig(beanFactory);
            logger.debug("Retrieved annotationConfig: {}", annotationConfig);
            if (annotationConfig != null && annotationConfig.getHttpLog() != null) {
                logger.debug("Annotation httpLog urlFormat: '{}'", annotationConfig.getHttpLog().getUrlFormat());
                logger.debug("Annotation httpLog includeQueryString: {}", annotationConfig.getHttpLog().isIncludeQueryString());
            } else {
                logger.debug("No annotation httpLog configuration found");
            }
            
            // 获取属性文件配置
            LogConfigProperties propertiesConfig = getPropertiesConfig(beanFactory);
            logger.debug("Retrieved propertiesConfig: {}", propertiesConfig);
            if (propertiesConfig != null && propertiesConfig.getHttpLog() != null) {
                logger.debug("Properties httpLog urlFormat: '{}'", propertiesConfig.getHttpLog().getUrlFormat());
                logger.debug("Properties httpLog includeQueryString: {}", propertiesConfig.getHttpLog().isIncludeQueryString());
            } else {
                logger.debug("No properties httpLog configuration found");
            }
            
            // 检测配置冲突
            if (annotationConfig != null && conflictDetector != null) {
                conflictDetector.detectConflicts(annotationConfig, propertiesConfig);
            }
            
            // 合并配置（注解优先）
            LogConfigProperties mergedConfig = mergeConfigs(annotationConfig, propertiesConfig);
            logger.debug("Merged configuration: {}", mergedConfig);
            if (mergedConfig != null && mergedConfig.getHttpLog() != null) {
                logger.debug("Final merged httpLog urlFormat: '{}'", mergedConfig.getHttpLog().getUrlFormat());
            }
            
            // 验证最终配置
            if (mergedConfig != null && configValidator != null) {
                configValidator.validate(mergedConfig);
            }
            
            // 注册最终配置（如果存在注解配置的话）
            if (annotationConfig != null) {
                registerMergedConfig(beanFactory, mergedConfig);
                logger.info("Configuration merge completed successfully");
                logger.debug("=== Atlas Log Configuration Processing Completed ===");
            } else {
                logger.debug("No annotation configuration found, skipping merge process");
            }
            
        } catch (Exception e) {
            logger.error("Failed to merge Atlas Log configurations", e);
            throw new IllegalStateException("Failed to merge Atlas Log configurations", e);
        }
    }
    
    /**
     * 获取注解配置
     */
    private LogConfigProperties getAnnotationConfig(ConfigurableListableBeanFactory beanFactory) {
        try {
            if (beanFactory.containsBean(ANNOTATION_CONFIG_PROCESSOR_BEAN_NAME)) {
                AtlasLogAnnotationConfigProcessor processor = beanFactory.getBean(
                    ANNOTATION_CONFIG_PROCESSOR_BEAN_NAME, AtlasLogAnnotationConfigProcessor.class);
                LogConfigProperties config = processor.getAnnotationConfig();
                logger.debug("Retrieved annotation configuration: {}", config);
                return config;
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve annotation configuration: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取属性文件配置
     */
    private LogConfigProperties getPropertiesConfig(ConfigurableListableBeanFactory beanFactory) {
        try {
            if (beanFactory.containsBean(LOG_CONFIG_PROPERTIES_BEAN_NAME)) {
                LogConfigProperties config = beanFactory.getBean(
                    LOG_CONFIG_PROPERTIES_BEAN_NAME, LogConfigProperties.class);
                logger.debug("Retrieved properties configuration: {}", config);
                return config;
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve properties configuration: {}", e.getMessage());
        }
        return new LogConfigProperties(); // 返回默认配置
    }
    
    /**
     * 合并配置（注解优先）
     */
    private LogConfigProperties mergeConfigs(LogConfigProperties annotationConfig, 
                                           LogConfigProperties propertiesConfig) {
        
        if (annotationConfig == null) {
            return propertiesConfig;
        }
        
        if (propertiesConfig == null) {
            return annotationConfig;
        }
        
        logger.info("Merging annotation configuration with properties configuration...");
        
        LogConfigProperties merged = new LogConfigProperties();
        
        // 基础配置合并
        mergeBasicConfig(merged, annotationConfig, propertiesConfig);
        
        // 列表配置合并
        mergeListConfig(merged, annotationConfig, propertiesConfig);
        
        // 嵌套配置合并
        mergeNestedConfigs(merged, annotationConfig, propertiesConfig);
        
        logger.debug("Configuration merge result: {}", merged);
        return merged;
    }
    
    /**
     * 合并基础配置
     */
    private void mergeBasicConfig(LogConfigProperties merged, 
                                 LogConfigProperties annotationConfig, 
                                 LogConfigProperties propertiesConfig) {
        
        merged.setEnabled(resolveValue(annotationConfig.isEnabled(), 
                                     propertiesConfig.isEnabled(), true, "enabled"));
        merged.setDefaultLevel(resolveValue(annotationConfig.getDefaultLevel(), 
                                          propertiesConfig.getDefaultLevel(), "INFO", "defaultLevel"));
        merged.setDateFormat(resolveValue(annotationConfig.getDateFormat(), 
                                        propertiesConfig.getDateFormat(), "yyyy-MM-dd HH:mm:ss.SSS", "dateFormat"));
        merged.setPrettyPrint(resolveValue(annotationConfig.isPrettyPrint(), 
                                         propertiesConfig.isPrettyPrint(), false, "prettyPrint"));
        merged.setMaxMessageLength(resolveValue(annotationConfig.getMaxMessageLength(), 
                                              propertiesConfig.getMaxMessageLength(), 2000, "maxMessageLength"));
        merged.setSpelEnabled(resolveValue(annotationConfig.isSpelEnabled(), 
                                         propertiesConfig.isSpelEnabled(), true, "spelEnabled"));
        merged.setConditionEnabled(resolveValue(annotationConfig.isConditionEnabled(), 
                                               propertiesConfig.isConditionEnabled(), true, "conditionEnabled"));
    }
    
    /**
     * 合并列表配置
     */
    private void mergeListConfig(LogConfigProperties merged, 
                               LogConfigProperties annotationConfig, 
                               LogConfigProperties propertiesConfig) {
        
        merged.setEnabledTags(resolveListValue(annotationConfig.getEnabledTags(), 
                                             propertiesConfig.getEnabledTags(), "enabledTags"));
        merged.setEnabledGroups(resolveListValue(annotationConfig.getEnabledGroups(), 
                                               propertiesConfig.getEnabledGroups(), "enabledGroups"));
        merged.setExclusions(resolveListValue(annotationConfig.getExclusions(), 
                                            propertiesConfig.getExclusions(), "exclusions"));
    }
    
    /**
     * 合并嵌套配置
     */
    private void mergeNestedConfigs(LogConfigProperties merged, 
                                  LogConfigProperties annotationConfig, 
                                  LogConfigProperties propertiesConfig) {
        
        mergeTraceConfig(merged, annotationConfig, propertiesConfig);
        mergePerformanceConfig(merged, annotationConfig, propertiesConfig);
        mergeConditionConfig(merged, annotationConfig, propertiesConfig);
        mergeSensitiveConfig(merged, annotationConfig, propertiesConfig);
        mergeHttpLogConfig(merged, annotationConfig, propertiesConfig);
    }
    
    /**
     * 合并链路追踪配置
     */
    private void mergeTraceConfig(LogConfigProperties merged, 
                                LogConfigProperties annotationConfig, 
                                LogConfigProperties propertiesConfig) {
        
        LogConfigProperties.TraceIdConfig mergedTrace = merged.getTraceId();
        LogConfigProperties.TraceIdConfig annotationTrace = annotationConfig.getTraceId();
        LogConfigProperties.TraceIdConfig propertiesTrace = propertiesConfig.getTraceId();
        
        mergedTrace.setEnabled(resolveValue(annotationTrace.isEnabled(), 
                                          propertiesTrace.isEnabled(), true, "trace.enabled"));
        mergedTrace.setHeaderName(resolveValue(annotationTrace.getHeaderName(), 
                                             propertiesTrace.getHeaderName(), "X-Trace-Id", "trace.headerName"));
        mergedTrace.setGenerator(resolveValue(annotationTrace.getGenerator(), 
                                            propertiesTrace.getGenerator(), "uuid", "trace.generator"));
    }
    
    /**
     * 合并性能监控配置
     */
    private void mergePerformanceConfig(LogConfigProperties merged, 
                                      LogConfigProperties annotationConfig, 
                                      LogConfigProperties propertiesConfig) {
        
        LogConfigProperties.PerformanceConfig mergedPerf = merged.getPerformance();
        LogConfigProperties.PerformanceConfig annotationPerf = annotationConfig.getPerformance();
        LogConfigProperties.PerformanceConfig propertiesPerf = propertiesConfig.getPerformance();
        
        mergedPerf.setEnabled(resolveValue(annotationPerf.isEnabled(), 
                                         propertiesPerf.isEnabled(), true, "performance.enabled"));
        mergedPerf.setSlowThreshold(resolveValue(annotationPerf.getSlowThreshold(), 
                                               propertiesPerf.getSlowThreshold(), 1000L, "performance.slowThreshold"));
        mergedPerf.setLogSlowMethods(resolveValue(annotationPerf.isLogSlowMethods(), 
                                                propertiesPerf.isLogSlowMethods(), true, "performance.logSlowMethods"));
    }
    
    /**
     * 合并条件评估配置
     */
    private void mergeConditionConfig(LogConfigProperties merged, 
                                    LogConfigProperties annotationConfig, 
                                    LogConfigProperties propertiesConfig) {
        
        LogConfigProperties.ConditionConfig mergedCondition = merged.getCondition();
        LogConfigProperties.ConditionConfig annotationCondition = annotationConfig.getCondition();
        LogConfigProperties.ConditionConfig propertiesCondition = propertiesConfig.getCondition();
        
        mergedCondition.setCacheEnabled(resolveValue(annotationCondition.isCacheEnabled(), 
                                                   propertiesCondition.isCacheEnabled(), true, "condition.cacheEnabled"));
        mergedCondition.setTimeoutMs(resolveValue(annotationCondition.getTimeoutMs(), 
                                                propertiesCondition.getTimeoutMs(), 1000L, "condition.timeoutMs"));
        mergedCondition.setFailSafe(resolveValue(annotationCondition.isFailSafe(), 
                                                propertiesCondition.isFailSafe(), true, "condition.failSafe"));
    }
    
    /**
     * 合并敏感数据配置
     */
    private void mergeSensitiveConfig(LogConfigProperties merged, 
                                    LogConfigProperties annotationConfig, 
                                    LogConfigProperties propertiesConfig) {
        
        LogConfigProperties.SensitiveConfig mergedSensitive = merged.getSensitive();
        LogConfigProperties.SensitiveConfig annotationSensitive = annotationConfig.getSensitive();
        LogConfigProperties.SensitiveConfig propertiesSensitive = propertiesConfig.getSensitive();
        
        mergedSensitive.setEnabled(resolveValue(annotationSensitive.isEnabled(), 
                                              propertiesSensitive.isEnabled(), true, "sensitive.enabled"));
        mergedSensitive.setMaskValue(resolveValue(annotationSensitive.getMaskValue(), 
                                                propertiesSensitive.getMaskValue(), "***", "sensitive.maskValue"));
        mergedSensitive.setCustomFields(resolveListValue(annotationSensitive.getCustomFields(), 
                                                        propertiesSensitive.getCustomFields(), "sensitive.customFields"));
    }
    
    /**
     * 合并HTTP日志配置
     */
    private void mergeHttpLogConfig(LogConfigProperties merged, 
                                  LogConfigProperties annotationConfig, 
                                  LogConfigProperties propertiesConfig) {
        
        LogConfigProperties.HttpLogConfig mergedHttpLog = merged.getHttpLog();
        LogConfigProperties.HttpLogConfig annotationHttpLog = annotationConfig != null ? annotationConfig.getHttpLog() : null;
        LogConfigProperties.HttpLogConfig propertiesHttpLog = propertiesConfig != null ? propertiesConfig.getHttpLog() : null;
        
        if (annotationHttpLog != null || propertiesHttpLog != null) {
            // 处理 logFullParameters
            Boolean annotationLogFullParams = annotationHttpLog != null ? Boolean.valueOf(annotationHttpLog.isLogFullParameters()) : null;
            Boolean propertiesLogFullParams = propertiesHttpLog != null ? Boolean.valueOf(propertiesHttpLog.isLogFullParameters()) : null;
            mergedHttpLog.setLogFullParameters(resolveValue(annotationLogFullParams, propertiesLogFullParams, Boolean.TRUE, "httpLog.logFullParameters"));
            
            // 处理 urlFormat
            mergedHttpLog.setUrlFormat(resolveValue(
                annotationHttpLog != null ? annotationHttpLog.getUrlFormat() : null,
                propertiesHttpLog != null ? propertiesHttpLog.getUrlFormat() : null,
                "{uri}{queryString}", "httpLog.urlFormat"));
            
            // 处理 includeQueryString
            Boolean annotationIncludeQuery = annotationHttpLog != null ? Boolean.valueOf(annotationHttpLog.isIncludeQueryString()) : null;
            Boolean propertiesIncludeQuery = propertiesHttpLog != null ? Boolean.valueOf(propertiesHttpLog.isIncludeQueryString()) : null;
            mergedHttpLog.setIncludeQueryString(resolveValue(annotationIncludeQuery, propertiesIncludeQuery, Boolean.TRUE, "httpLog.includeQueryString"));
            
            // 处理 includeHeaders
            Boolean annotationIncludeHeaders = annotationHttpLog != null ? Boolean.valueOf(annotationHttpLog.isIncludeHeaders()) : null;
            Boolean propertiesIncludeHeaders = propertiesHttpLog != null ? Boolean.valueOf(propertiesHttpLog.isIncludeHeaders()) : null;
            mergedHttpLog.setIncludeHeaders(resolveValue(annotationIncludeHeaders, propertiesIncludeHeaders, Boolean.FALSE, "httpLog.includeHeaders"));
            
            // 处理 excludeHeaders
            mergedHttpLog.setExcludeHeaders(resolveListValue(
                annotationHttpLog != null ? annotationHttpLog.getExcludeHeaders() : null,
                propertiesHttpLog != null ? propertiesHttpLog.getExcludeHeaders() : null,
                "httpLog.excludeHeaders"));
        }
    }
    
    /**
     * 解析配置值（配置文件优先）
     */
    private <T> T resolveValue(T annotationValue, T propertiesValue, T defaultValue, String configName) {
        T result;
        String source;
        
        // 配置文件优先：如果配置文件有配置，使用配置文件的值
        if (propertiesValue != null && !isDefaultValue(propertiesValue)) {
            result = propertiesValue;
            source = "properties";
        } else if (annotationValue != null && !isDefaultValue(annotationValue)) {
            // 如果配置文件没有配置，使用注解的值
            result = annotationValue;
            source = "annotation";
        } else {
            // 都没有配置，使用默认值
            result = defaultValue;
            source = "default";
        }
        
        logger.debug("Resolved config '{}' = {} (source: {})", configName, result, source);
        return result;
    }
    
    /**
     * 解析列表配置值
     */
    private List<String> resolveListValue(List<String> annotationValue, List<String> propertiesValue, String configName) {
        List<String> result;
        String source;
        
        if (annotationValue != null && !annotationValue.isEmpty()) {
            result = new ArrayList<>(annotationValue);
            source = "annotation";
        } else if (propertiesValue != null && !propertiesValue.isEmpty()) {
            result = new ArrayList<>(propertiesValue);
            source = "properties";
        } else {
            result = new ArrayList<>();
            source = "default";
        }
        
        logger.debug("Resolved list config '{}' = {} (source: {})", configName, result, source);
        return result;
    }
    
    /**
     * 判断是否为默认值
     */
    private boolean isDefaultValue(Object value) {
        if (value == null) {
            return true;
        }
        
        // 检查字符串默认值
        if (value instanceof String) {
            String strValue = (String) value;
            // ✅ 检查 HTTP 日志的系统默认值
            if ("{uri}{queryString}".equals(strValue)) {
                return true; // 这是系统默认的 URL 格式
            }
            // ✅ 检查其他系统默认值
            if ("yyyy-MM-dd HH:mm:ss.SSS".equals(strValue)) {
                return true; // 默认日期格式
            }
            if ("INFO".equals(strValue)) {
                return true; // 默认日志级别
            }
            if ("X-Trace-Id".equals(strValue)) {
                return true; // 默认 TraceId 头名称
            }
            if ("uuid".equals(strValue)) {
                return true; // 默认生成器类型
            }
            if ("***".equals(strValue)) {
                return true; // 默认脱敏值
            }
            return strValue.isEmpty() || "default".equals(strValue);
        }
        
        // 检查布尔值默认值（根据具体配置项判断）
        if (value instanceof Boolean) {
            // 对于布尔值，我们不能简单判断，因为 true/false 都可能是有意义的配置
            return false;
        }
        
        // 检查数值默认值
        if (value instanceof Number) {
            long longValue = ((Number) value).longValue();
            // ✅ 检查常见的系统默认数值
            if (longValue == 1000L) {
                return true; // 默认慢方法阈值
            }
            if (longValue == 2000L) {
                return true; // 默认最大消息长度
            }
            if (longValue == 32L) {
                return true; // 默认 TraceId 长度
            }
            return longValue == 0L;
        }
        
        // 检查集合默认值
        if (value instanceof java.util.Collection) {
            return ((java.util.Collection<?>) value).isEmpty();
        }
        
        return false;
    }
    
    /**
     * 注册合并后的配置
     */
    private void registerMergedConfig(ConfigurableListableBeanFactory beanFactory, 
                                    LogConfigProperties mergedConfig) {
        
        // 将合并后的配置注册为单例 Bean
        beanFactory.registerSingleton("atlasLogMergedConfig", mergedConfig);
        logger.info("Registered merged configuration as singleton bean: atlasLogMergedConfig");
    }
}