package io.github.nemoob.atlas.log.serializer;

import java.util.Map;

/**
 * 参数格式化器接口
 * 允许用户自定义参数和返回值的格式化逻辑
 * 
 * @author nemoob
 * @since 0.2.0
 */
public interface ArgumentFormatter {
    
    /**
     * 格式化方法参数
     * 
     * @param args 方法参数数组
     * @param context 格式化上下文
     * @return 格式化后的字符串
     */
    String formatArguments(Object[] args, FormatterContext context);
    
    /**
     * 格式化方法返回值
     * 
     * @param result 方法返回值
     * @param context 格式化上下文
     * @return 格式化后的字符串
     */
    String formatResult(Object result, FormatterContext context);
    
    /**
     * 格式化HTTP请求参数
     * 
     * @param parameters HTTP请求参数映射
     * @param context 格式化上下文
     * @return 格式化后的字符串
     */
    String formatHttpParameters(Map<String, String[]> parameters, FormatterContext context);
    
    /**
     * 获取格式化器名称
     * 
     * @return 格式化器名称
     */
    String getName();
    
    /**
     * 格式化上下文
     * 提供格式化过程中需要的上下文信息
     */
    interface FormatterContext {
        
        /**
         * 获取方法名称
         */
        String getMethodName();
        
        /**
         * 获取类名称
         */
        String getClassName();
        
        /**
         * 获取最大长度限制
         */
        int getMaxLength();
        
        /**
         * 获取自定义属性
         */
        Object getAttribute(String key);
        
        /**
         * 设置自定义属性
         */
        void setAttribute(String key, Object value);
        
        /**
         * 获取所有自定义属性
         */
        Map<String, Object> getAttributes();
    }
}