package io.github.nemoob.atlas.log.samples.controller;

import io.github.nemoob.atlas.log.annotation.JsonPathCompare;
import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.annotation.LogLevel;
import io.github.nemoob.atlas.log.samples.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * JsonPath 值比较功能演示控制器
 * 展示如何使用 @JsonPathCompare 注解进行值提取和比较
 * 
 * @author nemoob
 * @since 0.2.0
 */
@RestController
@RequestMapping("/api/jsonpath-compare")
@Slf4j
public class JsonPathCompareController {
    
    /**
     * 演示参数 vs 返回值比较
     */
    @PostMapping("/args-vs-result")
    @Log(value = "参数与返回值比较演示", logArgs = true, logResult = true)
    @JsonPathCompare({
        "$.id",           // 比较ID
        "$.username",     // 比较用户名
        "$.email",        // 比较邮箱
        "$.status"        // 比较状态
    })
    public Map<String, Object> compareArgsVsResult(@RequestBody User user) {
        // 模拟业务处理，修改一些字段
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername().toUpperCase()); // 用户名转大写
        result.put("email", user.getEmail());
        result.put("status", "PROCESSED"); // 状态改变
        result.put("processTime", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 演示嵌套对象比较
     */
    @PostMapping("/nested-comparison")
    @Log(value = "嵌套对象比较演示", logArgs = true, logResult = true)
    @JsonPathCompare({
        "$.user.id",              // 嵌套用户ID
        "$.user.profile.email",   // 深层嵌套邮箱
        "$.order.amount",         // 订单金额
        "$.order.status",         // 订单状态
        "$..payment.method"       // 任意层级的支付方式
    })
    public Map<String, Object> compareNestedObjects(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        // 复制用户信息
        if (request.containsKey("user")) {
            Map<String, Object> user = (Map<String, Object>) request.get("user");
            result.put("user", new HashMap<>(user));
        }
        
        // 处理订单信息
        if (request.containsKey("order")) {
            Map<String, Object> order = new HashMap<>((Map<String, Object>) request.get("order"));
            order.put("status", "CONFIRMED"); // 状态变更
            order.put("confirmTime", System.currentTimeMillis());
            result.put("order", order);
        }
        
        // 添加处理结果
        result.put("processed", true);
        result.put("processId", "PROC-" + System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 演示数组元素比较
     */
    @PostMapping("/array-comparison")
    @Log(value = "数组元素比较演示", logArgs = true, logResult = true)
    @JsonPathCompare({
        "$.items[0].name",        // 第一个商品名称
        "$.items[*].price",       // 所有商品价格
        "$.items[*].quantity",    // 所有商品数量
        "$.totalAmount"           // 总金额
    })
    public Map<String, Object> compareArrayElements(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>(request);
        
        // 计算总金额
        if (request.containsKey("items")) {
            double totalAmount = 0.0;
            for (Object item : (java.util.List<?>) request.get("items")) {
                Map<String, Object> itemMap = (Map<String, Object>) item;
                double price = ((Number) itemMap.get("price")).doubleValue();
                int quantity = ((Number) itemMap.get("quantity")).intValue();
                totalAmount += price * quantity;
            }
            result.put("totalAmount", totalAmount);
        }
        
        result.put("calculated", true);
        return result;
    }
    
    /**
     * 演示条件过滤比较
     */
    @PostMapping("/conditional-comparison")
    @Log(value = "条件过滤比较演示", logArgs = true, logResult = true)
    @JsonPathCompare({
        "$.users[?(@.age > 18)].username",       // 成年用户姓名
        "$.orders[?(@.amount > 100)].id",        // 大额订单ID
        "$.products[?(@.inStock == true)].username"  // 有库存商品名称
    })
    public Map<String, Object> compareWithConditions(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>(request);
        
        // 添加处理标记
        result.put("filtered", true);
        result.put("filterTime", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 演示仅提取模式
     */
    @PostMapping("/extract-only")
    @Log(value = "值提取演示", logArgs = true, logResult = true)
    @JsonPathCompare(
        value = {
            "$.userId",
            "$.operation",
            "$.timestamp",
            "$..sensitive.data"
        },
        mode = JsonPathCompare.CompareMode.EXTRACT_ONLY,
        logLevel = LogLevel.DEBUG
    )
    public Map<String, Object> extractValuesOnly(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", request.get("userId"));
        result.put("operation", "EXTRACT_DEMO");
        result.put("timestamp", System.currentTimeMillis());
        result.put("success", true);
        
        return result;
    }
    
    /**
     * 演示自定义消息模板
     */
    @PostMapping("/custom-message")
    @Log(value = "自定义消息演示", logArgs = true, logResult = true)
    @JsonPathCompare(
        value = {
            "$.productId",
            "$.price",
            "$.discount"
        },
        messageTemplate = "🔍 路径: {path} | 输入: {value1} | 输出: {value2} | 匹配: {equal} | 方法: {method}",
        logLevel = LogLevel.INFO
    )
    public Map<String, Object> customMessageTemplate(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("productId", request.get("productId"));
        
        // 计算折扣后价格
        if (request.containsKey("price") && request.containsKey("discount")) {
            double price = ((Number) request.get("price")).doubleValue();
            double discount = ((Number) request.get("discount")).doubleValue();
            result.put("price", price * (1 - discount / 100));
            result.put("discount", discount);
        }
        
        result.put("calculated", true);
        return result;
    }
    
    /**
     * 演示错误处理
     */
    @PostMapping("/error-handling")
    @Log(value = "错误处理演示", logArgs = true, logResult = true)
    @JsonPathCompare(
        value = {
            "$.validPath",
            "$.invalidPath.nonExistent",  // 不存在的路径
            "$.anotherValidPath"
        },
        onFailure = JsonPathCompare.FailureStrategy.LOG_WARNING
    )
    public Map<String, Object> errorHandlingDemo(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("validPath", "This path exists");
        result.put("anotherValidPath", "This also exists");
        result.put("processed", true);
        
        return result;
    }
    
    /**
     * 获取功能说明
     */
    @GetMapping("/info")
    @Log(value = "获取JsonPath比较功能信息", logResult = true)
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("description", "JsonPath 值提取和比较功能演示");
        info.put("features", new String[]{
            "参数与返回值比较",
            "嵌套对象字段比较",
            "数组元素比较",
            "条件过滤比较",
            "值提取模式",
            "自定义消息模板",
            "错误处理策略"
        });
        
        info.put("supportedPaths", new String[]{
            "$.field - 根级字段",
            "$.object.field - 嵌套字段",
            "$.array[0] - 数组元素",
            "$.array[*] - 所有数组元素",
            "$..field - 递归搜索字段",
            "$[?(@.condition)] - 条件过滤"
        });
        
        return info;
    }
}