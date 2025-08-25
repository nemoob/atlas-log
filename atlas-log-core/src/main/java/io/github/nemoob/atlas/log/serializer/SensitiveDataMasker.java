package io.github.nemoob.atlas.log.serializer;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏器
 * 用于在序列化过程中处理敏感信息，如密码、令牌等
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Slf4j
public class SensitiveDataMasker {
    
    /**
     * 敏感字段名模式（忽略大小写）
     */
    private static final Set<String> SENSITIVE_FIELD_NAMES = new HashSet<>(Arrays.asList(
            "password", "passwd", "pwd", "pass",
            "token", "accesstoken", "refreshtoken", "jwt",
            "secret", "secretkey", "privatekey",
            "apikey", "appkey", "authkey",
            "credential", "credentials", "auth",
            "sessionid", "session", "cookie",
            "bankcard", "cardno", "cardnumber",
            "idcard", "idno", "ssn",
            "phone", "mobile", "telephone", "tel",
            "email", "mail"
    ));
    
    /**
     * 敏感字段名正则模式
     */
    private static final List<Pattern> SENSITIVE_PATTERNS = Arrays.asList(
            Pattern.compile(".*password.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*token.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*secret.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*key.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*auth.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*credential.*", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 脱敏标记
     */
    private static final String MASK_VALUE = "***";
    
    /**
     * 反射字段缓存
     */
    private final Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();
    
    /**
     * 自定义敏感字段
     */
    private final Set<String> customSensitiveFields = new HashSet<>();
    
    /**
     * 是否启用脱敏
     */
    private final boolean enabled;
    
    public SensitiveDataMasker(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 添加自定义敏感字段
     */
    public void addSensitiveField(String fieldName) {
        customSensitiveFields.add(fieldName.toLowerCase());
    }
    
    /**
     * 批量添加自定义敏感字段
     */
    public void addSensitiveFields(String... fieldNames) {
        for (String fieldName : fieldNames) {
            addSensitiveField(fieldName);
        }
    }
    
    /**
     * 脱敏处理
     */
    public Object maskSensitiveData(Object obj) {
        if (!enabled || obj == null) {
            return obj;
        }
        
        try {
            return doMask(obj);
        } catch (Exception e) {
            log.warn("Sensitive data masking failed: {}", obj.getClass().getSimpleName(), e);
            return obj; // 脱敏失败时返回原对象
        }
    }
    
    /**
     * 执行脱敏处理
     */
    @SuppressWarnings("unchecked")
    private Object doMask(Object obj) {
        if (obj == null) {
            return null;
        }
        
        Class<?> clazz = obj.getClass();
        
        // 基本类型和包装类型直接返回
        if (isPrimitiveOrWrapper(clazz) || clazz == String.class) {
            return obj;
        }
        
        // 数组处理
        if (clazz.isArray()) {
            return maskArray(obj);
        }
        
        // 集合处理
        if (obj instanceof List) {
            return maskList((List<Object>) obj);
        }
        
        if (obj instanceof Map) {
            return maskMap((Map<String, Object>) obj);
        }
        
        // 普通对象处理
        return maskObject(obj);
    }
    
    /**
     * 处理数组
     */
    private Object maskArray(Object array) {
        if (array instanceof Object[]) {
            Object[] objArray = (Object[]) array;
            Object[] maskedArray = new Object[objArray.length];
            for (int i = 0; i < objArray.length; i++) {
                maskedArray[i] = doMask(objArray[i]);
            }
            return maskedArray;
        }
        return array; // 基本类型数组直接返回
    }
    
    /**
     * 处理List
     */
    private Object maskList(List<Object> list) {
        return list.stream()
                .map(this::doMask)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 处理Map
     */
    private Object maskMap(Map<String, Object> map) {
        Map<String, Object> maskedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (isSensitiveField(key)) {
                maskedMap.put(key, MASK_VALUE);
            } else {
                maskedMap.put(key, doMask(value));
            }
        }
        return maskedMap;
    }
    
    /**
     * 处理普通对象
     */
    private Object maskObject(Object obj) {
        Class<?> clazz = obj.getClass();
        
        // 跳过JDK类和一些常见框架类
        if (shouldSkipClass(clazz)) {
            return obj;
        }
        
        try {
            // 创建一个包含脱敏后字段的Map
            Map<String, Object> result = new HashMap<>();
            Field[] fields = getFields(clazz);
            
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object fieldValue = field.get(obj);
                
                if (isSensitiveField(fieldName)) {
                    result.put(fieldName, MASK_VALUE);
                } else {
                    result.put(fieldName, doMask(fieldValue));
                }
            }
            
            return result;
        } catch (Exception e) {
            log.debug("Cannot mask object: {}", clazz.getSimpleName(), e);
            return obj;
        }
    }
    
    /**
     * 检查是否为敏感字段
     */
    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        
        // 检查自定义敏感字段
        if (customSensitiveFields.contains(lowerFieldName)) {
            return true;
        }
        
        // 检查预定义敏感字段
        if (SENSITIVE_FIELD_NAMES.contains(lowerFieldName)) {
            return true;
        }
        
        // 检查正则模式
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(fieldName).matches()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取类的所有字段（包括父类）
     */
    private Field[] getFields(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, c -> {
            Set<Field> fields = new HashSet<>();
            Class<?> currentClass = c;
            
            while (currentClass != null && currentClass != Object.class) {
                fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                currentClass = currentClass.getSuperclass();
            }
            
            return fields.toArray(new Field[0]);
        });
    }
    
    /**
     * 检查是否为基本类型或包装类型
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz == Boolean.class || clazz == Character.class ||
               clazz == Byte.class || clazz == Short.class ||
               clazz == Integer.class || clazz == Long.class ||
               clazz == Float.class || clazz == Double.class;
    }
    
    /**
     * 检查是否应该跳过该类的处理
     */
    private boolean shouldSkipClass(Class<?> clazz) {
        String className = clazz.getName();
        return className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("sun.") ||
               className.startsWith("com.sun.") ||
               className.startsWith("org.springframework.") ||
               className.startsWith("org.apache.") ||
               className.startsWith("com.fasterxml.jackson.");
    }
}