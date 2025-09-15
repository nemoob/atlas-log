package io.github.nemoob.atlas.log.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

import java.util.Map;

/**
 * key=value 格式的参数格式化器
 * 
 * @author nemoob
 * @since 0.2.0
 */
public class KeyValueArgumentFormatter implements ArgumentFormatter {
    
    private final SensitiveDataMasker sensitiveDataMasker;
    private final String separator;
    private final String keyValueSeparator;
    private final boolean includeParameterIndex;
    
    public KeyValueArgumentFormatter(SensitiveDataMasker sensitiveDataMasker) {
        this(sensitiveDataMasker, "&", "=", true);
    }
    
    public KeyValueArgumentFormatter(SensitiveDataMasker sensitiveDataMasker, 
                                   String separator, 
                                   String keyValueSeparator, 
                                   boolean includeParameterIndex) {
        this.sensitiveDataMasker = sensitiveDataMasker;
        this.separator = separator;
        this.keyValueSeparator = keyValueSeparator;
        this.includeParameterIndex = includeParameterIndex;
    }
    
    @Override
    public String formatArguments(Object[] args, FormatterContext context) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(separator);
                }
                
                // 添加键名
                if (includeParameterIndex) {
                    sb.append("arg").append(i).append(keyValueSeparator);
                }
                
                // 添加值
                Object maskedArg = sensitiveDataMasker.maskSensitiveData(args[i]);
                String value = formatValueForKeyValue(maskedArg);
                sb.append(value);
            }
            
            return truncateIfNecessary(sb.toString(), context.getMaxLength());
        } catch (Exception e) {
            return "[FORMATTING_FAILED: " + e.getMessage() + "]";
        }
    }
    
    @Override
    public String formatResult(Object result, FormatterContext context) {
        if (result == null) {
            return "null";
        }
        
        try {
            Object maskedResult = sensitiveDataMasker.maskSensitiveData(result);
            String value = formatValueForKeyValue(maskedResult);
            return truncateIfNecessary(value, context.getMaxLength());
        } catch (Exception e) {
            return "[FORMATTING_FAILED: " + e.getMessage() + "]";
        }
    }
    
    @Override
    public String formatHttpParameters(Map<String, String[]> parameters, FormatterContext context) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();
                
                if (!first) {
                    sb.append(separator);
                }
                first = false;
                
                sb.append(key).append(keyValueSeparator);
                
                if (values.length == 1) {
                    sb.append(values[0]);
                } else {
                    sb.append("[");
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append(values[i]);
                    }
                    sb.append("]");
                }
            }
            
            return truncateIfNecessary(sb.toString(), context.getMaxLength());
        } catch (Exception e) {
            return "[FORMATTING_FAILED: " + e.getMessage() + "]";
        }
    }
    
    @Override
    public String getName() {
        return "key-value";
    }
    
    /**
     * 格式化值用于 key=value 输出
     */
    private String formatValueForKeyValue(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        // 基本类型和字符串直接输出
        if (isPrimitiveOrWrapper(obj) || obj instanceof String) {
            return String.valueOf(obj);
        }
        
        // 复杂对象使用 JSON 序列化
        try {
            return JSON.toJSONString(obj, 
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.IgnoreNonFieldGetter);
        } catch (Exception e) {
            // 序列化失败时返回对象描述
            return "[" + obj.getClass().getSimpleName() + "@" + Integer.toHexString(obj.hashCode()) + "]";
        }
    }
    
    /**
     * 检查是否为基本类型或包装类型
     */
    private boolean isPrimitiveOrWrapper(Object obj) {
        if (obj == null) {
            return false;
        }
        
        Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() ||
               clazz == Boolean.class ||
               clazz == Byte.class ||
               clazz == Character.class ||
               clazz == Short.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Float.class ||
               clazz == Double.class;
    }
    
    private String truncateIfNecessary(String str, int maxLength) {
        if (maxLength > 0 && str.length() > maxLength) {
            return str.substring(0, maxLength) + "[TRUNCATED]";
        }
        return str;
    }
}