package io.github.nemoob.atlas.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Atlas Log配置属性
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "atlas.log")
public class LogConfigProperties {
    
    /**
     * 是否启用Atlas Log
     */
    private boolean enabled = true;
    
    /**
     * 默认日志级别
     */
    private String defaultLevel = "INFO";
    
    /**
     * 日期格式
     */
    private String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
    
    /**
     * 是否美化JSON输出
     */
    private boolean prettyPrint = false;
    
    /**
     * 最大消息长度
     */
    private int maxMessageLength = 2000;
    
    /**
     * 是否启用SpEL表达式
     */
    private boolean spelEnabled = true;
    
    /**
     * 是否启用条件评估
     */
    private boolean conditionEnabled = true;
    
    /**
     * 启用的日志标签
     */
    private List<String> enabledTags = new ArrayList<>();
    
    /**
     * 启用的日志组
     */
    private List<String> enabledGroups = new ArrayList<>();
    
    /**
     * 排除的方法模式
     */
    private List<String> exclusions = new ArrayList<>();
    
    /**
     * 链路追踪配置
     */
    private TraceIdConfig traceId = new TraceIdConfig();
    
    /**
     * 性能监控配置
     */
    private PerformanceConfig performance = new PerformanceConfig();
    
    /**
     * 条件评估配置
     */
    private ConditionConfig condition = new ConditionConfig();
    
    /**
     * 敏感数据配置
     */
    private SensitiveConfig sensitive = new SensitiveConfig();
    
    /**
     * 复制构造函数（用于配置合并）
     */
    public LogConfigProperties(LogConfigProperties other) {
        if (other != null) {
            this.enabled = other.enabled;
            this.defaultLevel = other.defaultLevel;
            this.dateFormat = other.dateFormat;
            this.prettyPrint = other.prettyPrint;
            this.maxMessageLength = other.maxMessageLength;
            this.spelEnabled = other.spelEnabled;
            this.conditionEnabled = other.conditionEnabled;
            this.enabledTags = new ArrayList<>(other.enabledTags);
            this.enabledGroups = new ArrayList<>(other.enabledGroups);
            this.exclusions = new ArrayList<>(other.exclusions);
            this.traceId = new TraceIdConfig(other.traceId);
            this.performance = new PerformanceConfig(other.performance);
            this.condition = new ConditionConfig(other.condition);
            this.sensitive = new SensitiveConfig(other.sensitive);
        }
    }
    
    /**
     * 默认构造函数
     */
    public LogConfigProperties() {
        // 使用默认值初始化
    }
    
    /**
     * 检查配置是否为空（用于合并判断）
     */
    public boolean isEmpty() {
        return enabledTags.isEmpty() && enabledGroups.isEmpty() && exclusions.isEmpty();
    }
    
    /**
     * 链路追踪配置
     */
    @Data
    public static class TraceIdConfig {
        
        /**
         * 复制构造函数
         */
        public TraceIdConfig(TraceIdConfig other) {
            if (other != null) {
                this.enabled = other.enabled;
                this.headerName = other.headerName;
                this.generator = other.generator;
            }
        }
        
        /**
         * 默认构造函数
         */
        public TraceIdConfig() {
            // 使用默认值
        }
        /**
         * 是否启用链路追踪
         */
        private boolean enabled = true;
        
        /**
         * HTTP头名称
         */
        private String headerName = "X-Trace-Id";
        
        /**
         * 生成器类型：uuid, snowflake
         */
        private String generator = "uuid";
    }
    
    /**
     * 性能监控配置
     */
    @Data
    public static class PerformanceConfig {
        
        /**
         * 复制构造函数
         */
        public PerformanceConfig(PerformanceConfig other) {
            if (other != null) {
                this.enabled = other.enabled;
                this.slowThreshold = other.slowThreshold;
                this.logSlowMethods = other.logSlowMethods;
            }
        }
        
        /**
         * 默认构造函数
         */
        public PerformanceConfig() {
            // 使用默认值
        }
        /**
         * 是否启用性能监控
         */
        private boolean enabled = true;
        
        /**
         * 慢方法阈值（毫秒）
         */
        private long slowThreshold = 1000;
        
        /**
         * 是否记录慢方法日志
         */
        private boolean logSlowMethods = true;
    }
    
    /**
     * 条件评估配置
     */
    @Data
    public static class ConditionConfig {
        
        /**
         * 复制构造函数
         */
        public ConditionConfig(ConditionConfig other) {
            if (other != null) {
                this.cacheEnabled = other.cacheEnabled;
                this.timeoutMs = other.timeoutMs;
                this.failSafe = other.failSafe;
            }
        }
        
        /**
         * 默认构造函数
         */
        public ConditionConfig() {
            // 使用默认值
        }
        /**
         * 是否启用表达式缓存
         */
        private boolean cacheEnabled = true;
        
        /**
         * 表达式执行超时时间（毫秒）
         */
        private long timeoutMs = 1000;
        
        /**
         * 表达式执行失败时是否仍然记录日志
         */
        private boolean failSafe = true;
    }
    
    /**
     * 敏感数据配置
     */
    @Data
    public static class SensitiveConfig {
        
        /**
         * 复制构造函数
         */
        public SensitiveConfig(SensitiveConfig other) {
            if (other != null) {
                this.enabled = other.enabled;
                this.customFields = new ArrayList<>(other.customFields);
                this.maskValue = other.maskValue;
            }
        }
        
        /**
         * 默认构造函数
         */
        public SensitiveConfig() {
            // 使用默认值
        }
        /**
         * 是否启用敏感数据脱敏
         */
        private boolean enabled = true;
        
        /**
         * 自定义敏感字段
         */
        private List<String> customFields = new ArrayList<>();
        
        /**
         * 脱敏标记
         */
        private String maskValue = "***";
    }
}