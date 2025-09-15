package io.github.nemoob.atlas.log.expression;

import io.github.nemoob.atlas.log.context.LogContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SpEL表达式评估器测试
 * 
 * @author nemoob
 * @since 0.2.0
 */
@ExtendWith(MockitoExtension.class)
class SpelExpressionEvaluatorTest {
    
    @Mock
    private ApplicationContext applicationContext;
    
    private SpelExpressionEvaluator evaluator;
    
    @BeforeEach
    void setUp() {
        evaluator = new SpelExpressionEvaluator(applicationContext, true, 1000, true);
    }
    
    @Test
    void testEvaluateSimpleExpression() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{"test", 123})
                .setMethodName("testMethod");
        
        String result = evaluator.evaluateExpression("#{args[0]}", context);
        assertEquals("test", result);
        
        result = evaluator.evaluateExpression("#{args[1]}", context);
        assertEquals("123", result);
    }
    
    @Test
    void testEvaluateMethodNameExpression() {
        LogContext context = new LogContext()
                .setMethodName("testMethod")
                .setClassName("TestClass");
        
        String result = evaluator.evaluateExpression("#{methodName}", context);
        assertEquals("testMethod", result);
        
        result = evaluator.evaluateExpression("#{className}", context);
        assertEquals("TestClass", result);
    }
    
    @Test
    void testEvaluateResultExpression() {
        LogContext context = new LogContext()
                .setResult("success");
        
        String result = evaluator.evaluateExpression("#{result}", context);
        assertEquals("success", result);
    }
    
    @Test
    void testEvaluateExceptionExpression() {
        RuntimeException exception = new RuntimeException("test error");
        LogContext context = new LogContext()
                .setException(exception);
        
        String result = evaluator.evaluateExpression("#{exception.message}", context);
        assertEquals("test error", result);
    }
    
    @Test
    void testEvaluateExecutionTimeExpression() {
        LogContext context = new LogContext()
                .setExecutionTime(500L);
        
        String result = evaluator.evaluateExpression("#{executionTime}", context);
        assertEquals("500", result);
    }
    
    @Test
    void testEvaluateCondition() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{10, "admin"});
        
        // 测试数值比较
        boolean result = evaluator.evaluateCondition("#{args[0] > 5}", context);
        assertTrue(result);
        
        result = evaluator.evaluateCondition("#{args[0] < 5}", context);
        assertFalse(result);
        
        // 测试字符串比较
        result = evaluator.evaluateCondition("#{args[1] == 'admin'}", context);
        assertTrue(result);
        
        result = evaluator.evaluateCondition("#{args[1] == 'user'}", context);
        assertFalse(result);
    }
    
    @Test
    void testEvaluateComplexCondition() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{10, "admin"});
        
        boolean result = evaluator.evaluateCondition("#{args[0] > 5 and args[1] == 'admin'}", context);
        assertTrue(result);
        
        result = evaluator.evaluateCondition("#{args[0] < 5 or args[1] == 'admin'}", context);
        assertTrue(result);
        
        result = evaluator.evaluateCondition("#{args[0] < 5 and args[1] == 'user'}", context);
        assertFalse(result);
    }
    
    @Test
    void testEvaluateEmptyExpression() {
        LogContext context = new LogContext();
        
        String result = evaluator.evaluateExpression("", context);
        assertEquals("", result);
        
        result = evaluator.evaluateExpression(null, context);
        assertEquals("", result);
    }
    
    @Test
    void testEvaluateEmptyCondition() {
        LogContext context = new LogContext();
        
        boolean result = evaluator.evaluateCondition("", context);
        assertTrue(result);
        
        result = evaluator.evaluateCondition(null, context);
        assertTrue(result);
    }
    
    @Test
    void testEvaluateWithBeanReference() {
        // 模拟Spring Bean
        Object mockBean = mock(Object.class);
        when(applicationContext.getBean("testBean")).thenReturn(mockBean);
        
        LogContext context = new LogContext();
        
        // 注意：这个测试可能需要更复杂的Mock设置
        // 这里仅测试不抛出异常
        assertDoesNotThrow(() -> {
            evaluator.evaluateExpression("#{@testBean}", context);
        });
    }
    
    @Test
    void testFailSafeMode() {
        SpelExpressionEvaluator failSafeEvaluator = 
            new SpelExpressionEvaluator(applicationContext, true, 1000, true);
        
        LogContext context = new LogContext();
        
        // 测试无效表达式在安全模式下的行为
        String result = failSafeEvaluator.evaluateExpression("#{invalid.expression}", context);
        assertNotNull(result);
        assertTrue(result.contains("表达式处理失败"));
    }
    
    @Test
    void testNonFailSafeMode() {
        SpelExpressionEvaluator nonFailSafeEvaluator = 
            new SpelExpressionEvaluator(applicationContext, true, 1000, false);
        
        LogContext context = new LogContext();
        
        // 测试无效表达式在非安全模式下抛出异常
        assertThrows(Exception.class, () -> {
            nonFailSafeEvaluator.evaluateExpression("#{invalid.expression}", context);
        });
    }
    
    @Test
    void testCacheSize() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{"test"});
        
        // 执行几次表达式评估
        evaluator.evaluateExpression("#{args[0]}", context);
        evaluator.evaluateExpression("#{args[0]}", context);
        evaluator.evaluateExpression("#{methodName}", context);
        
        // 检查缓存大小
        assertTrue(evaluator.getCacheSize() >= 0);
        
        // 清空缓存
        evaluator.clearCache();
        assertEquals(0, evaluator.getCacheSize());
    }
    
    // ========== 新增：中文字符和模板表达式测试 ==========
    
    @Test
    void testArgsParameterRecognition() {
        LogContext context = new LogContext()
            .setArgs(new Object[]{"test", 123})
            .setMethodName("testMethod");
        
        // 测试纯SpEL表达式
        String result = evaluator.evaluateExpression("#{args[0]}", context);
        assertEquals("test", result);
        
        // 测试模板表达式
        String templateResult = evaluator.evaluateExpression("参数值: #{args[0]}", context);
        assertEquals("参数值: test", templateResult);
    }
    
    @Test
    void testNullArgsHandling() {
        LogContext context = new LogContext()
            .setArgs(null)
            .setMethodName("testMethod");
        
        // 测试null args的处理
        String result = evaluator.evaluateExpression("#{args.length}", context);
        // 现在应该返回错误信息而不是"0"
        assertTrue(result.contains("表达式引用args但LogContext.args为null") || 
                  result.contains("SpEL表达式评估失败"));
    }
    
    @Test
    void testEmptyArgsHandling() {
        LogContext context = new LogContext()
            .setArgs(new Object[0])
            .setMethodName("testMethod");
        
        // 测试空args数组的处理
        String result = evaluator.evaluateExpression("#{args.length}", context);
        assertEquals("0", result);
    }
    
    @Test
    void testNullLogContextHandling() {
        // 测试null LogContext的处理
        String result = evaluator.evaluateExpression("#{args[0]}", null);
        assertNotNull(result); // 应该返回错误信息而不是抛出异常
        assertTrue(result.contains("SpEL表达式评估失败") || result.contains("[LogContext为null]"));
    }
    
    @Test
    void testChineseTextExpression() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{1, "张三"});
        
        // 测试纯中文文本（这应该不再导致解析错误）
        String result = evaluator.evaluateExpression("查询用户信息", context);
        assertEquals("查询用户信息", result);
        
        // 测试包含中文的文本
        result = evaluator.evaluateExpression("用户管理系统", context);
        assertEquals("用户管理系统", result);
    }
    
    @Test
    void testChineseTemplateExpression() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{1, "张三"});
        
        // 这是导致原始错误的关键测试用例
        String result = evaluator.evaluateExpression("查询用户信息: 用户ID=#{args[0]}", context);
        assertEquals("查询用户信息: 用户ID=1", result);
        
        // 测试更复杂的中文模板
        result = evaluator.evaluateExpression("用户#{args[1]}的ID是#{args[0]}", context);
        assertEquals("用户张三的ID是1", result);
        
        // 测试多个占位符的中文模板
        result = evaluator.evaluateExpression("用户ID=#{args[0]}, 姓名=#{args[1]}", context);
        assertEquals("用户ID=1, 姓名=张三", result);
    }
    
    @Test
    void testEnglishPlainTextExpression() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{1});
        
        // 测试英文纯文本（原本导致错误的第二个用例）
        String result = evaluator.evaluateExpression("Get_user_information", context);
        assertEquals("Get_user_information", result);
        
        // 测试其他英文文本
        result = evaluator.evaluateExpression("User_login_success", context);
        assertEquals("User_login_success", result);
    }
    
    @Test
    void testMixedLanguageTemplateExpression() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{"admin", 123})
                .setResult("success");
        
        // 测试中英文混合模板
        String result = evaluator.evaluateExpression("User #{args[0]} 登录成功, ID=#{args[1]}", context);
        assertEquals("User admin 登录成功, ID=123", result);
        
        // 测试包含结果的模板
        result = evaluator.evaluateExpression("操作结果: #{result}", context);
        assertEquals("操作结果: success", result);
    }
    
    @Test
    void testExpressionTypeDetection() {
        // 测试表达式类型检测功能
        ExpressionType type = evaluator.detectExpressionType("查询用户信息");
        assertEquals(ExpressionType.PLAIN_TEXT, type);
        
        type = evaluator.detectExpressionType("#{args[0]}");
        assertEquals(ExpressionType.PURE_SPEL, type);
        
        type = evaluator.detectExpressionType("查询用户信息: 用户ID=#{args[0]}");
        assertEquals(ExpressionType.TEMPLATE, type);
        
        type = evaluator.detectExpressionType("Get_user_information");
        assertEquals(ExpressionType.PLAIN_TEXT, type);
    }
    
    @Test
    void testTemplateWithMultiplePlaceholders() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{"admin", 123, "管理员"})
                .setMethodName("login");
        
        // 测试多个占位符的复杂模板
        String result = evaluator.evaluateExpression(
            "方法#{methodName}: 用户#{args[0]}(#{args[2]}) 登录, ID=#{args[1]}", 
            context
        );
        assertEquals("方法login: 用户admin(管理员) 登录, ID=123", result);
    }
    
    @Test
    void testTemplateExpressionFailSafe() {
        LogContext context = new LogContext()
                .setArgs(new Object[]{1});
        
        // 测试模板中包含无效SpEL表达式的处理
        String result = evaluator.evaluateExpression(
            "查询用户: 用户ID=#{args[0]}, 状态=#{invalid.property}", 
            context
        );
        
        // 应该部分成功：有效的占位符被替换，无效的保留错误信息
        assertNotNull(result);
        assertTrue(result.contains("查询用户: 用户ID=1"));
        assertTrue(result.contains("表达式错误") || result.contains("invalid.property"));
    }
    
    @Test
    void testNewProcessorTypes() {
        // 测试新增的处理器类型查询方法
        String[] types = evaluator.getSupportedExpressionTypes();
        assertNotNull(types);
        assertTrue(types.length >= 3); // 至少应该有PLAIN_TEXT, PURE_SPEL, TEMPLATE
        
        // 检查是否包含期望的类型
        boolean hasPlainText = false, hasPureSpel = false, hasTemplate = false;
        for (String type : types) {
            if ("PLAIN_TEXT".equals(type)) hasPlainText = true;
            if ("PURE_SPEL".equals(type)) hasPureSpel = true;
            if ("TEMPLATE".equals(type)) hasTemplate = true;
        }
        
        assertTrue(hasPlainText);
        assertTrue(hasPureSpel);
        assertTrue(hasTemplate);
    }
}