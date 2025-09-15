package io.github.nemoob.atlas.log.samples.controller;

import io.github.nemoob.atlas.log.annotation.Log;
import io.github.nemoob.atlas.log.samples.model.User;
import io.github.nemoob.atlas.log.serializer.ArgumentFormatterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义格式化器演示控制器
 * 演示如何使用自定义格式化器来格式化参数和返回值
 * 
 * 配置示例：
 * # 使用自定义格式化器
 * atlas.log.argument-format.type=CUSTOM
 * atlas.log.argument-format.custom-formatter-name=custom
 * 
 * @author nemoob
 * @since 0.2.0
 */
@RestController
@RequestMapping("/api/custom-formatter")
public class CustomFormatterController {
    
    @Autowired
    private ArgumentFormatterManager formatterManager;
    
    /**
     * 基本类型参数演示
     * 
     * 自定义格式输出: [方法参数] basicTypes(参数1='张三', 参数2=数值[25], 参数3=布尔[true])
     */
    @PostMapping("/basic")
    @Log(value = "基本类型参数演示", logArgs = true)
    public Map<String, Object> basicTypes(@RequestParam String name, 
                                          @RequestParam Integer age, 
                                          @RequestParam Boolean active) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("age", age);
        result.put("active", active);
        result.put("timestamp", new Date());
        return result;
    }
    
    /**
     * 复杂对象参数演示
     * 
     * 自定义格式输出: [方法参数] complexObject(参数1=对象[User@abc123], 参数2='UPDATE')
     */
    @PostMapping("/complex")
    @Log(value = "复杂对象参数演示", logArgs = true, logResult = true)
    public Map<String, Object> complexObject(@RequestBody User user, 
                                            @RequestParam String operation) {
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("operation", operation);
        result.put("success", true);
        result.put("timestamp", new Date());
        return result;
    }
    
    /**
     * 时间参数演示
     * 
     * 自定义格式输出: [方法参数] dateExample(参数1=时间[2024-01-01 12:00:00], 参数2='查询')
     */
    @GetMapping("/date")
    @Log(value = "时间参数演示", logArgs = true, logResult = true)
    public Map<String, Object> dateExample(@RequestParam Date startTime, 
                                          @RequestParam String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("startTime", startTime);
        result.put("type", type);
        result.put("endTime", new Date());
        result.put("duration", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 空值和特殊值演示
     * 
     * 自定义格式输出: [方法参数] specialValues(参数1=null, 参数2='', 参数3=数值[0])
     */
    @PostMapping("/special-values")
    @Log(value = "空值和特殊值演示", logArgs = true)
    public Map<String, Object> specialValues(@RequestParam(required = false) String nullValue,
                                            @RequestParam(defaultValue = "") String emptyValue,
                                            @RequestParam(defaultValue = "0") Integer zeroValue) {
        Map<String, Object> result = new HashMap<>();
        result.put("nullValue", nullValue);
        result.put("emptyValue", emptyValue);
        result.put("zeroValue", zeroValue);
        return result;
    }
    
    /**
     * 手动使用格式化器演示
     */
    @GetMapping("/manual-format")
    @Log("手动格式化演示")
    public Map<String, Object> manualFormat(@RequestParam String data) {
        // 手动使用不同的格式化器
        Object[] args = {data, 123, true};
        
        Map<String, Object> result = new HashMap<>();
        
        // 使用 JSON 格式化器
        String jsonFormat = formatterManager.formatArguments("json", args, 
            new io.github.nemoob.atlas.log.serializer.DefaultFormatterContext(
                "manualFormat", "CustomFormatterController", 1000));
        
        // 使用 key-value 格式化器
        String keyValueFormat = formatterManager.formatArguments("key-value", args,
            new io.github.nemoob.atlas.log.serializer.DefaultFormatterContext(
                "manualFormat", "CustomFormatterController", 1000));
        
        // 使用自定义格式化器
        String customFormat = formatterManager.formatArguments("custom", args,
            new io.github.nemoob.atlas.log.serializer.DefaultFormatterContext(
                "manualFormat", "CustomFormatterController", 1000));
        
        result.put("input", data);
        result.put("jsonFormat", jsonFormat);
        result.put("keyValueFormat", keyValueFormat);
        result.put("customFormat", customFormat);
        result.put("availableFormatters", formatterManager.getFormatterNames());
        
        return result;
    }
    
    /**
     * 获取可用的格式化器列表
     */
    @GetMapping("/formatters")
    public Map<String, Object> getFormatters() {
        Map<String, Object> result = new HashMap<>();
        result.put("availableFormatters", formatterManager.getFormatterNames());
        result.put("defaultFormatter", formatterManager.getDefaultFormatterName());
        result.put("description", "可用的参数格式化器列表");
        return result;
    }
}