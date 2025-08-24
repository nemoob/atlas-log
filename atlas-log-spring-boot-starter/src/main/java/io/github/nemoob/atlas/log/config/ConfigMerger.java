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
 * @author Atlas Team
 * @since 1.0.0
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
            // 获取注解配置
            LogConfigProperties annotationConfig = getAnnotationConfig(beanFactory);
            
            // 获取属性文件配置
            LogConfigProperties propertiesConfig = getPropertiesConfig(beanFactory);
            
            // 检测配置冲突
            if (annotationConfig != null && conflictDetector != null) {
                conflictDetector.detectConflicts(annotationConfig, propertiesConfig);
            }
            
            // 合并配置（注解优先）
            LogConfigProperties mergedConfig = mergeConfigs(annotationConfig, propertiesConfig);
            
            // 验证最终配置
            if (mergedConfig != null && configValidator != null) {
                configValidator.validate(mergedConfig);
            }
            
            // 注册最终配置（如果存在注解配置的话）
            if (annotationConfig != null) {
                registerMergedConfig(beanFactory, mergedConfig);
                logger.info("Configuration merge completed successfully");
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
                AnnotationConfigProcessor processor = beanFactory.getBean(
                    ANNOTATION_CONFIG_PROCESSOR_BEAN_NAME, AnnotationConfigProcessor.class);
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
     * 解析配置值（注解优先）
     */
    private <T> T resolveValue(T annotationValue, T propertiesValue, T defaultValue, String configName) {
        T result;
        String source;
        
        if (annotationValue != null && !isDefaultValue(annotationValue)) {
            result = annotationValue;
            source = "annotation";
        } else if (propertiesValue != null && !isDefaultValue(propertiesValue)) {
            result = propertiesValue;
            source = "properties";
        } else {
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
        
        if (value instanceof String) {
            String str = (String) value;
            return str.isEmpty() || "INFO".equals(str) || "yyyy-MM-dd HH:mm:ss.SSS".equals(str) 
                   || "X-Trace-Id".equals(str) || "uuid".equals(str) || "***".equals(str);
        }
        
        if (value instanceof Number) {
            Number num = (Number) value;
            return num.longValue() == 1000L || num.intValue() == 2000;
        }
        
        if (value instanceof Boolean) {
            // 对于boolean值，不认为是默认值，因为true/false都是有意义的配置
            return false;
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