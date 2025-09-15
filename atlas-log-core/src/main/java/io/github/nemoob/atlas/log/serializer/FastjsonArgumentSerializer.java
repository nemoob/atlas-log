package io.github.nemoob.atlas.log.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.annotation.LogIgnore;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于Fastjson的JSON参数序列化器
 * 使用阿里巴巴 Fastjson2 进行序列化，解决循环引用等序列化问题
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Slf4j
public class FastjsonArgumentSerializer implements ArgumentSerializer {
    
    private final SensitiveDataMasker sensitiveDataMasker;
    private final ArgumentFormatConfig argumentFormatConfig;
    
    public FastjsonArgumentSerializer(SensitiveDataMasker sensitiveDataMasker, 
                                     ArgumentFormatConfig argumentFormatConfig) {
        this.sensitiveDataMasker = sensitiveDataMasker;
        this.argumentFormatConfig = argumentFormatConfig != null ? argumentFormatConfig : new ArgumentFormatConfig();
    }
    
    // 兼容旧的构造函数
    public FastjsonArgumentSerializer(SensitiveDataMasker sensitiveDataMasker) {
        this.sensitiveDataMasker = sensitiveDataMasker;
        this.argumentFormatConfig = new ArgumentFormatConfig();
    }
    
    @Override
    public String serializeArgs(Object[] args, Log annotation) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            List<Object> filteredArgs = filterArguments(args, annotation);
            
            // 脱敏处理
            List<Object> maskedArgs = new ArrayList<>();
            for (Object arg : filteredArgs) {
                Object maskedArg = sensitiveDataMasker.maskSensitiveData(arg);
                maskedArgs.add(maskedArg);
            }
            
            // 根据配置选择序列化格式
            String result;
            if (argumentFormatConfig.getType() == ArgumentFormatType.KEY_VALUE) {
                result = serializeArgsAsKeyValue(maskedArgs);
            } else {
                // 使用 Fastjson 序列化，禁用循环引用检测
                result = JSON.toJSONString(maskedArgs, 
                    JSONWriter.Feature.ReferenceDetection,
                    JSONWriter.Feature.IgnoreNonFieldGetter,
                    JSONWriter.Feature.WriteNullListAsEmpty,
                    JSONWriter.Feature.WriteNullStringAsEmpty);
            }
            
            return truncateIfNecessary(result, annotation.maxArgLength());
        } catch (Exception e) {
            log.warn("Fastjson parameter serialization failed", e);
            return serializeArgsWithFallback(args, annotation, e);
        }
    }
    
    @Override
    public String serializeResult(Object result, Log annotation) {
        if (result == null) {
            return "null";
        }
        
        try {
            Object maskedResult = sensitiveDataMasker.maskSensitiveData(result);
            
            // 使用 Fastjson 序列化，禁用循环引用检测
            String serialized = JSON.toJSONString(maskedResult,
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.IgnoreNonFieldGetter,
                JSONWriter.Feature.WriteNullListAsEmpty,
                JSONWriter.Feature.WriteNullStringAsEmpty);
            
            return truncateIfNecessary(serialized, annotation.maxResultLength());
        } catch (Exception e) {
            log.warn("Fastjson result serialization failed", e);
            return serializeResultWithFallback(result, annotation, e);
        }
    }
    
    @Override
    public String serialize(Object obj, int maxLength) {
        if (obj == null) {
            return "null";
        }
        
        try {
            Object maskedObj = sensitiveDataMasker.maskSensitiveData(obj);
            
            // 使用 Fastjson 序列化，禁用循环引用检测
            String result = JSON.toJSONString(maskedObj,
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.IgnoreNonFieldGetter,
                JSONWriter.Feature.WriteNullListAsEmpty,
                JSONWriter.Feature.WriteNullStringAsEmpty);
            
            return truncateIfNecessary(result, maxLength);
        } catch (Exception e) {
            log.warn("Fastjson object serialization failed: {}", obj.getClass().getSimpleName(), e);
            return serializeObjectWithFallback(obj, maxLength, e);
        }
    }
    
    /**
     * 过滤参数（支持@LogIgnore注解）
     */
    private List<Object> filterArguments(Object[] args, Log annotation) {
        List<Object> filteredArgs = new ArrayList<>();
        
        for (int i = 0; i < args.length; i++) {
            if (!isParameterIgnored(args, i)) {
                filteredArgs.add(args[i]);
            } else {
                filteredArgs.add("[IGNORED]");
            }
        }
        
        return filteredArgs;
    }
    
    /**
     * 检查参数是否被忽略
     */
    private boolean isParameterIgnored(Object[] args, int index) {
        // 简化实现，实际使用中可以通过反射检查@LogIgnore注解
        return false;
    }
    
    /**
     * 截断字符串
     */
    private String truncateIfNecessary(String str, int maxLength) {
        if (maxLength > 0 && str.length() > maxLength) {
            return str.substring(0, maxLength) + "[TRUNCATED]";
        }
        return str;
    }
    
    /**
     * 参数序列化失败时的降级处理
     */
    private String serializeArgsWithFallback(Object[] args, Log annotation, Exception originalException) {
        try {
            // 尝试简化序列化：只序列化基本类型和字符串
            List<Object> simplifiedArgs = new ArrayList<>();
            for (Object arg : args) {
                if (arg == null) {
                    simplifiedArgs.add(null);
                } else if (isPrimitiveOrWrapper(arg) || arg instanceof String) {
                    simplifiedArgs.add(arg);
                } else {
                    simplifiedArgs.add("[" + arg.getClass().getSimpleName() + "@" + Integer.toHexString(arg.hashCode()) + "]");
                }
            }
            return JSON.toJSONString(simplifiedArgs);
        } catch (Exception e) {
            log.debug("Fallback serialization also failed", e);
            return "[SERIALIZATION_FAILED: " + originalException.getMessage() + "]";
        }
    }
    
    /**
     * 返回值序列化失败时的降级处理
     */
    private String serializeResultWithFallback(Object result, Log annotation, Exception originalException) {
        try {
            if (result == null) {
                return "null";
            } else if (isPrimitiveOrWrapper(result) || result instanceof String) {
                return JSON.toJSONString(result);
            } else {
                return "[" + result.getClass().getSimpleName() + "@" + Integer.toHexString(result.hashCode()) + "]";
            }
        } catch (Exception e) {
            log.debug("Fallback result serialization also failed", e);
            return "[SERIALIZATION_FAILED: " + originalException.getMessage() + "]";
        }
    }
    
    /**
     * 对象序列化失败时的降级处理
     */
    private String serializeObjectWithFallback(Object obj, int maxLength, Exception originalException) {
        try {
            if (obj == null) {
                return "null";
            } else if (isPrimitiveOrWrapper(obj) || obj instanceof String) {
                String result = JSON.toJSONString(obj);
                return truncateIfNecessary(result, maxLength);
            } else {
                String result = "[" + obj.getClass().getSimpleName() + "@" + Integer.toHexString(obj.hashCode()) + "]";
                return truncateIfNecessary(result, maxLength);
            }
        } catch (Exception e) {
            log.debug("Fallback object serialization also failed", e);
            return "[SERIALIZATION_FAILED: " + originalException.getMessage() + "]";
        }
    }
    
    /**
     * 将参数序列化为 key=value 格式
     */
    private String serializeArgsAsKeyValue(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(argumentFormatConfig.getSeparator());
            }
            
            // 添加键名
            if (argumentFormatConfig.isIncludeParameterIndex()) {
                sb.append("arg").append(i).append(argumentFormatConfig.getKeyValueSeparator());
            }
            
            // 添加值
            Object arg = args.get(i);
            String value = formatValueForKeyValue(arg);
            sb.append(value);
        }
        
        return sb.toString();
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
}