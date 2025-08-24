package io.github.nemoob.atlas.log.serializer;

import io.github.nemoob.atlas.log.annotation.Log;

/**
 * 参数序列化器接口
 * 用于将方法参数和返回值序列化为字符串
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
public interface ArgumentSerializer {
    
    /**
     * 序列化方法参数
     * 
     * @param args 方法参数数组
     * @param annotation Log注解
     * @return 序列化后的字符串
     */
    String serializeArgs(Object[] args, Log annotation);
    
    /**
     * 序列化方法返回值
     * 
     * @param result 方法返回值
     * @param annotation Log注解
     * @return 序列化后的字符串
     */
    String serializeResult(Object result, Log annotation);
    
    /**
     * 序列化单个对象
     * 
     * @param obj 要序列化的对象
     * @param maxLength 最大长度限制
     * @return 序列化后的字符串
     */
    String serialize(Object obj, int maxLength);
}