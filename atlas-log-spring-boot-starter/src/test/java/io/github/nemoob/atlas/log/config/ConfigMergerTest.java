package io.github.nemoob.atlas.log.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ConfigMerger 单元测试
 */
class ConfigMergerTest {
    
    @Mock
    private ConfigurableListableBeanFactory beanFactory;
    
    @Mock
    private AnnotationConfigProcessor annotationProcessor;
    
    @Mock
    private AnnotationConfigValidator configValidator;
    
    @Mock
    private ConfigConflictDetector conflictDetector;
    
    private ConfigMerger configMerger;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        configMerger = new ConfigMerger();
        
        // 注入模拟的依赖
        try {
            java.lang.reflect.Field validatorField = ConfigMerger.class.getDeclaredField("configValidator");
            validatorField.setAccessible(true);
            validatorField.set(configMerger, configValidator);
            
            java.lang.reflect.Field detectorField = ConfigMerger.class.getDeclaredField("conflictDetector");
            detectorField.setAccessible(true);
            detectorField.set(configMerger, conflictDetector);
        } catch (Exception e) {
            fail("Failed to inject dependencies: " + e.getMessage());
        }
    }
    
    @Test
    void testMergeConfigsWithAnnotationPriority() {
        // 准备注解配置
        LogConfigProperties annotationConfig = new LogConfigProperties();
        annotationConfig.setEnabled(true);
        annotationConfig.setDefaultLevel("DEBUG");
        annotationConfig.setDateFormat("yyyy-MM-dd");
        annotationConfig.setEnabledTags(Arrays.asList("api", "business"));
        
        // 准备属性配置
        LogConfigProperties propertiesConfig = new LogConfigProperties();
        propertiesConfig.setEnabled(false);
        propertiesConfig.setDefaultLevel("INFO");
        propertiesConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
        propertiesConfig.setEnabledTags(Arrays.asList("security", "audit"));
        
        // 模拟bean工厂行为
        when(beanFactory.containsBean("atlasLogAnnotationConfigProcessor")).thenReturn(true);
        when(beanFactory.getBean("atlasLogAnnotationConfigProcessor", AnnotationConfigProcessor.class))
            .thenReturn(annotationProcessor);
        when(annotationProcessor.getAnnotationConfig()).thenReturn(annotationConfig);
        
        when(beanFactory.containsBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties")).thenReturn(true);
        when(beanFactory.getBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties", LogConfigProperties.class))
            .thenReturn(propertiesConfig);
        
        // 执行合并
        configMerger.postProcessBeanFactory(beanFactory);
        
        // 验证冲突检测被调用
        verify(conflictDetector).detectConflicts(annotationConfig, propertiesConfig);
        
        // 验证配置验证被调用
        verify(configValidator).validate(any(LogConfigProperties.class));
        
        // 验证合并后的配置被注册
        verify(beanFactory).registerSingleton(eq("atlasLogMergedConfig"), any(LogConfigProperties.class));
    }
    
    @Test
    void testMergeConfigsWithNoAnnotationConfig() {
        // 准备属性配置
        LogConfigProperties propertiesConfig = new LogConfigProperties();
        propertiesConfig.setEnabled(true);
        propertiesConfig.setDefaultLevel("INFO");
        
        // 模拟bean工厂行为（没有注解配置）
        when(beanFactory.containsBean("atlasLogAnnotationConfigProcessor")).thenReturn(false);
        when(beanFactory.containsBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties")).thenReturn(true);
        when(beanFactory.getBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties", LogConfigProperties.class))
            .thenReturn(propertiesConfig);
        
        // 执行合并
        configMerger.postProcessBeanFactory(beanFactory);
        
        // 验证冲突检测不会被调用
        verify(conflictDetector, never()).detectConflicts(any(), any());
        
        // 验证配置验证会被调用（因为即使没有注解配置，也会验证合并后的配置）
        verify(configValidator).validate(any());
        
        // 验证合并后的配置不会被注册（因为没有注解配置）
        verify(beanFactory, never()).registerSingleton(eq("atlasLogMergedConfig"), any());
    }
    
    @Test
    void testMergeConfigsWithException() {
        // 模拟异常情况
        when(beanFactory.containsBean("atlasLogAnnotationConfigProcessor")).thenReturn(true);
        when(beanFactory.getBean("atlasLogAnnotationConfigProcessor", AnnotationConfigProcessor.class))
            .thenThrow(new RuntimeException("Test exception"));
        
        // 准备属性配置
        LogConfigProperties propertiesConfig = new LogConfigProperties();
        propertiesConfig.setEnabled(true);
        propertiesConfig.setDefaultLevel("INFO");
        
        // 模拟bean工厂行为（属性配置）
        when(beanFactory.containsBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties")).thenReturn(true);
        when(beanFactory.getBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties", LogConfigProperties.class))
            .thenReturn(propertiesConfig);
        
        // 执行合并（应该处理异常）
        assertDoesNotThrow(() -> configMerger.postProcessBeanFactory(beanFactory));
        
        // 验证异常处理后，冲突检测不会被执行
        verify(conflictDetector, never()).detectConflicts(any(), any());
        
        // 验证配置验证会被执行（因为即使注解配置获取失败，也会验证属性配置）
        verify(configValidator).validate(any());
    }
    
    @Test
    void testMergeNestedConfigs() {
        // 准备注解配置
        LogConfigProperties annotationConfig = new LogConfigProperties();
        LogConfigProperties.TraceIdConfig annotationTrace = annotationConfig.getTraceId();
        annotationTrace.setEnabled(true);
        annotationTrace.setHeaderName("Custom-Trace");
        annotationTrace.setGenerator("uuid");
        
        LogConfigProperties.PerformanceConfig annotationPerf = annotationConfig.getPerformance();
        annotationPerf.setEnabled(false);
        annotationPerf.setSlowThreshold(2000L);
        
        // 准备属性配置
        LogConfigProperties propertiesConfig = new LogConfigProperties();
        LogConfigProperties.TraceIdConfig propertiesTrace = propertiesConfig.getTraceId();
        propertiesTrace.setEnabled(false);
        propertiesTrace.setHeaderName("X-Trace-Id");
        propertiesTrace.setGenerator("snowflake");
        
        LogConfigProperties.PerformanceConfig propertiesPerf = propertiesConfig.getPerformance();
        propertiesPerf.setEnabled(true);
        propertiesPerf.setSlowThreshold(1000L);
        
        // 模拟bean工厂行为
        when(beanFactory.containsBean("atlasLogAnnotationConfigProcessor")).thenReturn(true);
        when(beanFactory.getBean("atlasLogAnnotationConfigProcessor", AnnotationConfigProcessor.class))
            .thenReturn(annotationProcessor);
        when(annotationProcessor.getAnnotationConfig()).thenReturn(annotationConfig);
        
        when(beanFactory.containsBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties")).thenReturn(true);
        when(beanFactory.getBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties", LogConfigProperties.class))
            .thenReturn(propertiesConfig);
        
        // 执行合并
        configMerger.postProcessBeanFactory(beanFactory);
        
        // 验证相关操作被调用
        verify(conflictDetector).detectConflicts(annotationConfig, propertiesConfig);
        verify(configValidator).validate(any(LogConfigProperties.class));
        verify(beanFactory).registerSingleton(eq("atlasLogMergedConfig"), any(LogConfigProperties.class));
    }
    
    @Test
    void testPostProcessBeanFactoryWithValidationFailure() {
        // 准备配置
        LogConfigProperties annotationConfig = new LogConfigProperties();
        LogConfigProperties propertiesConfig = new LogConfigProperties();
        
        // 模拟bean工厂行为
        when(beanFactory.containsBean("atlasLogAnnotationConfigProcessor")).thenReturn(true);
        when(beanFactory.getBean("atlasLogAnnotationConfigProcessor", AnnotationConfigProcessor.class))
            .thenReturn(annotationProcessor);
        when(annotationProcessor.getAnnotationConfig()).thenReturn(annotationConfig);
        
        when(beanFactory.containsBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties")).thenReturn(true);
        when(beanFactory.getBean("atlas.log-io.github.nemoob.atlas.log.config.LogConfigProperties", LogConfigProperties.class))
            .thenReturn(propertiesConfig);
        
        // 模拟验证失败
        doThrow(new IllegalArgumentException("Validation failed")).when(configValidator).validate(any());
        
        // 执行合并（应该抛出异常）
        assertThrows(IllegalStateException.class, () -> configMerger.postProcessBeanFactory(beanFactory));
        
        // 验证相关操作被调用
        verify(conflictDetector).detectConflicts(annotationConfig, propertiesConfig);
        verify(configValidator).validate(any(LogConfigProperties.class));
    }
}