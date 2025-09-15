# Atlas Log 变更日志

本文档记录了 Atlas Log 各版本的重要变更、新功能、修复和改进。

## [0.2.0] - 2025-09-12

### 🔧 修复 (Bug Fixes)

#### **修复注解配置中 urlFormat 属性不生效的问题**
- **问题描述**: 使用 `@EnableAtlasLog(httpLog = @AtlasLogHttpLog(urlFormat = "{queryString}"))` 注解配置时，urlFormat 属性没有生效，系统仍使用默认的 `{method} {uri}` 格式
- **根本原因**: `ConfigMerger.mergeNestedConfigs()` 方法中缺少了 `mergeHttpLogConfig()` 调用，导致 HTTP 日志配置无法正确合并
- **修复内容**:
  - 在 `ConfigMerger.mergeNestedConfigs()` 中添加了 `mergeHttpLogConfig()` 调用
  - 实现了完整的 `mergeHttpLogConfig()` 方法，支持所有 HTTP 日志配置属性的合并
  - 修复了布尔类型参数的类型兼容性问题
  - 确保注解配置优先于 YAML 配置
- **影响范围**: 所有使用 `@AtlasLogHttpLog` 注解配置的用户
- **向后兼容**: 完全向后兼容，不影响现有功能

#### **修复 SensitiveDataMasker 序列化异常问题**
- **问题描述**: 在处理复杂对象（如 ResponseFacade、RequestFacade 等 Servlet 容器对象）时出现序列化异常
- **修复内容**:
  - 简化了 `SensitiveDataMasker` 实现，避免复杂的序列化操作
  - 对于复杂对象直接返回类型描述而非尝试序列化
  - 移除了可能导致循环引用和序列化失败的复杂逻辑
- **性能改进**: 显著提升了日志记录性能，减少了内存占用

#### **修复注解级别格式化器配置问题**
- **问题描述**: `@Log` 注解中的 `argumentFormatter` 和 `resultFormatter` 属性配置后不生效
- **修复内容**:
  - 修复了 `AtlasLogAspect.serializeArgs()` 方法中方法信息传递不完整的问题
  - 确保注解级别的格式化器配置能够正确应用
  - 添加了调试日志以便排查配置问题

### ✨ 功能增强 (Enhancements)

#### **完善 HTTP 日志配置支持**
- 新增了完整的 HTTP 日志配置合并逻辑
- 支持以下配置属性的注解级别配置：
  - `urlFormat`: URL 格式化模式，支持占位符 `{method}`, `{uri}`, `{queryString}`, `{remoteAddr}`
  - `logFullParameters`: 是否记录完整请求参数
  - `includeQueryString`: 是否包含查询字符串
  - `includeHeaders`: 是否包含请求头信息
  - `excludeHeaders`: 排除的请求头列表

#### **改进配置优先级处理**
- 明确了配置优先级：注解配置 > YAML 配置 > 默认值
- 改进了配置合并逻辑，确保各级配置能够正确生效
- 添加了详细的调试日志，便于排查配置问题

### 📚 文档更新 (Documentation)

- 新增了变更日志文档
- 更新了配置相关文档，明确了注解配置的使用方法
- 添加了 urlFormat 配置的详细说明和示例

### 🔍 调试改进 (Debugging)

- 添加了配置解析过程的调试日志
- 改进了错误信息的可读性
- 提供了配置验证的调试方法

---

## 如何升级到 0.2.0

### Maven 依赖更新
```xml
<dependency>
    <groupId>io.github.nemoob</groupId>
    <artifactId>atlas-log-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

### 配置验证
升级后，请验证您的注解配置是否正常工作：

```java
@Component
public class ConfigChecker {
    
    @Autowired
    private LogConfigProperties properties;
    
    @PostConstruct
    public void checkConfig() {
        System.out.println("=== Atlas Log Configuration ===");
        System.out.println("URL Format: " + properties.getHttpLog().getUrlFormat());
        System.out.println("Include Query String: " + properties.getHttpLog().isIncludeQueryString());
        System.out.println("===============================");
    }
}
```

### 注意事项
- 0.2.0 版本完全向后兼容，无需修改现有代码
- 如果您之前遇到注解配置不生效的问题，升级后应该能够正常工作
- 建议启用调试日志以验证配置是否正确加载

---

## 版本说明

- **[0.2.0]**: 当前版本，修复了重要的配置问题
- **[0.1.x]**: 初始版本系列

更多详细信息请参考各版本的发布说明。