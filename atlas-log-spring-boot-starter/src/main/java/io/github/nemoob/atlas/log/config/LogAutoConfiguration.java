package io.github.nemoob.atlas.log.config;

import io.github.nemoob.atlas.log.aspect.AtlasLogAspect;
import io.github.nemoob.atlas.log.async.TraceIdTaskDecorator;
import io.github.nemoob.atlas.log.expression.SpelExpressionEvaluator;
import io.github.nemoob.atlas.log.serializer.ArgumentSerializer;
import io.github.nemoob.atlas.log.serializer.ArgumentFormatConfig;
import io.github.nemoob.atlas.log.serializer.ArgumentFormatType;
import io.github.nemoob.atlas.log.serializer.ArgumentFormatter;
import io.github.nemoob.atlas.log.serializer.ArgumentFormatterManager;
import io.github.nemoob.atlas.log.serializer.FastjsonArgumentSerializer;
import io.github.nemoob.atlas.log.serializer.JsonArgumentFormatter;
import io.github.nemoob.atlas.log.serializer.KeyValueArgumentFormatter;
import io.github.nemoob.atlas.log.serializer.SensitiveDataMasker;
import io.github.nemoob.atlas.log.comparator.JsonPathValueExtractor;
import io.github.nemoob.atlas.log.processor.JsonPathCompareProcessor;
import io.github.nemoob.atlas.log.web.LoggingFilter;
import io.github.nemoob.atlas.log.web.TraceIdInterceptor;
// Jackson 相关导入已移除，使用 Fastjson 替代
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

/**
 * Atlas Log自动配置类
 * <p>
 * 支持注解配置和属性文件配置两种方式，
 * 并按照优先级策略进行配置合并。
 * </p>
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Configuration
@EnableConfigurationProperties(LogConfigProperties.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)  // 添加这行
@ConditionalOnProperty(prefix = "atlas.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class LogAutoConfiguration {
    
    /**
     * 获取最终的配置（优先使用合并后的配置）
     */
    private LogConfigProperties getEffectiveConfig(ApplicationContext applicationContext,
                                                   LogConfigProperties defaultConfig) {
        try {
            // 尝试获取合并后的配置
            LogConfigProperties mergedConfig = applicationContext.getBean(
                "atlasLogMergedConfig", LogConfigProperties.class);
            log.debug("Using merged annotation configuration");
            return mergedConfig;
        } catch (Exception e) {
            // 如果没有合并配置，使用默认的属性配置
            log.debug("Using default properties configuration");
            return defaultConfig;
        }
    }
    
    /**
     * 配置敏感数据脱敏器
     */
    @Bean
    @ConditionalOnMissingBean
    public SensitiveDataMasker sensitiveDataMasker(LogConfigProperties properties,
                                                   ApplicationContext applicationContext) {
        LogConfigProperties effectiveConfig = getEffectiveConfig(applicationContext, properties);
        
        SensitiveDataMasker masker = new SensitiveDataMasker(effectiveConfig.getSensitive().isEnabled());
        
        // 添加自定义敏感字段
        if (effectiveConfig.getSensitive().getCustomFields() != null) {
            for (String field : effectiveConfig.getSensitive().getCustomFields()) {
                masker.addSensitiveField(field);
            }
        }
        
        log.info("Sensitive data masker configured, enabled: {}", effectiveConfig.getSensitive().isEnabled());
        return masker;
    }
    
    // ObjectMapper 配置已移除，使用 Fastjson 替代
    
    /**
     * 配置参数格式化器管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public ArgumentFormatterManager argumentFormatterManager(SensitiveDataMasker sensitiveDataMasker,
                                                           LogConfigProperties properties,
                                                           ApplicationContext applicationContext) {
        LogConfigProperties effectiveConfig = getEffectiveConfig(applicationContext, properties);
        LogConfigProperties.ArgumentFormatConfig config = effectiveConfig.getArgumentFormat();
        
        // 创建内置格式化器
        JsonArgumentFormatter jsonFormatter = new JsonArgumentFormatter(sensitiveDataMasker);
        KeyValueArgumentFormatter keyValueFormatter = new KeyValueArgumentFormatter(
            sensitiveDataMasker,
            config.getSeparator(),
            config.getKeyValueSeparator(),
            config.isIncludeParameterIndex()
        );
        
        // 创建格式化器管理器
        ArgumentFormatterManager manager = new ArgumentFormatterManager(jsonFormatter, "json");
        
        // 注册内置格式化器
        manager.registerFormatter("json", jsonFormatter);
        manager.registerFormatter("key-value", keyValueFormatter);
        
        // 注册用户自定义格式化器（如果存在）
        registerCustomFormatters(manager, applicationContext);
        
        log.info("ArgumentFormatterManager configured with formatters: {}", manager.getFormatterNames());
        return manager;
    }
    
    /**
     * 注册用户自定义格式化器
     */
    private void registerCustomFormatters(ArgumentFormatterManager manager, ApplicationContext applicationContext) {
        try {
            // 获取所有 ArgumentFormatter 类型的 Bean
            Map<String, ArgumentFormatter> customFormatters = applicationContext.getBeansOfType(ArgumentFormatter.class);
            
            for (Map.Entry<String, ArgumentFormatter> entry : customFormatters.entrySet()) {
                String beanName = entry.getKey();
                ArgumentFormatter formatter = entry.getValue();
                
                // 跳过内置格式化器
                if (formatter instanceof JsonArgumentFormatter || formatter instanceof KeyValueArgumentFormatter) {
                    continue;
                }
                
                // 注册自定义格式化器
                String formatterName = formatter.getName();
                if (formatterName == null || formatterName.trim().isEmpty()) {
                    formatterName = beanName;
                }
                
                manager.registerFormatter(formatterName, formatter);
                log.info("Registered custom formatter: {} -> {}", formatterName, formatter.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.warn("Failed to register custom formatters: {}", e.getMessage());
        }
    }
    
    /**
     * 配置参数格式配置（兼容性保持）
     */
    @Bean
    @ConditionalOnMissingBean
    public ArgumentFormatConfig argumentFormatConfig(LogConfigProperties properties,
                                                    ApplicationContext applicationContext) {
        LogConfigProperties effectiveConfig = getEffectiveConfig(applicationContext, properties);
        LogConfigProperties.ArgumentFormatConfig config = effectiveConfig.getArgumentFormat();
        
        ArgumentFormatType type = ArgumentFormatType.JSON;
        if (config.getType() == LogConfigProperties.ArgumentFormatType.KEY_VALUE) {
            type = ArgumentFormatType.KEY_VALUE;
        }
        
        return new ArgumentFormatConfig(
            type,
            config.getSeparator(),
            config.getKeyValueSeparator(),
            config.isIncludeParameterIndex()
        );
    }
    
    /**
     * 配置参数序列化器（Fastjson）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.alibaba.fastjson2.JSON")
    public FastjsonArgumentSerializer fastjsonArgumentSerializer(SensitiveDataMasker sensitiveDataMasker,
                                                                 ArgumentFormatConfig argumentFormatConfig) {
        log.info("Using Fastjson-based argument serializer with format: {}", argumentFormatConfig.getType());
        return new FastjsonArgumentSerializer(sensitiveDataMasker, argumentFormatConfig);
    }
    
    /**
     * 配置统一的参数序列化器接口
     */
    @Bean
    @ConditionalOnMissingBean(ArgumentSerializer.class)
    public ArgumentSerializer argumentSerializer(FastjsonArgumentSerializer fastjsonSerializer) {
        log.info("ArgumentSerializer configured with Fastjson implementation");
        return fastjsonSerializer;
    }
    
    /**
     * 配置SpEL表达式评估器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "atlas.log", name = "spel-enabled", havingValue = "true", matchIfMissing = true)
    public SpelExpressionEvaluator spelExpressionEvaluator(ApplicationContext applicationContext,
                                                           LogConfigProperties properties) {
        LogConfigProperties effectiveConfig = getEffectiveConfig(applicationContext, properties);
        LogConfigProperties.ConditionConfig conditionConfig = effectiveConfig.getCondition();
        
        SpelExpressionEvaluator evaluator = new SpelExpressionEvaluator(
                applicationContext,
                conditionConfig.isCacheEnabled(),
                conditionConfig.getTimeoutMs(),
                conditionConfig.isFailSafe()
        );
        
        log.info("SpEL expression evaluator configured successfully, cache enabled: {}, timeout: {}ms", 
                conditionConfig.isCacheEnabled(), conditionConfig.getTimeoutMs());
        return evaluator;
    }
    
    /**
     * 配置 JsonPath 值提取器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.jayway.jsonpath.JsonPath")
    public JsonPathValueExtractor jsonPathValueExtractor() {
        return new JsonPathValueExtractor(true);
    }
    
    /**
     * 配置 JsonPath 比较处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.jayway.jsonpath.JsonPath")
    public JsonPathCompareProcessor jsonPathCompareProcessor(JsonPathValueExtractor jsonPathValueExtractor) {
        return new JsonPathCompareProcessor(jsonPathValueExtractor);
    }
    
    /**
     * 配置 JsonPath 比较处理器（降级版本）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass("com.jayway.jsonpath.JsonPath")
    public JsonPathCompareProcessor jsonPathCompareProcessorFallback() {
        // 当 JsonPath 不可用时，创建一个空的处理器
        return new JsonPathCompareProcessor(null);
    }
    
    /**
     * 配置日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public AtlasLogAspect atlasLogAspect(SpelExpressionEvaluator spelExpressionEvaluator,
                                    ArgumentSerializer argumentSerializer,
                                    JsonPathCompareProcessor jsonPathCompareProcessor,
                                    ArgumentFormatterManager argumentFormatterManager) {
        log.info("Atlas Log aspect configured successfully");
        return new AtlasLogAspect(spelExpressionEvaluator, argumentSerializer, jsonPathCompareProcessor, argumentFormatterManager);
    }
    
    /**
     * 配置 TraceId 任务装饰器
     * 用于异步执行时传递 TraceId
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.core.task.TaskDecorator")
    public TaskDecorator traceIdTaskDecorator() {
        log.info("Atlas Log TraceId task decorator configured successfully");
        return new TraceIdTaskDecorator();
    }
    
    /**
     * Web相关配置
     */
    @Configuration
    @ConditionalOnWebApplication
    @ConditionalOnClass(name = "javax.servlet.Filter")
    public static class WebConfiguration implements WebMvcConfigurer {
        
        private final LogConfigProperties properties;
        private final ApplicationContext applicationContext;
        
        public WebConfiguration(LogConfigProperties properties, ApplicationContext applicationContext) {
            this.properties = properties;
            this.applicationContext = applicationContext;
        }
        
        /**
         * 配置TraceId拦截器
         */
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "atlas.log.trace-id", name = "enabled", havingValue = "true", matchIfMissing = true)
        public TraceIdInterceptor traceIdInterceptor() {
            LogConfigProperties effectiveConfig = getEffectiveConfig();
            return new TraceIdInterceptor(effectiveConfig.getTraceId().getHeaderName());
        }
        
        /**
         * 配置日志过滤器
         */
        @Bean
        @ConditionalOnMissingBean(name = "atlasLogFilterRegistration")
        public FilterRegistrationBean<LoggingFilter> atlasLogFilterRegistration(ArgumentFormatConfig argumentFormatConfig) {
            LogConfigProperties effectiveConfig = getEffectiveConfig();
            
            FilterRegistrationBean<LoggingFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new LoggingFilter(effectiveConfig, argumentFormatConfig));
            registration.addUrlPatterns("/*");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            registration.setName("atlasLoggingFilter");
            
            log.info("Atlas Log filter configured successfully with argument format: {}", argumentFormatConfig.getType());
            return registration;
        }
        
        /**
         * 注册拦截器
         */
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            LogConfigProperties effectiveConfig = getEffectiveConfig();
            
            if (effectiveConfig.getTraceId().isEnabled()) {
                registry.addInterceptor(traceIdInterceptor())
                        .addPathPatterns("/**")
                        .order(Ordered.HIGHEST_PRECEDENCE + 1);
                log.info("TraceId interceptor configured successfully, header name: {}", effectiveConfig.getTraceId().getHeaderName());
            }
        }
        
        /**
         * 获取有效配置
         */
        private LogConfigProperties getEffectiveConfig() {
            try {
                LogConfigProperties mergedConfig = applicationContext.getBean("atlasLogMergedConfig", LogConfigProperties.class);
                log.debug("Successfully retrieved merged configuration: atlasLogMergedConfig");
                if (mergedConfig.getHttpLog() != null) {
                    log.debug("Merged config HTTP Log urlFormat: '{}'", mergedConfig.getHttpLog().getUrlFormat());
                }
                return mergedConfig;
            } catch (Exception e) {
                log.warn("Failed to retrieve merged configuration 'atlasLogMergedConfig', falling back to properties: {}", e.getMessage());
                if (properties.getHttpLog() != null) {
                    log.debug("Fallback properties HTTP Log urlFormat: '{}'", properties.getHttpLog().getUrlFormat());
                }
                return properties;
            }
        }
    }
    
    /**
     * 条件配置：当没有启用SpEL时的默认配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "atlas.log", name = "spel-enabled", havingValue = "false")
    public static class NoSpelConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public SpelExpressionEvaluator spelExpressionEvaluator() {
            log.warn("SpEL expression is disabled, using empty implementation");
            return new SpelExpressionEvaluator(null, false, 0, true);
        }
    }
}