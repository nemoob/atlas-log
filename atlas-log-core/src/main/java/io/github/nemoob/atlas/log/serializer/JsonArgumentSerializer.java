package io.github.nemoob.atlas.log.serializer;

import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.annotation.LogIgnore;
import io.github.nemoob.atlas.log.exception.SerializationException;
import io.github.nemoob.atlas.log.util.ReflectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于Jackson的JSON参数序列化器
 * 支持敏感数据脱敏和循环引用处理
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Slf4j
public class JsonArgumentSerializer implements ArgumentSerializer {
    
    private final ObjectMapper objectMapper;
    private final SensitiveDataMasker sensitiveDataMasker;
    
    public JsonArgumentSerializer(ObjectMapper objectMapper, SensitiveDataMasker sensitiveDataMasker) {
        this.objectMapper = objectMapper;
        this.sensitiveDataMasker = sensitiveDataMasker;
    }
    
    @Override
    public String serializeArgs(Object[] args, Log annotation) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            List<Object> filteredArgs = filterArguments(args, annotation);
            String result = objectMapper.writeValueAsString(filteredArgs);
            return truncateIfNecessary(result, annotation.maxArgLength());
        } catch (JsonProcessingException e) {
            log.warn("Parameter serialization failed", e);
            throw new SerializationException(args.getClass(), "JSON序列化失败", e);
        }
    }
    
    @Override
    public String serializeResult(Object result, Log annotation) {
        if (result == null) {
            return "null";
        }
        
        try {
            Object maskedResult = sensitiveDataMasker.maskSensitiveData(result);
            String serialized = objectMapper.writeValueAsString(maskedResult);
            return truncateIfNecessary(serialized, annotation.maxResultLength());
        } catch (JsonProcessingException e) {
            log.warn("Return value serialization failed: {}", result.getClass().getSimpleName(), e);
            throw new SerializationException(result.getClass(), "JSON序列化失败", e);
        }
    }
    
    @Override
    public String serialize(Object obj, int maxLength) {
        if (obj == null) {
            return "null";
        }
        
        try {
            Object maskedObj = sensitiveDataMasker.maskSensitiveData(obj);
            String result = objectMapper.writeValueAsString(maskedObj);
            return truncateIfNecessary(result, maxLength);
        } catch (JsonProcessingException e) {
            log.warn("Object serialization failed: {}", obj.getClass().getSimpleName(), e);
            return "[序列化失败: " + obj.getClass().getSimpleName() + "]";
        }
    }
    
    /**
     * 过滤参数，排除被@LogIgnore标记的参数和excludeArgs指定的参数
     */
    private List<Object> filterArguments(Object[] args, Log annotation) {
        List<Object> filteredArgs = new ArrayList<>();
        Set<Integer> excludeIndexes = Arrays.stream(annotation.excludeArgs())
                .boxed()
                .collect(Collectors.toSet());
        
        for (int i = 0; i < args.length; i++) {
            if (excludeIndexes.contains(i)) {
                filteredArgs.add("[excluded]");
                continue;
            }
            
            // 检查参数是否标记了@LogIgnore
            if (isParameterIgnored(args, i)) {
                filteredArgs.add("[ignored]");
                continue;
            }
            
            Object maskedArg = sensitiveDataMasker.maskSensitiveData(args[i]);
            filteredArgs.add(maskedArg);
        }
        
        return filteredArgs;
    }
    
    /**
     * 检查参数是否被@LogIgnore标记
     * 注意：这里需要通过反射获取方法参数的注解信息
     * 在实际的AOP切面中，会有方法签名信息可以使用
     */
    private boolean isParameterIgnored(Object[] args, int index) {
        // 这里简化处理，实际实现中会在AOP切面中获取参数注解信息
        // 并传递给序列化器
        return false;
    }
    
    /**
     * 根据最大长度截断字符串
     */
    private String truncateIfNecessary(String str, int maxLength) {
        if (maxLength <= 0 || str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 检查参数注解信息
     * 这个方法会在AOP切面中被调用，传入方法的参数注解信息
     */
    public String serializeArgsWithParameterInfo(Object[] args, Log annotation, Parameter[] parameters) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            List<Object> filteredArgs = new ArrayList<>();
            Set<Integer> excludeIndexes = Arrays.stream(annotation.excludeArgs())
                    .boxed()
                    .collect(Collectors.toSet());
            
            for (int i = 0; i < args.length; i++) {
                if (excludeIndexes.contains(i)) {
                    filteredArgs.add("[excluded]");
                    continue;
                }
                
                // 检查参数注解
                if (i < parameters.length && hasLogIgnoreAnnotation(parameters[i])) {
                    filteredArgs.add("[ignored:" + getIgnoreReason(parameters[i]) + "]");
                    continue;
                }
                
                Object maskedArg = sensitiveDataMasker.maskSensitiveData(args[i]);
                filteredArgs.add(maskedArg);
            }
            
            String result = objectMapper.writeValueAsString(filteredArgs);
            return truncateIfNecessary(result, annotation.maxArgLength());
        } catch (JsonProcessingException e) {
            log.warn("Parameter serialization failed", e);
            throw new SerializationException(args.getClass(), "JSON序列化失败", e);
        }
    }
    
    /**
     * 检查参数是否有@LogIgnore注解
     */
    private boolean hasLogIgnoreAnnotation(Parameter parameter) {
        return parameter.isAnnotationPresent(LogIgnore.class);
    }
    
    /**
     * 获取@LogIgnore注解的原因
     */
    private String getIgnoreReason(Parameter parameter) {
        LogIgnore logIgnore = parameter.getAnnotation(LogIgnore.class);
        return logIgnore != null ? logIgnore.reason() : "sensitive data";
    }
}