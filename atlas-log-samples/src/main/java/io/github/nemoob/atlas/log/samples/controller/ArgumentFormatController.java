package io.github.nemoob.atlas.log.samples.controller;

import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.samples.model.User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 参数格式演示控制器
 * 演示不同的参数输出格式：JSON 和 key=value
 * 
 * 配置示例：
 * # JSON 格式（默认）
 * atlas.log.argument-format.type=JSON
 * 
 * # key=value 格式
 * atlas.log.argument-format.type=KEY_VALUE
 * atlas.log.argument-format.separator=&
 * atlas.log.argument-format.key-value-separator==
 * atlas.log.argument-format.include-parameter-index=true
 * 
 * @author nemoob
 * @since 0.2.0
 */
@RestController
@RequestMapping("/api/argument-format")
public class ArgumentFormatController {
    
    /**
     * 基本类型参数演示
     * 
     * JSON 格式输出: ["张三", 25, true]
     * key=value 格式输出: arg0=张三&arg1=25&arg2=true
     */
    @PostMapping("/basic")
    @Log("基本类型参数演示")
    public Map<String, Object> basicTypes(@RequestParam String name, 
                                          @RequestParam Integer age, 
                                          @RequestParam Boolean active) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("age", age);
        result.put("active", active);
        return result;
    }
    
    /**
     * 复杂对象参数演示
     * 
     * JSON 格式输出: [{"id":1,"username":"admin","email":"admin@example.com"}, "UPDATE"]
     * key=value 格式输出: arg0={"id":1,"username":"admin","email":"admin@example.com"}&arg1=UPDATE
     */
    @PostMapping("/complex")
    @Log("复杂对象参数演示")
    public Map<String, Object> complexObject(@RequestBody User user, 
                                            @RequestParam String operation) {
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("operation", operation);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 多参数演示
     * 
     * JSON 格式输出: ["search", 10, 1, ["active", "verified"], {"sortBy":"createTime","order":"desc"}]
     * key=value 格式输出: arg0=search&arg1=10&arg2=1&arg3=["active","verified"]&arg4={"sortBy":"createTime","order":"desc"}
     */
    @GetMapping("/multi-params")
    @Log("多参数演示")
    public Map<String, Object> multipleParams(@RequestParam String keyword,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(required = false) String[] tags,
                                             @RequestParam Map<String, String> filters) {
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("pageSize", pageSize);
        result.put("pageNum", pageNum);
        result.put("tags", tags);
        result.put("filters", filters);
        result.put("total", 100);
        return result;
    }
    
    /**
     * 空值和特殊值演示
     * 
     * JSON 格式输出: [null, "", 0, false]
     * key=value 格式输出: arg0=null&arg1=&arg2=0&arg3=false
     */
    @PostMapping("/special-values")
    @Log("空值和特殊值演示")
    public Map<String, Object> specialValues(@RequestParam(required = false) String nullValue,
                                            @RequestParam(defaultValue = "") String emptyValue,
                                            @RequestParam(defaultValue = "0") Integer zeroValue,
                                            @RequestParam(defaultValue = "false") Boolean falseValue) {
        Map<String, Object> result = new HashMap<>();
        result.put("nullValue", nullValue);
        result.put("emptyValue", emptyValue);
        result.put("zeroValue", zeroValue);
        result.put("falseValue", falseValue);
        return result;
    }
    
    /**
     * 嵌套对象演示
     * 
     * JSON 格式输出: [{"user":{"id":1,"username":"admin"},"metadata":{"source":"web","version":"1.0"}}]
     * key=value 格式输出: arg0={"user":{"id":1,"username":"admin"},"metadata":{"source":"web","version":"1.0"}}
     */
    @PostMapping("/nested")
    @Log("嵌套对象演示")
    public Map<String, Object> nestedObject(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        result.put("received", data);
        result.put("processed", true);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 获取当前参数格式配置
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("message", "当前参数格式配置");
        config.put("description", "查看应用日志以了解当前使用的参数格式");
        config.put("formats", new String[]{"JSON", "KEY_VALUE"});
        return config;
    }
}