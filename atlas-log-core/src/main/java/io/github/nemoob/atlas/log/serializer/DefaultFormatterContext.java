package io.github.nemoob.atlas.log.serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认格式化上下文实现
 * 
 * @author nemoob
 * @since 0.2.0
 */
public class DefaultFormatterContext implements ArgumentFormatter.FormatterContext {
    
    private final String methodName;
    private final String className;
    private final int maxLength;
    private final Map<String, Object> attributes;
    
    public DefaultFormatterContext(String methodName, String className, int maxLength) {
        this.methodName = methodName;
        this.className = className;
        this.maxLength = maxLength;
        this.attributes = new ConcurrentHashMap<>();
    }
    
    @Override
    public String getMethodName() {
        return methodName;
    }
    
    @Override
    public String getClassName() {
        return className;
    }
    
    @Override
    public int getMaxLength() {
        return maxLength;
    }
    
    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
}