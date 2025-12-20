# 双版本架构快速实施指南

## 🚀 快速开始

本指南帮助你在30分钟内搭建双版本架构的基础框架。

---

## 步骤1：创建项目结构（5分钟）

### 1.1 创建目录结构

```bash
# 在项目根目录执行
mkdir -p saas/ai-kef-saas-api/src/main/java/com/example/aikef/saas/api
mkdir -p saas/ai-kef-saas-impl/src/main/java/com/example/aikef/saas/impl
mkdir -p saas/ai-kef-saas-web/src/main/java/com/example/aikef/saas/web
```

### 1.2 更新 .gitignore

在项目根目录的 `.gitignore` 文件末尾添加：

```gitignore
# ========== SAAS商业版模块（不提交） ==========
saas/
pom-saas.xml
*.saas.jar
target-saas/
build-saas.sh
```

---

## 步骤2：创建SAAS API接口（10分钟）

### 2.1 创建 TenantService 接口

**文件**：`saas/ai-kef-saas-api/src/main/java/com/example/aikef/saas/api/TenantService.java`

```java
package com.example.aikef.saas.api;

import java.util.Optional;
import java.util.UUID;

/**
 * 租户服务接口
 * 社区版不实现，商业版实现
 */
public interface TenantService {
    Optional<UUID> getCurrentTenantId();
    boolean validateTenantAccess(UUID resourceTenantId);
    boolean hasFeature(String feature);
    boolean checkLimit(String limitType, long currentValue);
}
```

### 2.2 创建社区版默认实现

**文件**：`src/main/java/com/example/aikef/saas/CommunityTenantService.java`

```java
package com.example.aikef.saas;

import com.example.aikef.saas.api.TenantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnMissingBean(name = "tenantServiceImpl")
public class CommunityTenantService implements TenantService {
    
    @Override
    public Optional<UUID> getCurrentTenantId() {
        return Optional.empty();
    }
    
    @Override
    public boolean validateTenantAccess(UUID resourceTenantId) {
        return true;
    }
    
    @Override
    public boolean hasFeature(String feature) {
        return true;
    }
    
    @Override
    public boolean checkLimit(String limitType, long currentValue) {
        return true;
    }
}
```

---

## 步骤3：创建Maven配置（10分钟）

### 3.1 创建SAAS API模块POM

**文件**：`saas/ai-kef-saas-api/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>
    
    <groupId>com.example</groupId>
    <artifactId>ai-kef-saas-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    
    <dependencies>
        <!-- 只包含接口，不依赖实现 -->
    </dependencies>
</project>
```

### 3.2 更新主POM（可选依赖）

在现有的 `pom.xml` 中添加SAAS API作为可选依赖：

```xml
<dependencies>
    <!-- 其他依赖... -->
    
    <!-- SAAS API（可选） -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>ai-kef-saas-api</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 步骤4：在业务代码中使用（5分钟）

### 4.1 示例：在Service中使用

**文件**：`src/main/java/com/example/aikef/service/ChatSessionService.java`

```java
package com.example.aikef.service;

import com.example.aikef.saas.api.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionService {
    
    @Autowired(required = false)
    private TenantService tenantService;  // 社区版为null，商业版有值
    
    public ChatSession createSession(Customer customer) {
        ChatSession session = new ChatSession();
        session.setCustomer(customer);
        
        // 如果SAAS模块存在，自动设置tenant_id
        if (tenantService != null) {
            tenantService.getCurrentTenantId().ifPresent(tenantId -> {
                // 设置tenant_id（需要实体类支持）
                // session.setTenantId(tenantId);
            });
        }
        
        return sessionRepository.save(session);
    }
}
```

---

## 步骤5：测试验证

### 5.1 社区版测试

```bash
# 构建社区版
mvn clean compile

# 运行测试
mvn test

# 启动应用
mvn spring-boot:run
```

验证：应用正常启动，`TenantService` 使用 `CommunityTenantService` 实现。

### 5.2 商业版测试（可选，需要完整实现）

```bash
# 构建商业版（需要完整的pom-saas.xml）
mvn clean compile -f pom-saas.xml
```

---

## 下一步

完成基础框架后，可以：

1. **完善SAAS接口**：根据业务需求定义更多接口
2. **实现商业版功能**：在 `saas/ai-kef-saas-impl` 中实现具体功能
3. **数据库设计**：设计租户相关表结构
4. **功能开发**：开发SAAS特定功能

---

## 常见问题

### Q: 如何确保SAAS代码不提交？

A: 在 `.gitignore` 中添加 `saas/` 目录，并定期检查 `git status`。

### Q: 社区版如何引用SAAS接口？

A: 将SAAS API作为可选依赖（`<optional>true</optional>`），运行时通过条件装配选择实现。

### Q: 如何同步社区版更新到商业版？

A: 使用Git分支，定期将 `main` 分支合并到 `saas-commercial` 分支。

### Q: 商业版如何独立构建？

A: 创建独立的 `pom-saas.xml`，包含所有模块（社区版+SAAS模块）。

---

## 文件清单

### 需要创建的文件

```
saas/
├── ai-kef-saas-api/
│   ├── pom.xml
│   └── src/main/java/com/example/aikef/saas/api/
│       └── TenantService.java
└── (其他模块待开发)

src/main/java/com/example/aikef/saas/
└── CommunityTenantService.java
```

### 需要修改的文件

```
.gitignore          # 添加saas/目录
pom.xml            # 添加可选依赖（可选）
```

---

## 完成检查清单

- [ ] 创建了 `saas/` 目录结构
- [ ] 更新了 `.gitignore`
- [ ] 创建了 `TenantService` 接口
- [ ] 创建了 `CommunityTenantService` 实现
- [ ] 创建了SAAS API模块的 `pom.xml`
- [ ] 在业务代码中使用了 `TenantService`
- [ ] 社区版可以正常编译和运行
- [ ] 验证了 `git status` 不显示 `saas/` 目录

完成以上步骤后，双版本架构的基础框架就搭建完成了！

