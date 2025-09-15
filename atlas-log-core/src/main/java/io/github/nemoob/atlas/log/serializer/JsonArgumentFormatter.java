package io.github.nemoob.atlas.log.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

import java.util.Map;

/**
 * JSON 格式的参数格式化器
 * 
 * @author nemoob
 * @since 0.2.0
 */
public class JsonArgumentFormatter implements ArgumentFormatter {
    
    private final SensitiveDataMasker sensitiveDataMasker;
    
    public JsonArgumentFormatter(SensitiveDataMasker sensitiveDataMasker) {
        this.sensitiveDataMasker = sensitiveDataMasker;
    }
    
    @Override
    public String formatArguments(Object[] args, FormatterContext context) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            // 脱敏处理
            Object[] maskedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                maskedArgs[i] = sensitiveDataMasker.maskSensitiveData(args[i]);
            }
            
            String result = JSON.toJSONString(maskedArgs,
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.IgnoreNonFieldGetter,
                JSONWriter.Feature.WriteNullListAsEmpty,
                JSONWriter.Feature.WriteNullStringAsEmpty);
            
            return truncateIfNecessary(result, context.getMaxLength());
        } catch (Exception e) {
            return "[SERIALIZATION_FAILED: " + e.getMessage() + "]";
        }
    }
    
    @Override
    public String formatResult(Object result, FormatterContext context) {
        if (result == null) {
            return "null";
        }
        
        try {
            Object maskedResult = sensitiveDataMasker.maskSensitiveData(result);
            
            String serialized = JSON.toJSONString(maskedResult,
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.IgnoreNonFieldGetter,
                JSONWriter.Feature.WriteNullListAsEmpty,
                JSONWriter.Feature.WriteNullStringAsEmpty);
            
            return truncateIfNecessary(serialized, context.getMaxLength());
        } catch (Exception e) {
            return "[SERIALIZATION_FAILED: " + e.getMessage() + "]";
        }
    }
    
    @Override
    public String formatHttpParameters(Map<String, String[]> parameters, FormatterContext context) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                
                sb.append("\"").append(entry.getKey()).append("\":");
                
                String[] values = entry.getValue();
                if (values.length == 1) {
                    sb.append("\"").append(values[0]).append("\"");
                } else {
                    sb.append("[");
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append("\"").append(values[i]).append("\"");
                    }
                    sb.append("]");
                }
            }
            
            sb.append("}");
            return truncateIfNecessary(sb.toString(), context.getMaxLength());
        } catch (Exception e) {
            return "[FORMATTING_FAILED: " + e.getMessage() + "]";
        }
    }
    
    @Override
    public String getName() {
        return "json";
    }
    
    private String truncateIfNecessary(String str, int maxLength) {
        if (maxLength > 0 && str.length() > maxLength) {
            return str.substring(0, maxLength) + "[TRUNCATED]";
        }
        return str;
    }
}