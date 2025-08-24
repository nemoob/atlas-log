package io.github.nemoob.atlas.log.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感数据脱敏器测试
 * 
 * @author Atlas Team
 * @since 1.0.0
 */
class SensitiveDataMaskerTest {
    
    private SensitiveDataMasker masker;
    
    @BeforeEach
    void setUp() {
        masker = new SensitiveDataMasker(true);
    }
    
    @Test
    void testMaskBasicTypes() {
        // 基本类型应该直接返回
        assertEquals("test", masker.maskSensitiveData("test"));
        assertEquals(123, masker.maskSensitiveData(123));
        assertEquals(123L, masker.maskSensitiveData(123L));
        assertEquals(123.45, masker.maskSensitiveData(123.45));
        assertEquals(true, masker.maskSensitiveData(true));
        assertNull(masker.maskSensitiveData(null));
    }
    
    @Test
    void testMaskArray() {
        Object[] array = {"password123", "normal", "token456"};
        Object result = masker.maskSensitiveData(array);
        
        assertTrue(result instanceof Object[]);
        Object[] maskedArray = (Object[]) result;
        assertEquals(3, maskedArray.length);
        assertEquals("password123", maskedArray[0]); // 数组元素本身不会被脱敏
        assertEquals("normal", maskedArray[1]);
        assertEquals("token456", maskedArray[2]);
    }
    
    @Test
    void testMaskList() {
        List<String> list = Arrays.asList("password123", "normal", "token456");
        Object result = masker.maskSensitiveData(list);
        
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<String> maskedList = (List<String>) result;
        assertEquals(3, maskedList.size());
        assertEquals("password123", maskedList.get(0)); // List元素本身不会被脱敏
    }
    
    @Test
    void testMaskMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "testuser");
        map.put("password", "secret123");
        map.put("token", "abc123");
        map.put("normalField", "normalValue");
        
        Object result = masker.maskSensitiveData(map);
        
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> maskedMap = (Map<String, Object>) result;
        
        assertEquals("testuser", maskedMap.get("username"));
        assertEquals("***", maskedMap.get("password"));
        assertEquals("***", maskedMap.get("token"));
        assertEquals("normalValue", maskedMap.get("normalField"));
    }
    
    @Test
    void testMaskObject() {
        TestUser user = new TestUser();
        user.username = "testuser";
        user.password = "secret123";
        user.accessToken = "token123";
        user.age = 25;
        
        Object result = masker.maskSensitiveData(user);
        
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> maskedMap = (Map<String, Object>) result;
        
        assertEquals("testuser", maskedMap.get("username"));
        assertEquals("***", maskedMap.get("password"));
        assertEquals("***", maskedMap.get("accessToken"));
        assertEquals(25, maskedMap.get("age"));
    }
    
    @Test
    void testCustomSensitiveFields() {
        masker.addSensitiveField("customSecret");
        
        Map<String, Object> map = new HashMap<>();
        map.put("customSecret", "shouldBeMasked");
        map.put("normalField", "shouldNotBeMasked");
        
        Object result = masker.maskSensitiveData(map);
        
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> maskedMap = (Map<String, Object>) result;
        
        assertEquals("***", maskedMap.get("customSecret"));
        assertEquals("shouldNotBeMasked", maskedMap.get("normalField"));
    }
    
    @Test
    void testBatchAddSensitiveFields() {
        masker.addSensitiveFields("field1", "field2", "field3");
        
        Map<String, Object> map = new HashMap<>();
        map.put("field1", "value1");
        map.put("field2", "value2");
        map.put("field3", "value3");
        map.put("normalField", "normalValue");
        
        Object result = masker.maskSensitiveData(map);
        
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> maskedMap = (Map<String, Object>) result;
        
        assertEquals("***", maskedMap.get("field1"));
        assertEquals("***", maskedMap.get("field2"));
        assertEquals("***", maskedMap.get("field3"));
        assertEquals("normalValue", maskedMap.get("normalField"));
    }
    
    @Test
    void testDisabledMasker() {
        SensitiveDataMasker disabledMasker = new SensitiveDataMasker(false);
        
        Map<String, Object> map = new HashMap<>();
        map.put("password", "secret123");
        map.put("token", "token123");
        
        Object result = disabledMasker.maskSensitiveData(map);
        
        // 禁用时应该返回原对象
        assertSame(map, result);
    }
    
    @Test
    void testCaseInsensitiveSensitiveFields() {
        Map<String, Object> map = new HashMap<>();
        map.put("PASSWORD", "secret123");
        map.put("Token", "token123");
        map.put("UserPassword", "password123");
        
        Object result = masker.maskSensitiveData(map);
        
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> maskedMap = (Map<String, Object>) result;
        
        assertEquals("***", maskedMap.get("PASSWORD"));
        assertEquals("***", maskedMap.get("Token"));
        assertEquals("***", maskedMap.get("UserPassword"));
    }
    
    @Test
    void testNestedObject() {
        TestUser user = new TestUser();
        user.username = "testuser";
        user.password = "secret123";
        
        Map<String, Object> container = new HashMap<>();
        container.put("user", user);
        container.put("normalField", "normalValue");
        
        Object result = masker.maskSensitiveData(container);
        
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> maskedContainer = (Map<String, Object>) result;
        
        assertEquals("normalValue", maskedContainer.get("normalField"));
        
        Object maskedUser = maskedContainer.get("user");
        assertTrue(maskedUser instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> maskedUserMap = (Map<String, Object>) maskedUser;
        assertEquals("testuser", maskedUserMap.get("username"));
        assertEquals("***", maskedUserMap.get("password"));
    }
    
    // 测试用的内部类
    static class TestUser {
        public String username;
        public String password;
        public String accessToken;
        public int age;
    }
}