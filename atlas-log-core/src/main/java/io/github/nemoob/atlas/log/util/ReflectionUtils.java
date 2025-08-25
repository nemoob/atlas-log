package io.github.nemoob.atlas.log.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射工具类
 * 提供常用的反射操作方法
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
@Slf4j
public class ReflectionUtils {
    
    /**
     * 方法参数缓存
     */
    private static final ConcurrentHashMap<Method, Parameter[]> PARAMETER_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 字段缓存
     */
    private static final ConcurrentHashMap<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 获取方法的参数信息（带缓存）
     * 
     * @param method 方法对象
     * @return 参数数组
     */
    public static Parameter[] getParameters(Method method) {
        return PARAMETER_CACHE.computeIfAbsent(method, Method::getParameters);
    }
    
    /**
     * 获取类的所有字段（包括父类，带缓存）
     * 
     * @param clazz 类对象
     * @return 字段数组
     */
    public static Field[] getAllFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, ReflectionUtils::doGetAllFields);
    }
    
    /**
     * 实际获取所有字段的方法
     */
    private static Field[] doGetAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                fields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return fields.toArray(new Field[0]);
    }
    
    /**
     * 检查参数是否有指定注解
     * 
     * @param parameter 参数对象
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public static boolean hasAnnotation(Parameter parameter, Class<? extends Annotation> annotationClass) {
        return parameter.isAnnotationPresent(annotationClass);
    }
    
    /**
     * 获取参数的指定注解
     * 
     * @param parameter 参数对象
     * @param annotationClass 注解类
     * @return 注解对象，如果不存在返回null
     */
    public static <T extends Annotation> T getAnnotation(Parameter parameter, Class<T> annotationClass) {
        return parameter.getAnnotation(annotationClass);
    }
    
    /**
     * 检查方法是否有指定注解
     * 
     * @param method 方法对象
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public static boolean hasAnnotation(Method method, Class<? extends Annotation> annotationClass) {
        return method.isAnnotationPresent(annotationClass);
    }
    
    /**
     * 获取方法的指定注解
     * 
     * @param method 方法对象
     * @param annotationClass 注解类
     * @return 注解对象，如果不存在返回null
     */
    public static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        return method.getAnnotation(annotationClass);
    }
    
    /**
     * 检查类是否有指定注解
     * 
     * @param clazz 类对象
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return clazz.isAnnotationPresent(annotationClass);
    }
    
    /**
     * 获取类的指定注解
     * 
     * @param clazz 类对象
     * @param annotationClass 注解类
     * @return 注解对象，如果不存在返回null
     */
    public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationClass) {
        return clazz.getAnnotation(annotationClass);
    }
    
    /**
     * 安全地获取字段值
     * 
     * @param field 字段对象
     * @param obj 目标对象
     * @return 字段值，获取失败返回null
     */
    public static Object getFieldValue(Field field, Object obj) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            log.debug("Failed to get field value: {}.{}", obj.getClass().getSimpleName(), field.getName(), e);
            return null;
        }
    }
    
    /**
     * 安全地设置字段值
     * 
     * @param field 字段对象
     * @param obj 目标对象
     * @param value 要设置的值
     * @return 是否设置成功
     */
    public static boolean setFieldValue(Field field, Object obj, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (Exception e) {
            log.debug("Failed to set field value: {}.{}", obj.getClass().getSimpleName(), field.getName(), e);
            return false;
        }
    }
    
    /**
     * 格式化方法签名
     * 
     * @param method 方法对象
     * @return 格式化后的方法签名
     */
    public static String formatMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getSimpleName())
          .append(".")
          .append(method.getName())
          .append("(");
        
        Parameter[] parameters = getParameters(method);
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameters[i].getType().getSimpleName());
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * 清空缓存
     */
    public static void clearCaches() {
        PARAMETER_CACHE.clear();
        FIELD_CACHE.clear();
    }
    
    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        return String.format("ParameterCache: %d, FieldCache: %d", 
                PARAMETER_CACHE.size(), FIELD_CACHE.size());
    }
}