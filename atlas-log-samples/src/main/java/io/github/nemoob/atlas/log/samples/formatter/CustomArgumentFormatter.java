package io.github.nemoob.atlas.log.samples.formatter;

import io.github.nemoob.atlas.log.serializer.ArgumentFormatter;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 自定义参数格式化器示例
 * 演示如何实现自定义的参数格式化逻辑
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Component
public class CustomArgumentFormatter implements ArgumentFormatter {
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public String formatArguments(Object[] args, FormatterContext context) {
        if (args == null || args.length == 0) {
            return "[无参数]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[方法参数] ");
        sb.append(context.getMethodName()).append("(");
        
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            
            Object arg = args[i];
            sb.append("参数").append(i + 1).append("=");
            
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                sb.append("'").append(arg).append("'");
            } else if (arg instanceof Number) {
                sb.append("数值[").append(arg).append("]");
            } else if (arg instanceof Date) {
                sb.append("时间[").append(dateFormat.format((Date) arg)).append("]");
            } else if (arg instanceof Boolean) {
                sb.append("布尔[").append(arg).append("]");
            } else {
                sb.append("对象[").append(arg.getClass().getSimpleName()).append("@")
                  .append(Integer.toHexString(arg.hashCode())).append("]");
            }
        }
        
        sb.append(")");
        
        String result = sb.toString();
        return truncateIfNecessary(result, context.getMaxLength());
    }
    
    @Override
    public String formatResult(Object result, FormatterContext context) {
        if (result == null) {
            return "[返回值] null";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[返回值] ");
        
        if (result instanceof String) {
            sb.append("字符串: '").append(result).append("'");
        } else if (result instanceof Number) {
            sb.append("数值: ").append(result);
        } else if (result instanceof Date) {
            sb.append("时间: ").append(dateFormat.format((Date) result));
        } else if (result instanceof Boolean) {
            sb.append("布尔: ").append(result);
        } else if (result instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) result;
            sb.append("映射[大小=").append(map.size()).append("] {");
            
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count > 0) sb.append(", ");
                if (count >= 3) {
                    sb.append("...");
                    break;
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                count++;
            }
            sb.append("}");
        } else {
            sb.append("对象[").append(result.getClass().getSimpleName())
              .append("@").append(Integer.toHexString(result.hashCode())).append("]");
        }
        
        String formatted = sb.toString();
        return truncateIfNecessary(formatted, context.getMaxLength());
    }
    
    @Override
    public String formatHttpParameters(Map<String, String[]> parameters, FormatterContext context) {
        if (parameters == null || parameters.isEmpty()) {
            return "[HTTP参数] 无";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[HTTP参数] ");
        
        int count = 0;
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            if (count > 0) {
                sb.append(" | ");
            }
            
            String key = entry.getKey();
            String[] values = entry.getValue();
            
            sb.append(key).append("=");
            
            if (values.length == 1) {
                sb.append("'").append(values[0]).append("'");
            } else {
                sb.append("[");
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append("'").append(values[i]).append("'");
                }
                sb.append("]");
            }
            
            count++;
        }
        
        String result = sb.toString();
        return truncateIfNecessary(result, context.getMaxLength());
    }
    
    @Override
    public String getName() {
        return "custom";
    }
    
    private String truncateIfNecessary(String str, int maxLength) {
        if (maxLength > 0 && str.length() > maxLength) {
            return str.substring(0, maxLength) + "[已截断]";
        }
        return str;
    }
}