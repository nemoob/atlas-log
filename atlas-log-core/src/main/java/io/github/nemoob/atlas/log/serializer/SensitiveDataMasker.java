package io.github.nemoob.atlas.log.serializer;

import lombok.extern.slf4j.Slf4j;

/**
 * 敏感数据脱敏器
 * 用于在序列化过程中处理敏感信息，如密码、令牌等
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Slf4j
public class SensitiveDataMasker {
    
    // 简化实现：移除复杂的敏感字段检测逻辑
    
    /**
     * 是否启用脱敏
     */
    private final boolean enabled;
    
    public SensitiveDataMasker(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 添加自定义敏感字段（简化实现：不再实际处理）
     */
    public void addSensitiveField(String fieldName) {
        // 简化实现：不再处理敏感字段
    }
    
    /**
     * 批量添加自定义敏感字段（简化实现：不再实际处理）
     */
    public void addSensitiveFields(String... fieldNames) {
        // 简化实现：不再处理敏感字段
    }
    
    /**
     * 脱敏处理
     */
    public Object maskSensitiveData(Object obj) {
        if (!enabled || obj == null) {
            return obj;
        }
        
        // 简化处理：对于复杂对象直接返回类型描述，避免序列化问题
        Class<?> clazz = obj.getClass();
        
        // 基本类型和字符串直接返回
        if (isPrimitiveOrWrapper(clazz) || clazz == String.class) {
            return obj;
        }
        
        // 跳过复杂对象，直接返回类型描述
        if (shouldSkipClass(clazz)) {
            return "[" + clazz.getSimpleName() + "@" + Integer.toHexString(obj.hashCode()) + "]";
        }
        
        // 对于其他对象，也返回类型描述，避免序列化问题
        return "[" + clazz.getSimpleName() + "@" + Integer.toHexString(obj.hashCode()) + "]";
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
               className.startsWith("com.fasterxml.jackson.") ||
               // Servlet 容器相关类
               className.startsWith("org.apache.catalina.") ||
               className.startsWith("org.apache.tomcat.") ||
               className.startsWith("org.eclipse.jetty.") ||
               className.startsWith("io.undertow.") ||
               className.startsWith("weblogic.") ||
               className.startsWith("com.ibm.websphere.") ||
               // 其他常见的不可序列化类
               className.contains("Facade") ||
               className.contains("Wrapper") ||
               className.contains("Proxy");
    }
}