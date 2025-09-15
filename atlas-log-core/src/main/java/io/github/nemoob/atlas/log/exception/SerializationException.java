package io.github.nemoob.atlas.log.exception;

/**
 * 序列化异常
 * 
 * @author nemoob
 * @since 0.2.0
 */
public class SerializationException extends LogException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 序列化的对象类型
     */
    private final Class<?> objectType;
    
    public SerializationException(Class<?> objectType, String message) {
        super("序列化失败: " + (objectType != null ? objectType.getSimpleName() : "unknown") + ", 原因: " + message);
        this.objectType = objectType;
    }
    
    public SerializationException(Class<?> objectType, String message, Throwable cause) {
        super("序列化失败: " + (objectType != null ? objectType.getSimpleName() : "unknown") + ", 原因: " + message, cause);
        this.objectType = objectType;
    }
    
    public SerializationException(Class<?> objectType, Throwable cause) {
        super("序列化失败: " + (objectType != null ? objectType.getSimpleName() : "unknown"), cause);
        this.objectType = objectType;
    }
    
    /**
     * 获取序列化失败的对象类型
     * 
     * @return 对象类型
     */
    public Class<?> getObjectType() {
        return objectType;
    }
}