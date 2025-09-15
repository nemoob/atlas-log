package io.github.nemoob.atlas.log.serializer;

/**
 * 参数格式配置
 * 
 * @author nemoob
 * @since 0.2.0
 */
public class ArgumentFormatConfig {
    
    /**
     * 参数输出格式类型
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
    
    public ArgumentFormatConfig() {
    }
    
    public ArgumentFormatConfig(ArgumentFormatType type, String separator, String keyValueSeparator, boolean includeParameterIndex) {
        this.type = type;
        this.separator = separator;
        this.keyValueSeparator = keyValueSeparator;
        this.includeParameterIndex = includeParameterIndex;
    }
    
    public ArgumentFormatType getType() {
        return type;
    }
    
    public void setType(ArgumentFormatType type) {
        this.type = type;
    }
    
    public String getSeparator() {
        return separator;
    }
    
    public void setSeparator(String separator) {
        this.separator = separator;
    }
    
    public String getKeyValueSeparator() {
        return keyValueSeparator;
    }
    
    public void setKeyValueSeparator(String keyValueSeparator) {
        this.keyValueSeparator = keyValueSeparator;
    }
    
    public boolean isIncludeParameterIndex() {
        return includeParameterIndex;
    }
    
    public void setIncludeParameterIndex(boolean includeParameterIndex) {
        this.includeParameterIndex = includeParameterIndex;
    }
}