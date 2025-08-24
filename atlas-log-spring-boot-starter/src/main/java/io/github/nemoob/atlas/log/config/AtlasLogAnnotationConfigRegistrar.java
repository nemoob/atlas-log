package io.github.nemoob.atlas.log.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Atlas Log 注解配置注册器
 * <p>
 * 负责解析 @EnableAtlasLog 注解并注册相关的配置处理器组件到Spring容器中。
 * </p>
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public class AtlasLogAnnotationConfigRegistrar implements ImportBeanDefinitionRegistrar {
    
    private static final Logger logger = LoggerFactory.getLogger(AtlasLogAnnotationConfigRegistrar.class);
    
    private static final String ENABLE_ATLAS_LOG_ANNOTATION = "io.github.nemoob.atlas.log.annotation.EnableAtlasLog";
    private static final String ANNOTATION_CONFIG_PROCESSOR_BEAN_NAME = "atlasLogAnnotationConfigProcessor";
    private static final String CONFIG_MERGER_BEAN_NAME = "atlasLogConfigMerger";
    
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, 
                                      BeanDefinitionRegistry registry) {
        
        if (!importingClassMetadata.hasAnnotation(ENABLE_ATLAS_LOG_ANNOTATION)) {
            logger.debug("@EnableAtlasLog annotation not found, skipping registration");
            return;
        }
        
        // 获取注解属性
        Map<String, Object> attributes = importingClassMetadata
            .getAnnotationAttributes(ENABLE_ATLAS_LOG_ANNOTATION);
            
        if (attributes == null) {
            logger.warn("@EnableAtlasLog annotation attributes is null, using default configuration");
            attributes = getDefaultAttributes();
        }
        
        logger.info("Registering Atlas Log annotation configuration with attributes: {}", attributes);
        
        // 注册注解配置处理器
        registerAnnotationConfigProcessor(registry, attributes);
        
        // 注册配置合并器
        registerConfigMerger(registry);
        
        logger.info("Atlas Log annotation configuration registration completed successfully");
    }
    
    /**
     * 注册注解配置处理器
     */
    private void registerAnnotationConfigProcessor(BeanDefinitionRegistry registry, 
                                                 Map<String, Object> attributes) {
        if (registry.containsBeanDefinition(ANNOTATION_CONFIG_PROCESSOR_BEAN_NAME)) {
            logger.debug("AnnotationConfigProcessor already registered, skipping");
            return;
        }
        
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
            .genericBeanDefinition(AnnotationConfigProcessor.class);
        builder.addConstructorArgValue(attributes);
        // builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE); // Spring 5.x+ only
        
        registry.registerBeanDefinition(ANNOTATION_CONFIG_PROCESSOR_BEAN_NAME, 
                                       builder.getBeanDefinition());
        
        logger.debug("Registered AnnotationConfigProcessor bean: {}", ANNOTATION_CONFIG_PROCESSOR_BEAN_NAME);
    }
    
    /**
     * 注册配置合并器
     */
    private void registerConfigMerger(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(CONFIG_MERGER_BEAN_NAME)) {
            logger.debug("ConfigMerger already registered, skipping");
            return;
        }
        
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
            .genericBeanDefinition(ConfigMerger.class);
        // builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE); // Spring 5.x+ only
        
        registry.registerBeanDefinition(CONFIG_MERGER_BEAN_NAME, 
                                       builder.getBeanDefinition());
        
        logger.debug("Registered ConfigMerger bean: {}", CONFIG_MERGER_BEAN_NAME);
    }
    
    /**
     * 获取默认属性
     */
    private Map<String, Object> getDefaultAttributes() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("enabled", true);
        defaults.put("defaultLevel", "INFO");
        defaults.put("dateFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        defaults.put("prettyPrint", false);
        defaults.put("maxMessageLength", 2000);
        defaults.put("spelEnabled", true);
        defaults.put("conditionEnabled", true);
        defaults.put("enabledTags", new String[]{"business", "security", "api"});
        defaults.put("enabledGroups", new String[]{"default", "business"});
        defaults.put("exclusions", new String[]{"*.toString", "*.hashCode", "*.equals"});
        return defaults;
    }
}