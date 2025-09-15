package io.github.nemoob.atlas.log.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Atlas Log 配置冲突检测器
 * <p>
 * 负责检测注解配置和属性文件配置之间的冲突，
 * 并记录冲突信息以帮助开发者了解配置覆盖情况。
 * </p>
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Component
public class ConfigConflictDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigConflictDetector.class);
    
    /**
     * 检测配置冲突
     */
    public void detectConflicts(LogConfigProperties annotationConfig, 
                               LogConfigProperties propertiesConfig) {
        
        if (annotationConfig == null || propertiesConfig == null) {
            logger.debug("Skipping conflict detection: one of the configurations is null");
            return;
        }
        
        logger.debug("Starting configuration conflict detection...");
        
        List<String> conflicts = new ArrayList<>();
        
        // 检测基础配置冲突
        detectBasicConfigConflicts(annotationConfig, propertiesConfig, conflicts);
        
        // 检测列表配置冲突
        detectListConfigConflicts(annotationConfig, propertiesConfig, conflicts);
        
        // 检测嵌套配置冲突
        detectNestedConfigConflicts(annotationConfig, propertiesConfig, conflicts);
        
        // 记录冲突信息
        if (!conflicts.isEmpty()) {
            logConflicts(conflicts);
        } else {
            logger.debug("No configuration conflicts detected");
        }
    }
    
    /**
     * 检测基础配置冲突
     */
    private void detectBasicConfigConflicts(LogConfigProperties annotationConfig, 
                                           LogConfigProperties propertiesConfig, 
                                           List<String> conflicts) {
        
        detectConflict("enabled", annotationConfig.isEnabled(), propertiesConfig.isEnabled(), conflicts);
        detectConflict("defaultLevel", annotationConfig.getDefaultLevel(), propertiesConfig.getDefaultLevel(), conflicts);
        detectConflict("dateFormat", annotationConfig.getDateFormat(), propertiesConfig.getDateFormat(), conflicts);
        detectConflict("prettyPrint", annotationConfig.isPrettyPrint(), propertiesConfig.isPrettyPrint(), conflicts);
        detectConflict("maxMessageLength", annotationConfig.getMaxMessageLength(), propertiesConfig.getMaxMessageLength(), conflicts);
        detectConflict("spelEnabled", annotationConfig.isSpelEnabled(), propertiesConfig.isSpelEnabled(), conflicts);
        detectConflict("conditionEnabled", annotationConfig.isConditionEnabled(), propertiesConfig.isConditionEnabled(), conflicts);
    }
    
    /**
     * 检测列表配置冲突
     */
    private void detectListConfigConflicts(LogConfigProperties annotationConfig, 
                                          LogConfigProperties propertiesConfig, 
                                          List<String> conflicts) {
        
        detectListConflict("enabledTags", annotationConfig.getEnabledTags(), propertiesConfig.getEnabledTags(), conflicts);
        detectListConflict("enabledGroups", annotationConfig.getEnabledGroups(), propertiesConfig.getEnabledGroups(), conflicts);
        detectListConflict("exclusions", annotationConfig.getExclusions(), propertiesConfig.getExclusions(), conflicts);
    }
    
    /**
     * 检测嵌套配置冲突
     */
    private void detectNestedConfigConflicts(LogConfigProperties annotationConfig, 
                                            LogConfigProperties propertiesConfig, 
                                            List<String> conflicts) {
        
        detectTraceConfigConflicts(annotationConfig.getTraceId(), propertiesConfig.getTraceId(), conflicts);
        detectPerformanceConfigConflicts(annotationConfig.getPerformance(), propertiesConfig.getPerformance(), conflicts);
        detectConditionConfigConflicts(annotationConfig.getCondition(), propertiesConfig.getCondition(), conflicts);
        detectSensitiveConfigConflicts(annotationConfig.getSensitive(), propertiesConfig.getSensitive(), conflicts);
    }
    
    /**
     * 检测链路追踪配置冲突
     */
    private void detectTraceConfigConflicts(LogConfigProperties.TraceIdConfig annotationConfig, 
                                           LogConfigProperties.TraceIdConfig propertiesConfig, 
                                           List<String> conflicts) {
        
        detectConflict("trace.enabled", annotationConfig.isEnabled(), propertiesConfig.isEnabled(), conflicts);
        detectConflict("trace.headerName", annotationConfig.getHeaderName(), propertiesConfig.getHeaderName(), conflicts);
        detectConflict("trace.generator", annotationConfig.getGenerator(), propertiesConfig.getGenerator(), conflicts);
    }
    
    /**
     * 检测性能监控配置冲突
     */
    private void detectPerformanceConfigConflicts(LogConfigProperties.PerformanceConfig annotationConfig, 
                                                 LogConfigProperties.PerformanceConfig propertiesConfig, 
                                                 List<String> conflicts) {
        
        detectConflict("performance.enabled", annotationConfig.isEnabled(), propertiesConfig.isEnabled(), conflicts);
        detectConflict("performance.slowThreshold", annotationConfig.getSlowThreshold(), propertiesConfig.getSlowThreshold(), conflicts);
        detectConflict("performance.logSlowMethods", annotationConfig.isLogSlowMethods(), propertiesConfig.isLogSlowMethods(), conflicts);
    }
    
    /**
     * 检测条件评估配置冲突
     */
    private void detectConditionConfigConflicts(LogConfigProperties.ConditionConfig annotationConfig, 
                                               LogConfigProperties.ConditionConfig propertiesConfig, 
                                               List<String> conflicts) {
        
        detectConflict("condition.cacheEnabled", annotationConfig.isCacheEnabled(), propertiesConfig.isCacheEnabled(), conflicts);
        detectConflict("condition.timeoutMs", annotationConfig.getTimeoutMs(), propertiesConfig.getTimeoutMs(), conflicts);
        detectConflict("condition.failSafe", annotationConfig.isFailSafe(), propertiesConfig.isFailSafe(), conflicts);
    }
    
    /**
     * 检测敏感数据配置冲突
     */
    private void detectSensitiveConfigConflicts(LogConfigProperties.SensitiveConfig annotationConfig, 
                                               LogConfigProperties.SensitiveConfig propertiesConfig, 
                                               List<String> conflicts) {
        
        detectConflict("sensitive.enabled", annotationConfig.isEnabled(), propertiesConfig.isEnabled(), conflicts);
        detectConflict("sensitive.maskValue", annotationConfig.getMaskValue(), propertiesConfig.getMaskValue(), conflicts);
        detectListConflict("sensitive.customFields", annotationConfig.getCustomFields(), propertiesConfig.getCustomFields(), conflicts);
    }
    
    /**
     * 检测单个配置项冲突
     */
    private <T> void detectConflict(String configName, T annotationValue, T propertiesValue, List<String> conflicts) {
        if (annotationValue != null && propertiesValue != null) {
            if (!Objects.equals(annotationValue, propertiesValue)) {
                conflicts.add(String.format("%s: annotation=%s, properties=%s", 
                    configName, annotationValue, propertiesValue));
            }
        }
    }
    
    /**
     * 检测列表配置项冲突
     */
    private void detectListConflict(String configName, List<String> annotationValue, 
                                   List<String> propertiesValue, List<String> conflicts) {
        if (annotationValue != null && propertiesValue != null) {
            if (!annotationValue.isEmpty() && !propertiesValue.isEmpty()) {
                if (!Objects.equals(annotationValue, propertiesValue)) {
                    conflicts.add(String.format("%s: annotation=%s, properties=%s", 
                        configName, annotationValue, propertiesValue));
                }
            }
        }
    }
    
    /**
     * 记录冲突信息
     */
    private void logConflicts(List<String> conflicts) {
        logger.warn("\n" +
                "===============================================\n" +
                "  Atlas Log Configuration Conflicts Detected  \n" +
                "===============================================\n" +
                "Annotation configuration will override properties configuration:\n");
        
        for (String conflict : conflicts) {
            logger.warn("  > {}", conflict);
        }
        
        logger.warn("\n" +
                "Priority: Annotation > Properties > Environment > Default\n" +
                "================================================\n");
    }
}