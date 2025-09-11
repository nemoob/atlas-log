package io.github.nemoob.atlas.log.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * AtlasLogAnnotationConfigProcessor 单元测试
 */
class AtlasLogAnnotationConfigProcessorTest {
    
    private AtlasLogAnnotationConfigProcessor processor;
    private ApplicationContext applicationContext;
    
    @BeforeEach
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
    }
    
    @Test
    void testProcessBasicConfig() {
        // 准备测试数据
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("enabled", true);
        attributes.put("defaultLevel", "DEBUG");
        attributes.put("dateFormat", "yyyy-MM-dd HH:mm:ss");
        attributes.put("prettyPrint", true);
        attributes.put("maxMessageLength", 5000);
        attributes.put("spelEnabled", true);
        attributes.put("conditionEnabled", false);
        
        // 创建处理器
        processor = new AtlasLogAnnotationConfigProcessor(attributes);
        processor.setApplicationContext(applicationContext);
        
        // 执行处理
        processor.processAnnotationConfig();
        
        // 验证结果
        LogConfigProperties config = processor.getAnnotationConfig();
        assertNotNull(config);
        assertTrue(config.isEnabled());
        assertEquals("DEBUG", config.getDefaultLevel());
        assertEquals("yyyy-MM-dd HH:mm:ss", config.getDateFormat());
        assertTrue(config.isPrettyPrint());
        assertEquals(5000, config.getMaxMessageLength());
        assertTrue(config.isSpelEnabled());
        assertFalse(config.isConditionEnabled());
    }
    
    @Test
    void testProcessTagsAndGroupsConfig() {
        // 准备测试数据
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("enabledTags", new String[]{"api", "business", "security"});
        attributes.put("enabledGroups", new String[]{"default", "urgent"});
        attributes.put("exclusions", new String[]{"*.toString", "*.hashCode"});
        
        // 创建处理器
        processor = new AtlasLogAnnotationConfigProcessor(attributes);
        processor.setApplicationContext(applicationContext);
        
        // 执行处理
        processor.processAnnotationConfig();
        
        // 验证结果
        LogConfigProperties config = processor.getAnnotationConfig();
        assertNotNull(config);
        assertEquals(3, config.getEnabledTags().size());
        assertTrue(config.getEnabledTags().contains("api"));
        assertTrue(config.getEnabledTags().contains("business"));
        assertTrue(config.getEnabledTags().contains("security"));
        
        assertEquals(2, config.getEnabledGroups().size());
        assertTrue(config.getEnabledGroups().contains("default"));
        assertTrue(config.getEnabledGroups().contains("urgent"));
        
        assertEquals(2, config.getExclusions().size());
        assertTrue(config.getExclusions().contains("*.toString"));
        assertTrue(config.getExclusions().contains("*.hashCode"));
    }
    
    @Test
    void testProcessNestedConfigs() {
        // 准备嵌套配置数据
        Map<String, Object> traceAttrs = new HashMap<>();
        traceAttrs.put("enabled", true);
        traceAttrs.put("headerName", "Custom-Trace-Id");
        traceAttrs.put("generator", "snowflake");
        
        Map<String, Object> performanceAttrs = new HashMap<>();
        performanceAttrs.put("enabled", false);
        performanceAttrs.put("slowThreshold", 2000L);
        performanceAttrs.put("logSlowMethods", false);
        
        Map<String, Object> conditionAttrs = new HashMap<>();
        conditionAttrs.put("cacheEnabled", false);
        conditionAttrs.put("timeoutMs", 500L);
        conditionAttrs.put("failSafe", false);
        
        Map<String, Object> sensitiveAttrs = new HashMap<>();
        sensitiveAttrs.put("enabled", true);
        sensitiveAttrs.put("maskValue", "XXX");
        sensitiveAttrs.put("customFields", new String[]{"phone", "email"});
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("trace", traceAttrs);
        attributes.put("performance", performanceAttrs);
        attributes.put("condition", conditionAttrs);
        attributes.put("sensitive", sensitiveAttrs);
        
        // 创建处理器
        processor = new AtlasLogAnnotationConfigProcessor(attributes);
        processor.setApplicationContext(applicationContext);
        
        // 执行处理
        processor.processAnnotationConfig();
        
        // 验证结果
        LogConfigProperties config = processor.getAnnotationConfig();
        assertNotNull(config);
        
        // 验证链路追踪配置
        LogConfigProperties.TraceIdConfig traceConfig = config.getTraceId();
        assertTrue(traceConfig.isEnabled());
        assertEquals("Custom-Trace-Id", traceConfig.getHeaderName());
        assertEquals("snowflake", traceConfig.getGenerator());
        
        // 验证性能监控配置
        LogConfigProperties.PerformanceConfig perfConfig = config.getPerformance();
        assertFalse(perfConfig.isEnabled());
        assertEquals(2000L, perfConfig.getSlowThreshold());
        assertFalse(perfConfig.isLogSlowMethods());
        
        // 验证条件评估配置
        LogConfigProperties.ConditionConfig conditionConfig = config.getCondition();
        assertFalse(conditionConfig.isCacheEnabled());
        assertEquals(500L, conditionConfig.getTimeoutMs());
        assertFalse(conditionConfig.isFailSafe());
        
        // 验证敏感数据配置
        LogConfigProperties.SensitiveConfig sensitiveConfig = config.getSensitive();
        assertTrue(sensitiveConfig.isEnabled());
        assertEquals("XXX", sensitiveConfig.getMaskValue());
        assertEquals(2, sensitiveConfig.getCustomFields().size());
        assertTrue(sensitiveConfig.getCustomFields().contains("phone"));
        assertTrue(sensitiveConfig.getCustomFields().contains("email"));
    }
    
    @Test
    void testProcessWithEmptyAttributes() {
        // 准备空属性
        Map<String, Object> attributes = new HashMap<>();
        
        // 创建处理器
        processor = new AtlasLogAnnotationConfigProcessor(attributes);
        processor.setApplicationContext(applicationContext);
        
        // 执行处理
        processor.processAnnotationConfig();
        
        // 验证结果（应该使用默认值）
        LogConfigProperties config = processor.getAnnotationConfig();
        assertNotNull(config);
        // 验证默认值是否正确设置
        assertNotNull(config.getTraceId());
        assertNotNull(config.getPerformance());
        assertNotNull(config.getCondition());
        assertNotNull(config.getSensitive());
    }
    
    @Test
    void testProcessWithNullValues() {
        // 准备含null值的属性
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("defaultLevel", null);
        attributes.put("dateFormat", null);
        attributes.put("enabledTags", null);
        
        // 创建处理器
        processor = new AtlasLogAnnotationConfigProcessor(attributes);
        processor.setApplicationContext(applicationContext);
        
        // 执行处理
        processor.processAnnotationConfig();
        
        // 验证结果（应该使用默认值）
        LogConfigProperties config = processor.getAnnotationConfig();
        assertNotNull(config);
        // 空值应该被默认值替代
        assertNotNull(config.getEnabledTags());
        assertNotNull(config.getEnabledGroups());
        assertNotNull(config.getExclusions());
    }
}