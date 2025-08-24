package io.github.nemoob.atlas.log.config;

import io.github.nemoob.atlas.log.aspect.LogAspect;
import io.github.nemoob.atlas.log.expression.SpelExpressionEvaluator;
import io.github.nemoob.atlas.log.serializer.JsonArgumentSerializer;
import io.github.nemoob.atlas.log.serializer.SensitiveDataMasker;
import io.github.nemoob.atlas.log.web.LoggingFilter;
import io.github.nemoob.atlas.log.web.TraceIdInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Atlas Log自动配置类
 * <p>
 * 支持注解配置和属性文件配置两种方式，
 * 并按照优先级策略进行配置合并。
 * </p>
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(LogConfigProperties.class)
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
            log.debug("使用合并后的注解配置");
            return mergedConfig;
        } catch (Exception e) {
            // 如果没有合并配置，使用默认的属性配置
            log.debug("使用默认属性文件配置");
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
        
        log.info("配置敏感数据脱敏器完成，启用状态: {}", effectiveConfig.getSensitive().isEnabled());
        return masker;
    }
    
    /**
     * 配置ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean(name = "atlasLogObjectMapper")
    public ObjectMapper atlasLogObjectMapper(LogConfigProperties properties,
                                            ApplicationContext applicationContext) {
        LogConfigProperties effectiveConfig = getEffectiveConfig(applicationContext, properties);
        
        ObjectMapper mapper = new ObjectMapper();
        
        if (effectiveConfig.isPrettyPrint()) {
            mapper.writerWithDefaultPrettyPrinter();
        }
        
        // 配置日期格式
        if (effectiveConfig.getDateFormat() != null && !effectiveConfig.getDateFormat().isEmpty()) {
            mapper.setDateFormat(new java.text.SimpleDateFormat(effectiveConfig.getDateFormat()));
        }
        
        return mapper;
    }
    
    /**
     * 配置参数序列化器
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonArgumentSerializer jsonArgumentSerializer(ObjectMapper atlasLogObjectMapper,
                                                         SensitiveDataMasker sensitiveDataMasker) {
        return new JsonArgumentSerializer(atlasLogObjectMapper, sensitiveDataMasker);
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
        
        log.info("配置SpEL表达式评估器完成，缓存启用: {}, 超时时间: {}ms", 
                conditionConfig.isCacheEnabled(), conditionConfig.getTimeoutMs());
        return evaluator;
    }
    
    /**
     * 配置日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public LogAspect logAspect(SpelExpressionEvaluator spelExpressionEvaluator,
                               JsonArgumentSerializer argumentSerializer) {
        log.info("配置Atlas Log切面完成");
        return new LogAspect(spelExpressionEvaluator, argumentSerializer);
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
        @ConditionalOnMissingBean
        public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration() {
            LogConfigProperties effectiveConfig = getEffectiveConfig();
            
            FilterRegistrationBean<LoggingFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new LoggingFilter(effectiveConfig));
            registration.addUrlPatterns("/*");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
            registration.setName("atlasLoggingFilter");
            
            log.info("配置Atlas Log过滤器完成");
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
                        .order(Ordered.HIGHEST_PRECEDENCE);
                log.info("配置TraceId拦截器完成，Header名称: {}", effectiveConfig.getTraceId().getHeaderName());
            }
        }
        
        /**
         * 获取有效配置
         */
        private LogConfigProperties getEffectiveConfig() {
            try {
                return applicationContext.getBean("atlasLogMergedConfig", LogConfigProperties.class);
            } catch (Exception e) {
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
            log.warn("SpEL表达式已禁用，使用空实现");
            return new SpelExpressionEvaluator(null, false, 0, true);
        }
    }
}