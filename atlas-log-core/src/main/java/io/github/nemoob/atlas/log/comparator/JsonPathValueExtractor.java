package io.github.nemoob.atlas.log.comparator;

import com.alibaba.fastjson2.JSON;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JsonPath 值提取器
 * 用于从对象中提取指定路径的值，并进行比较
 * 
 * 核心功能：
 * 1. 使用 JsonPath 从对象中提取指定路径的值
 * 2. 支持多个路径的批量提取
 * 3. 提供值比较功能
 * 4. 处理提取失败的情况
 * 
 * @author nemoob
 * @since 0.2.0
 */
@Slf4j
public class JsonPathValueExtractor {
    
    private final boolean enabled;
    
    public JsonPathValueExtractor(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 从对象中提取单个路径的值
     * 
     * @param obj 源对象
     * @param jsonPath JsonPath 表达式
     * @return 提取的值，如果提取失败返回 null
     */
    public Object extractValue(Object obj, String jsonPath) {
        if (!enabled || obj == null || jsonPath == null || jsonPath.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 将对象序列化为 JSON
            String json = JSON.toJSONString(obj);
            
            // 使用 JsonPath 提取值
            DocumentContext context = JsonPath.parse(json);
            return context.read(jsonPath);
        } catch (PathNotFoundException e) {
            log.debug("Path not found: {} in object: {}", jsonPath, obj.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.debug("Failed to extract value from path: {} in object: {}", jsonPath, obj.getClass().getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 从对象中提取多个路径的值
     * 
     * @param obj 源对象
     * @param jsonPaths JsonPath 表达式列表
     * @return 路径到值的映射
     */
    public Map<String, Object> extractValues(Object obj, List<String> jsonPaths) {
        Map<String, Object> result = new HashMap<>();
        
        if (!enabled || obj == null || jsonPaths == null || jsonPaths.isEmpty()) {
            return result;
        }
        
        try {
            // 将对象序列化为 JSON
            String json = JSON.toJSONString(obj);
            DocumentContext context = JsonPath.parse(json);
            
            // 批量提取值
            for (String jsonPath : jsonPaths) {
                if (jsonPath != null && !jsonPath.trim().isEmpty()) {
                    try {
                        Object value = context.read(jsonPath);
                        result.put(jsonPath, value);
                    } catch (PathNotFoundException e) {
                        log.debug("Path not found: {}", jsonPath);
                        result.put(jsonPath, null);
                    } catch (Exception e) {
                        log.debug("Failed to extract value from path: {}", jsonPath, e);
                        result.put(jsonPath, null);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to serialize object for value extraction: {}", obj.getClass().getSimpleName(), e);
        }
        
        return result;
    }
    
    /**
     * 比较两个对象在指定路径上的值
     * 
     * @param obj1 第一个对象
     * @param obj2 第二个对象
     * @param jsonPath JsonPath 表达式
     * @return 比较结果
     */
    public ValueComparisonResult compareValues(Object obj1, Object obj2, String jsonPath) {
        Object value1 = extractValue(obj1, jsonPath);
        Object value2 = extractValue(obj2, jsonPath);
        
        return new ValueComparisonResult(jsonPath, value1, value2, Objects.equals(value1, value2));
    }
    
    /**
     * 比较两个对象在多个路径上的值
     * 
     * @param obj1 第一个对象
     * @param obj2 第二个对象
     * @param jsonPaths JsonPath 表达式列表
     * @return 比较结果列表
     */
    public List<ValueComparisonResult> compareValues(Object obj1, Object obj2, List<String> jsonPaths) {
        List<ValueComparisonResult> results = new ArrayList<>();
        
        if (!enabled || jsonPaths == null || jsonPaths.isEmpty()) {
            return results;
        }
        
        Map<String, Object> values1 = extractValues(obj1, jsonPaths);
        Map<String, Object> values2 = extractValues(obj2, jsonPaths);
        
        for (String jsonPath : jsonPaths) {
            Object value1 = values1.get(jsonPath);
            Object value2 = values2.get(jsonPath);
            boolean isEqual = Objects.equals(value1, value2);
            
            results.add(new ValueComparisonResult(jsonPath, value1, value2, isEqual));
        }
        
        return results;
    }
    
    /**
     * 检查对象是否包含指定路径
     * 
     * @param obj 源对象
     * @param jsonPath JsonPath 表达式
     * @return 是否包含该路径
     */
    public boolean hasPath(Object obj, String jsonPath) {
        if (!enabled || obj == null || jsonPath == null || jsonPath.trim().isEmpty()) {
            return false;
        }
        
        try {
            String json = JSON.toJSONString(obj);
            DocumentContext context = JsonPath.parse(json);
            context.read(jsonPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        } catch (Exception e) {
            log.debug("Error checking path existence: {}", jsonPath, e);
            return false;
        }
    }
    
    /**
     * 获取对象中所有可用的路径（简化版本）
     * 
     * @param obj 源对象
     * @return 可用路径列表
     */
    public List<String> getAvailablePaths(Object obj) {
        List<String> paths = new ArrayList<>();
        
        if (!enabled || obj == null) {
            return paths;
        }
        
        try {
            String json = JSON.toJSONString(obj);
            DocumentContext context = JsonPath.parse(json);
            
            // 简单的路径发现（可以根据需要扩展）
            paths.addAll(discoverPaths(context, "$"));
        } catch (Exception e) {
            log.debug("Failed to discover paths in object: {}", obj.getClass().getSimpleName(), e);
        }
        
        return paths;
    }
    
    /**
     * 递归发现路径（简化实现）
     */
    private List<String> discoverPaths(DocumentContext context, String basePath) {
        List<String> paths = new ArrayList<>();
        
        try {
            // 这里可以实现更复杂的路径发现逻辑
            // 目前只返回基础路径
            paths.add(basePath);
        } catch (Exception e) {
            log.debug("Failed to discover paths from base: {}", basePath, e);
        }
        
        return paths;
    }
    
    /**
     * 检查 JsonPath 是否可用
     */
    public static boolean isJsonPathAvailable() {
        try {
            Class.forName("com.jayway.jsonpath.JsonPath");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 值比较结果
     */
    public static class ValueComparisonResult {
        private final String jsonPath;
        private final Object value1;
        private final Object value2;
        private final boolean isEqual;
        
        public ValueComparisonResult(String jsonPath, Object value1, Object value2, boolean isEqual) {
            this.jsonPath = jsonPath;
            this.value1 = value1;
            this.value2 = value2;
            this.isEqual = isEqual;
        }
        
        public String getJsonPath() {
            return jsonPath;
        }
        
        public Object getValue1() {
            return value1;
        }
        
        public Object getValue2() {
            return value2;
        }
        
        public boolean isEqual() {
            return isEqual;
        }
        
        @Override
        public String toString() {
            return String.format("ValueComparisonResult{path='%s', value1=%s, value2=%s, equal=%s}", 
                jsonPath, value1, value2, isEqual);
        }
    }
}