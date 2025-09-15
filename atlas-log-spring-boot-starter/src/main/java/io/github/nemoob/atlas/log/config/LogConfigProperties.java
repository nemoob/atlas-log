package io.github.nemoob.atlas.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Atlas Log配置属性
 * 
 * @author nemoob
 * @since 0.2.0
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
     * 敏感数据脱敏配置
     */
    private SensitiveConfig sensitive = new SensitiveConfig();
    
    // 序列化器配置已移除，统一使用 Fastjson
    
    /**
     * 参数格式配置
     */
    private ArgumentFormatConfig argumentFormat = new ArgumentFormatConfig();
    
    /**
     * HTTP请求日志配置
     */
    private HttpLogConfig httpLog = new HttpLogConfig();
    
    /**
     * 返回值记录配置
     */
    private ResultLogConfig resultLog = new ResultLogConfig();
    
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
            this.argumentFormat = new ArgumentFormatConfig(other.argumentFormat);
            this.httpLog = new HttpLogConfig(other.httpLog);
            this.resultLog = new ResultLogConfig(other.resultLog);
        }
    }
    
    /**
     * 默认构造函数
     */
    public LogConfigProperties() {
        // 使用默认值初始化
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
         * 自定义敏感字段（用于反射模式）
         */
        private List<String> customFields = new ArrayList<>();
        
        /**
          * 脱敏替换值
          */
         private String maskValue = "***";
     }
     
     // SerializerConfig 已移除，统一使用 Fastjson
     
     /**
      * 参数格式配置
      */
     @Data
     public static class ArgumentFormatConfig {
         
         /**
          * 参数输出格式类型
          * JSON: 使用 JSON 格式输出参数 (默认)
          * KEY_VALUE: 使用 key=value&key2=value2 格式输出参数
          */
         private ArgumentFormatType type = ArgumentFormatType.JSON;
         
         /**
          * key=value 格式的分隔符
          */
         private String separator = "&";
         
         /**
          * key=value 格式的键值分隔符
          */
         private String keyValueSeparator = "=";
         
         /**
           * 是否包含参数索引作为键名
           * true: arg0=value1&arg1=value2
           * false: value1&value2
           */
          private boolean includeParameterIndex = true;
          
          /**
           * 自定义格式化器名称
           * 当 type = CUSTOM 时使用
           */
          private String customFormatterName = "";
          
          /**
           * 自定义格式化器配置
           * 用于传递给自定义格式化器的配置参数
           */
          private Map<String, Object> customFormatterConfig = new HashMap<>();
         
         /**
           * 复制构造函数
           */
          public ArgumentFormatConfig(ArgumentFormatConfig other) {
              if (other != null) {
                  this.type = other.type;
                  this.separator = other.separator;
                  this.keyValueSeparator = other.keyValueSeparator;
                  this.includeParameterIndex = other.includeParameterIndex;
                  this.customFormatterName = other.customFormatterName;
                  this.customFormatterConfig = new HashMap<>(other.customFormatterConfig);
              }
          }
         
         /**
          * 默认构造函数
          */
         public ArgumentFormatConfig() {
         }
     }
     
     /**
       * 参数格式类型枚举
       */
      public enum ArgumentFormatType {
          /**
           * JSON 格式
           */
          JSON,
          
          /**
           * key=value 格式
           */
          KEY_VALUE,
          
          /**
           * 自定义格式化器
           */
          CUSTOM
      }
     
     /**
     * HTTP请求日志配置
     */
    @Data
    public static class HttpLogConfig {
        
        /**
         * 复制构造函数
         */
        public HttpLogConfig(HttpLogConfig other) {
            if (other != null) {
                this.logFullParameters = other.logFullParameters;
                this.urlFormat = other.urlFormat;
                this.includeQueryString = other.includeQueryString;
                this.includeHeaders = other.includeHeaders;
                this.excludeHeaders = new ArrayList<>(other.excludeHeaders);
            }
        }
        
        /**
         * 默认构造函数
         */
        public HttpLogConfig() {
            // 使用默认值
        }
        
        /**
         * 是否记录完整的请求参数
         */
        private boolean logFullParameters = true;
        
        /**
         * URL格式化模式
         * 支持占位符：{method}, {uri}, {queryString}, {remoteAddr}
         */
        private String urlFormat = "{uri}{queryString}";
        
        /**
         * 是否包含查询字符串
         */
        private boolean includeQueryString = true;
        
        /**
         * 是否包含请求头信息
         */
        private boolean includeHeaders = false;
        
        /**
         * 排除的请求头（敏感信息）
         */
        private List<String> excludeHeaders = new ArrayList<String>() {{
            add("authorization");
            add("cookie");
            add("x-auth-token");
        }};
     }
     
     /**
      * 返回值记录配置
      */
     @Data
     public static class ResultLogConfig {
         
         /**
          * 复制构造函数
          */
         public ResultLogConfig(ResultLogConfig other) {
             if (other != null) {
                 this.enabled = other.enabled;
                 this.maxLength = other.maxLength;
                 this.printAll = other.printAll;
                 this.truncateMessage = other.truncateMessage;
             }
         }
         
         /**
          * 默认构造函数
          */
         public ResultLogConfig() {
             // 使用默认值
         }
         
         /**
          * 是否全局启用返回值记录
          * 当此项为 false 时，即使 @Log 注解中 logResult=true 也不会记录返回值
          */
         private boolean enabled = true;
         
         /**
          * 返回值最大长度限制
          * -1 表示不限制长度（全部打印）
          */
         private int maxLength = 1000;
         
         /**
          * 是否打印完整返回值（忽略长度限制）
          */
         private boolean printAll = false;
         
         /**
          * 返回值被截断时的提示信息
          */
         private String truncateMessage = "[TRUNCATED]";
     }
 }