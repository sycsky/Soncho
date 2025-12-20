# 双版本架构方案（社区版 + SAAS商业版）

## 📋 方案概述

本方案提供一种**零侵入**的方式，在现有社区版代码基础上，通过模块化架构和条件装配机制，实现社区版和SAAS商业版的独立运行。

**核心原则**：
- ✅ 社区版代码完全独立，不受影响
- ✅ SAAS模块作为可选插件，通过接口抽象接入
- ✅ 商业版可以独立构建和部署
- ✅ 两套代码可以共存，互不干扰

---

## 1. 项目结构设计

### 1.1 Maven 多模块结构

```
ai_kef/
├── pom.xml                          # 父POM
├── ai-kef-common/                   # 公共模块（社区版）
│   ├── pom.xml
│   └── src/main/java/...
├── ai-kef-core/                     # 核心业务模块（社区版）
│   ├── pom.xml
│   └── src/main/java/...
├── ai-kef-web/                      # Web层（社区版）
│   ├── pom.xml
│   └── src/main/java/...
├── ai-kef-saas-api/                 # SAAS接口定义（可选）
│   ├── pom.xml
│   └── src/main/java/...
│       └── com/example/aikef/saas/
│           └── api/
│               ├── TenantService.java
│               ├── SubscriptionService.java
│               └── ...
├── ai-kef-saas-impl/                # SAAS实现（商业版，不提交）
│   ├── pom.xml
│   └── src/main/java/...
│       └── com/example/aikef/saas/
│           └── impl/
│               ├── TenantServiceImpl.java
│               ├── SubscriptionServiceImpl.java
│               └── ...
└── ai-kef-saas-web/                 # SAAS Web层（商业版，不提交）
    ├── pom.xml
    └── src/main/java/...
```

### 1.2 目录结构说明

```
ai_kef/
├── .gitignore                       # 忽略SAAS模块
├── pom.xml                          # 父POM（社区版）
├── pom-saas.xml                     # SAAS版POM（不提交）
├── src/                             # 社区版代码（现有代码）
│   └── main/java/...
├── saas/                            # SAAS模块（不提交到Git）
│   ├── ai-kef-saas-api/
│   ├── ai-kef-saas-impl/
│   └── ai-kef-saas-web/
└── docs/
    └── DUAL_VERSION_ARCHITECTURE.md
```

---

## 2. Git 分支策略

### 2.1 分支结构

```
main (社区版)
  ├── 所有社区版代码
  └── .gitignore 排除 saas/ 目录

saas-commercial (商业版分支，私有)
  ├── 包含所有社区版代码
  └── 包含 saas/ 目录下的商业代码
```

### 2.2 .gitignore 配置

```gitignore
# SAAS商业版模块（不提交到社区版仓库）
saas/
pom-saas.xml
*.saas.jar

# 商业版构建产物
target-saas/
```

### 2.3 分支管理流程

```bash
# 社区版开发流程
git checkout main
git pull origin main
# 开发社区版功能
git commit -m "feat: 社区版新功能"
git push origin main

# 商业版开发流程（私有仓库）
git checkout saas-commercial
git merge main  # 同步社区版更新
# 开发SAAS功能
git commit -m "feat: SAAS功能"
git push origin saas-commercial  # 推送到私有仓库
```

---

## 3. Maven 配置

### 3.1 父POM（社区版）- pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>ai-kef</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>ai-kef-common</module>
        <module>ai-kef-core</module>
        <module>ai-kef-web</module>
        <!-- SAAS模块不包含在社区版 -->
    </modules>
    
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.5</spring-boot.version>
        <saas.enabled>false</saas.enabled>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 3.2 SAAS版父POM - pom-saas.xml（不提交）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>ai-kef-saas</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <modules>
        <!-- 包含所有社区版模块 -->
        <module>../ai-kef-common</module>
        <module>../ai-kef-core</module>
        <module>../ai-kef-web</module>
        <!-- SAAS模块 -->
        <module>saas/ai-kef-saas-api</module>
        <module>saas/ai-kef-saas-impl</module>
        <module>saas/ai-kef-saas-web</module>
    </modules>
    
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.5</spring-boot.version>
        <saas.enabled>true</saas.enabled>
    </properties>
</project>
```

### 3.3 社区版Web模块 - ai-kef-web/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <artifactId>ai-kef</artifactId>
        <groupId>com.example</groupId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>ai-kef-web</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>ai-kef-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- SAAS API（可选，社区版不实现） -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>ai-kef-saas-api</artifactId>
            <version>${project.version}</version>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 3.4 SAAS API模块 - saas/ai-kef-saas-api/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <artifactId>ai-kef-saas</artifactId>
        <groupId>com.example</groupId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom-saas.xml</relativePath>
    </parent>
    
    <artifactId>ai-kef-saas-api</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <!-- 只包含接口定义，不依赖实现 -->
    </dependencies>
</project>
```

---

## 4. 接口抽象设计

### 4.1 SAAS API 接口定义

**saas/ai-kef-saas-api/src/main/java/com/example/aikef/saas/api/TenantService.java**

```java
package com.example.aikef.saas.api;

import java.util.UUID;
import java.util.Optional;

/**
 * 租户服务接口
 * 社区版不实现，商业版实现
 */
public interface TenantService {
    
    /**
     * 获取当前租户ID
     * 社区版返回null，商业版返回实际租户ID
     */
    Optional<UUID> getCurrentTenantId();
    
    /**
     * 验证租户访问权限
     * 社区版直接通过，商业版验证租户ID
     */
    boolean validateTenantAccess(UUID resourceTenantId);
    
    /**
     * 检查功能是否可用
     * 社区版返回true，商业版根据套餐判断
     */
    boolean hasFeature(String feature);
    
    /**
     * 检查使用量限制
     * 社区版返回true，商业版检查实际使用量
     */
    boolean checkLimit(String limitType, long currentValue);
}
```

**saas/ai-kef-saas-api/src/main/java/com/example/aikef/saas/api/SubscriptionService.java**

```java
package com.example.aikef.saas.api;

import java.util.UUID;

/**
 * 订阅服务接口
 */
public interface SubscriptionService {
    
    /**
     * 获取租户套餐类型
     */
    String getPlanType(UUID tenantId);
    
    /**
     * 检查订阅状态
     */
    boolean isSubscriptionActive(UUID tenantId);
    
    /**
     * 记录使用量
     */
    void recordUsage(UUID tenantId, String usageType, long amount);
}
```

### 4.2 社区版默认实现（空实现）

**src/main/java/com/example/aikef/saas/CommunityTenantService.java**

```java
package com.example.aikef.saas;

import com.example.aikef.saas.api.TenantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * 社区版默认实现（空实现）
 * 当SAAS模块不存在时自动使用此实现
 */
@Service
@ConditionalOnMissingBean(name = "tenantServiceImpl")
public class CommunityTenantService implements TenantService {
    
    @Override
    public Optional<UUID> getCurrentTenantId() {
        // 社区版没有租户概念，返回空
        return Optional.empty();
    }
    
    @Override
    public boolean validateTenantAccess(UUID resourceTenantId) {
        // 社区版不验证，直接通过
        return true;
    }
    
    @Override
    public boolean hasFeature(String feature) {
        // 社区版所有功能都可用
        return true;
    }
    
    @Override
    public boolean checkLimit(String limitType, long currentValue) {
        // 社区版不限制
        return true;
    }
}
```

### 4.3 商业版实现（不提交）

**saas/ai-kef-saas-impl/src/main/java/com/example/aikef/saas/impl/TenantServiceImpl.java**

```java
package com.example.aikef.saas.impl;

import com.example.aikef.saas.api.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * SAAS商业版实现
 * 此文件不提交到社区版仓库
 */
@Service("tenantServiceImpl")
public class TenantServiceImpl implements TenantService {
    
    @Autowired
    private TenantContext tenantContext;
    
    @Autowired
    private FeatureService featureService;
    
    @Override
    public Optional<UUID> getCurrentTenantId() {
        return Optional.ofNullable(tenantContext.getTenantId());
    }
    
    @Override
    public boolean validateTenantAccess(UUID resourceTenantId) {
        UUID currentTenantId = tenantContext.getTenantId();
        return currentTenantId != null && currentTenantId.equals(resourceTenantId);
    }
    
    @Override
    public boolean hasFeature(String feature) {
        UUID tenantId = tenantContext.getTenantId();
        return featureService.hasFeature(tenantId, feature);
    }
    
    @Override
    public boolean checkLimit(String limitType, long currentValue) {
        UUID tenantId = tenantContext.getTenantId();
        return featureService.checkLimit(tenantId, limitType, currentValue);
    }
}
```

---

## 5. 条件装配机制

### 5.1 使用 Spring 条件装配

**src/main/java/com/example/aikef/config/SaasAutoConfiguration.java**

```java
package com.example.aikef.config;

import com.example.aikef.saas.api.TenantService;
import com.example.aikef.saas.CommunityTenantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SAAS自动配置
 * 如果SAAS模块存在，使用商业版实现
 * 如果不存在，使用社区版默认实现
 */
@Configuration
public class SaasAutoConfiguration {
    
    /**
     * 社区版默认实现
     * 只有当商业版实现不存在时才生效
     */
    @Bean
    @ConditionalOnMissingBean(name = "tenantServiceImpl")
    public TenantService communityTenantService() {
        return new CommunityTenantService();
    }
}
```

### 5.2 使用 @ConditionalOnClass

**src/main/java/com/example/aikef/config/TenantAspect.java**

```java
package com.example.aikef.config;

import com.example.aikef.saas.api.TenantService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * 租户数据隔离切面
 * 只有当TenantService存在时才生效
 */
@Aspect
@Component
@ConditionalOnBean(TenantService.class)
public class TenantAspect {
    
    @Autowired(required = false)
    private TenantService tenantService;
    
    /**
     * 自动注入tenant_id到查询中
     */
    @Before("execution(* com.example.aikef.repository.*Repository.*(..))")
    public void injectTenantId() {
        if (tenantService != null) {
            tenantService.getCurrentTenantId().ifPresent(tenantId -> {
                // 注入tenant_id到查询上下文
                TenantContext.setTenantId(tenantId);
            });
        }
    }
}
```

---

## 6. 数据访问层改造

### 6.1 使用 AOP 自动注入 tenant_id

**src/main/java/com/example/aikef/repository/TenantAwareRepository.java**

```java
package com.example.aikef.repository;

import com.example.aikef.saas.api.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

/**
 * 租户感知的Repository基类
 * 社区版不生效，商业版自动注入tenant_id
 */
public interface TenantAwareRepository<T> extends JpaRepository<T, UUID> {
    
    // 社区版：直接查询，不添加tenant_id过滤
    // 商业版：通过AOP自动添加tenant_id过滤
    
    // 示例方法
    // List<T> findAll();  // 自动添加 WHERE tenant_id = ?
}
```

### 6.2 实体类改造（可选）

**src/main/java/com/example/aikef/model/base/TenantAwareEntity.java**

```java
package com.example.aikef.model.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * 租户感知的实体基类
 * 社区版不使用，商业版继承此基类
 */
@MappedSuperclass
public abstract class TenantAwareEntity extends AuditableEntity {
    
    @Column(name = "tenant_id")
    private UUID tenantId;
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
}
```

**社区版实体保持不变**：
```java
// 社区版：Agent extends AuditableEntity
// 商业版：Agent extends TenantAwareEntity（通过继承或Mixin）
```

---

## 7. 构建和打包

### 7.1 社区版构建

```bash
# 构建社区版
mvn clean package -f pom.xml

# 输出：ai-kef-web-1.0.0.jar（社区版）
```

### 7.2 商业版构建

```bash
# 构建商业版
mvn clean package -f pom-saas.xml

# 输出：ai-kef-saas-web-1.0.0-SNAPSHOT.jar（商业版）
```

### 7.3 构建脚本

**build-community.sh**（社区版）

```bash
#!/bin/bash
echo "构建社区版..."
mvn clean package -f pom.xml -DskipTests
echo "构建完成：target/ai-kef-web-1.0.0.jar"
```

**build-saas.sh**（商业版，不提交）

```bash
#!/bin/bash
echo "构建SAAS商业版..."
mvn clean package -f pom-saas.xml -DskipTests
echo "构建完成：target-saas/ai-kef-saas-web-1.0.0-SNAPSHOT.jar"
```

---

## 8. 配置文件管理

### 8.1 社区版配置

**src/main/resources/application.yml**

```yaml
spring:
  application:
    name: ai-kef-community

# 社区版配置
community:
  enabled: true
  features:
    ai-workflow: true
    knowledge-base: true
    tools: true
```

### 8.2 商业版配置

**saas/ai-kef-saas-web/src/main/resources/application-saas.yml**（不提交）

```yaml
spring:
  application:
    name: ai-kef-saas

# SAAS配置
saas:
  enabled: true
  tenant:
    identification:
      - subdomain
      - header
      - token
  billing:
    enabled: true
    provider: stripe
```

---

## 9. 数据库迁移策略

### 9.1 社区版数据库

```sql
-- 社区版：不包含tenant相关表
-- 所有表保持原样
```

### 9.2 商业版数据库

```sql
-- 商业版：包含tenant相关表
CREATE TABLE tenants (...);
CREATE TABLE subscriptions (...);
-- ...

-- 为现有表添加tenant_id（可选）
ALTER TABLE agents ADD COLUMN tenant_id CHAR(36);
-- ...
```

### 9.3 使用 Flyway/Liquibase 管理

**社区版迁移脚本**：
```
db/migration/
  V1__init_community.sql
  V2__add_features.sql
```

**商业版迁移脚本**（不提交）：
```
saas/db/migration/
  V1__init_saas.sql
  V2__add_tenant_tables.sql
  V3__migrate_data.sql
```

---

## 10. 代码使用示例

### 10.1 在业务代码中使用

**src/main/java/com/example/aikef/service/ChatSessionService.java**

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
            tenantService.getCurrentTenantId().ifPresent(session::setTenantId);
        }
        
        return sessionRepository.save(session);
    }
    
    public List<ChatSession> findAll() {
        // 社区版：查询所有
        // 商业版：自动过滤当前租户的数据
        return sessionRepository.findAll();
    }
}
```

### 10.2 功能权限检查

```java
@Service
public class FeatureService {
    
    @Autowired(required = false)
    private TenantService tenantService;
    
    public void checkFeature(String feature) {
        if (tenantService != null) {
            if (!tenantService.hasFeature(feature)) {
                throw new FeatureNotAvailableException("功能不可用");
            }
        }
        // 社区版：不检查，直接通过
    }
}
```

---

## 11. 部署方案

### 11.1 社区版部署

```yaml
# docker-compose-community.yml
version: '3.8'
services:
  app:
    image: ai-kef-community:1.0.0
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=community
```

### 11.2 商业版部署

```yaml
# docker-compose-saas.yml（不提交）
version: '3.8'
services:
  app:
    image: ai-kef-saas:1.0.0
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=saas
      - SAAS_ENABLED=true
```

---

## 12. 版本管理

### 12.1 版本号策略

```
社区版：1.0.0, 1.1.0, 1.2.0...
商业版：1.0.0-SAAS, 1.1.0-SAAS, 1.2.0-SAAS...
```

### 12.2 依赖管理

```xml
<!-- 商业版依赖社区版 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>ai-kef-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 13. 开发工作流

### 13.1 日常开发

```bash
# 1. 在社区版开发新功能
git checkout main
# 开发...
git commit -m "feat: 新功能"
git push origin main

# 2. 同步到商业版
git checkout saas-commercial
git merge main
# 开发SAAS特定功能
git commit -m "feat: SAAS功能"
git push origin saas-commercial  # 推送到私有仓库
```

### 13.2 发布流程

```bash
# 社区版发布
git checkout main
git tag v1.0.0
git push origin v1.0.0
mvn deploy -f pom.xml

# 商业版发布
git checkout saas-commercial
git tag v1.0.0-SAAS
git push origin v1.0.0-SAAS  # 推送到私有仓库
mvn deploy -f pom-saas.xml  # 部署到私有Maven仓库
```

---

## 14. 优势总结

### 14.1 代码隔离
- ✅ 社区版代码完全独立
- ✅ SAAS代码不污染社区版
- ✅ 两套代码可以独立演进

### 14.2 维护成本
- ✅ 社区版功能自动同步到商业版
- ✅ 商业版可以独立开发SAAS功能
- ✅ 减少代码重复

### 14.3 商业保护
- ✅ SAAS代码不提交到公开仓库
- ✅ 商业版可以独立授权
- ✅ 保护商业机密

### 14.4 灵活性
- ✅ 可以选择性启用SAAS功能
- ✅ 支持渐进式迁移
- ✅ 不影响现有用户

---

## 15. 实施步骤

### 阶段1：项目结构改造（1周）
1. 创建Maven多模块结构
2. 配置父POM和子模块
3. 创建SAAS API接口定义
4. 配置.gitignore

### 阶段2：接口抽象（1周）
1. 定义SAAS接口
2. 实现社区版默认实现
3. 配置条件装配
4. 测试接口切换

### 阶段3：SAAS实现（2-3周）
1. 实现商业版功能
2. 数据库设计
3. 功能开发
4. 测试验证

### 阶段4：构建和部署（1周）
1. 配置构建脚本
2. 配置CI/CD
3. 部署测试
4. 文档完善

---

## 16. 注意事项

### 16.1 接口兼容性
- SAAS接口一旦定义，要保持向后兼容
- 新增方法要有默认实现或标记为可选

### 16.2 依赖管理
- 避免循环依赖
- SAAS模块依赖社区版，不能反向依赖

### 16.3 测试策略
- 社区版测试不依赖SAAS模块
- 商业版测试包含社区版功能测试

### 16.4 文档管理
- 社区版文档公开
- 商业版文档私有

---

## 17. 总结

本方案通过以下机制实现零侵入的双版本架构：

1. **模块化设计**：SAAS作为独立模块，不侵入社区版
2. **接口抽象**：通过接口定义，社区版和商业版分别实现
3. **条件装配**：Spring自动选择实现类
4. **分支管理**：Git分支隔离商业代码
5. **构建分离**：独立的构建和打包流程

这样既保护了商业代码，又保证了社区版的独立性，同时两套代码可以独立演进。

