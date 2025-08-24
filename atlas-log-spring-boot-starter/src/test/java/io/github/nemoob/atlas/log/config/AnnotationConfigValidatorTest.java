package io.github.nemoob.atlas.log.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnnotationConfigValidator 单元测试
 */
class AnnotationConfigValidatorTest {
    
    private AnnotationConfigValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new AnnotationConfigValidator();
    }
    
    @Test
    void testValidateValidConfig() {
        // 准备有效配置
        LogConfigProperties config = createValidConfig();
        
        // 验证应该成功
        assertDoesNotThrow(() -> validator.validate(config));
    }
    
    @Test
    void testValidateNullConfig() {
        // 验证null配置应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
    }
    
    @Test
    void testValidateInvalidLogLevel() {
        LogConfigProperties config = createValidConfig();
        config.setDefaultLevel("INVALID");
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateNullLogLevel() {
        LogConfigProperties config = createValidConfig();
        config.setDefaultLevel(null);
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateEmptyLogLevel() {
        LogConfigProperties config = createValidConfig();
        config.setDefaultLevel("");
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateValidLogLevels() {
        LogConfigProperties config = createValidConfig();
        
        // 测试所有有效的日志级别
        String[] validLevels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF"};
        for (String level : validLevels) {
            config.setDefaultLevel(level);
            assertDoesNotThrow(() -> validator.validate(config), "Level " + level + " should be valid");
            
            // 测试小写版本
            config.setDefaultLevel(level.toLowerCase());
            assertDoesNotThrow(() -> validator.validate(config), "Level " + level.toLowerCase() + " should be valid");
        }
    }
    
    @Test
    void testValidateInvalidDateFormat() {
        LogConfigProperties config = createValidConfig();
        config.setDateFormat("invalid-format");
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateNullDateFormat() {
        LogConfigProperties config = createValidConfig();
        config.setDateFormat(null);
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateInvalidMaxMessageLength() {
        LogConfigProperties config = createValidConfig();
        config.setMaxMessageLength(0);
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        config.setMaxMessageLength(-1);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateEmptyTagsInList() {
        LogConfigProperties config = createValidConfig();
        config.setEnabledTags(Arrays.asList("api", "", "business"));
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateNullTagsInList() {
        LogConfigProperties config = createValidConfig();
        config.setEnabledTags(Arrays.asList("api", null, "business"));
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateInvalidMethodPattern() {
        LogConfigProperties config = createValidConfig();
        config.setExclusions(Arrays.asList("invalid-pattern"));
        
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }
    
    @Test
    void testValidateValidMethodPatterns() {
        LogConfigProperties config = createValidConfig();
        config.setExclusions(Arrays.asList("ClassName.methodName", "*.toString", "Package.Class.method"));
        
        assertDoesNotThrow(() -> validator.validate(config));
    }
    
    @Test
    void testValidateTraceConfig() {
        LogConfigProperties config = createValidConfig();
        
        // 测试null配置
        config.setTraceId(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试无效的header名称
        config.setTraceId(new LogConfigProperties.TraceIdConfig());
        config.getTraceId().setHeaderName(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        config.getTraceId().setHeaderName("");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试无效的生成器
        config.getTraceId().setHeaderName("X-Trace-Id");
        config.getTraceId().setGenerator("invalid");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试有效配置
        config.getTraceId().setGenerator("uuid");
        assertDoesNotThrow(() -> validator.validate(config));
        
        config.getTraceId().setGenerator("snowflake");
        assertDoesNotThrow(() -> validator.validate(config));
    }
    
    @Test
    void testValidatePerformanceConfig() {
        LogConfigProperties config = createValidConfig();
        
        // 测试null配置
        config.setPerformance(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试负数阈值
        config.setPerformance(new LogConfigProperties.PerformanceConfig());
        config.getPerformance().setSlowThreshold(-1);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试有效配置
        config.getPerformance().setSlowThreshold(1000);
        assertDoesNotThrow(() -> validator.validate(config));
    }
    
    @Test
    void testValidateConditionConfig() {
        LogConfigProperties config = createValidConfig();
        
        // 测试null配置
        config.setCondition(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试无效超时时间
        config.setCondition(new LogConfigProperties.ConditionConfig());
        config.getCondition().setTimeoutMs(0);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        config.getCondition().setTimeoutMs(-1);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试有效配置
        config.getCondition().setTimeoutMs(1000);
        assertDoesNotThrow(() -> validator.validate(config));
    }
    
    @Test
    void testValidateSensitiveConfig() {
        LogConfigProperties config = createValidConfig();
        
        // 测试null配置
        config.setSensitive(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试无效mask值
        config.setSensitive(new LogConfigProperties.SensitiveConfig());
        config.getSensitive().setMaskValue(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        config.getSensitive().setMaskValue("");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        
        // 测试有效配置
        config.getSensitive().setMaskValue("***");
        config.getSensitive().setCustomFields(Arrays.asList("phone", "email"));
        assertDoesNotThrow(() -> validator.validate(config));
    }
    
    private LogConfigProperties createValidConfig() {
        LogConfigProperties config = new LogConfigProperties();
        config.setEnabled(true);
        config.setDefaultLevel("INFO");
        config.setDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        config.setPrettyPrint(false);
        config.setMaxMessageLength(2000);
        config.setSpelEnabled(true);
        config.setConditionEnabled(true);
        config.setEnabledTags(Arrays.asList("api", "business"));
        config.setEnabledGroups(Arrays.asList("default"));
        config.setExclusions(Arrays.asList("*.toString"));
        
        // 设置有效的嵌套配置
        LogConfigProperties.TraceIdConfig traceConfig = config.getTraceId();
        traceConfig.setEnabled(true);
        traceConfig.setHeaderName("X-Trace-Id");
        traceConfig.setGenerator("uuid");
        
        LogConfigProperties.PerformanceConfig perfConfig = config.getPerformance();
        perfConfig.setEnabled(true);
        perfConfig.setSlowThreshold(1000);
        perfConfig.setLogSlowMethods(true);
        
        LogConfigProperties.ConditionConfig conditionConfig = config.getCondition();
        conditionConfig.setCacheEnabled(true);
        conditionConfig.setTimeoutMs(1000);
        conditionConfig.setFailSafe(true);
        
        LogConfigProperties.SensitiveConfig sensitiveConfig = config.getSensitive();
        sensitiveConfig.setEnabled(true);
        sensitiveConfig.setMaskValue("***");
        sensitiveConfig.setCustomFields(Arrays.asList("phone", "email"));
        
        return config;
    }
}