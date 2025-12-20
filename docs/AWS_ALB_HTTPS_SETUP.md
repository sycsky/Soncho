# AWS ALB HTTPS 配置指南

本文档说明如何将 AWS Application Load Balancer (ALB) 从 HTTP 升级到 HTTPS。

## ⚠️ 重要提示

**你需要拥有自己的域名才能配置 HTTPS！**
- ❌ 不能使用 ALB 的 AWS 域名（如 `agent-xxx.elb.amazonaws.com`）
- ✅ 必须使用你自己的自定义域名（如 `example.com`）
- 如果没有域名，可以在 **AWS Route 53** 直接购买（推荐，方便管理）

### 在 AWS Route 53 购买域名

**是的，AWS 提供域名注册服务！** 通过 Route 53 可以购买域名。

**优势：**
- ✅ 与 AWS 服务集成方便（ALB、ACM 等）
- ✅ 统一管理（域名和 AWS 资源在同一个控制台）
- ✅ 自动配置 DNS（购买后自动配置）
- ✅ 价格透明

**购买步骤：**
1. 登录 AWS 控制台
2. 进入 **Route 53** 服务
3. 点击左侧菜单的 **已注册的域**（Registered domains）
4. 点击 **注册域**（Register domain）
5. 搜索你想要的域名（如 `example.com`）
6. 选择可用的域名并添加到购物车
7. 填写注册信息（姓名、地址、邮箱等）
8. 完成支付

**价格参考：**
- `.com` 域名：约 $12-15/年
- `.net` 域名：约 $12-15/年
- `.org` 域名：约 $12-15/年
- 其他后缀价格不同

**注意：**
- 域名注册需要 1-3 天完成
- 注册后，Route 53 会自动创建托管区域（Hosted Zone）
- 可以直接在 Route 53 中管理 DNS 记录

## 前置条件

1. 已有一个运行中的 ALB（当前配置为 HTTP:80）
2. **拥有自己的域名**（必需）⚠️
   - 不能使用 ALB 的 AWS 域名（如 `agent-xxx.elb.amazonaws.com`）
   - 需要有自己的域名（如 `example.com`）
   - 如果没有域名，需要先购买一个（可以在 Route 53、GoDaddy、阿里云等购买）
3. AWS 账户权限（可以创建和管理证书）
4. 域名的 DNS 管理权限（用于 DNS 验证）

## 步骤 1: 获取 SSL/TLS 证书

### ⚠️ 重要提示：使用公有证书，不是私有 CA！

**对于 ALB HTTPS，你需要的是"公有证书"（Public Certificate），而不是"私有 CA"（Private CA）！**

- **公有证书**：用于面向互联网的服务（如 ALB），浏览器自动信任 ✅
- **私有 CA**：用于内部网络，需要手动安装 CA 证书到客户端 ❌（不适合 ALB）

### 选项 A: 使用 AWS Certificate Manager (ACM) - 推荐 ⭐ 免费

**ACM 定价说明：**
- **不可导出的公有证书**：**完全免费** ✅（用于 ALB、CloudFront 等 AWS 服务）
- **可导出的公有证书**：需要付费（仅在需要导出证书到 AWS 外部使用时才需要）
  - 标准域名：15 美元/证书
  - 通配符证书：149 美元/证书

**对于 ALB HTTPS，使用不可导出的公有证书即可，完全免费！**

### 正确的操作步骤：

1. 登录 AWS 控制台，进入 **Certificate Manager (ACM)**
2. **重要**：确保你在正确的区域（Region），ALB 和证书必须在同一个区域
3. 在左侧菜单中，点击 **公有证书**（Public certificates）标签页
   - ⚠️ **不要点击"私有 CA"（Private CA）标签页**
4. 点击 **请求证书**（Request certificate）按钮
5. 选择 **请求公有证书**（Request a public certificate）
6. 在 **域名** 字段输入你的域名：
   - ⚠️ **重要**：不能使用 ALB 的 AWS 域名（如 `agent-433135470.ap-northeast-1.elb.amazonaws.com`）
   - ✅ **必须使用你自己的自定义域名**（如 `example.com`）
   - 单个域名：`example.com` 或 `api.example.com`
   - 通配符证书：`*.example.com`（可以保护所有子域名，如 `api.example.com`、`www.example.com` 等）
   - 多个域名：可以添加多个域名（如 `example.com` 和 `www.example.com`）
   
   **示例：**
   - 如果你有域名 `mycompany.com`，可以填写：
     - `mycompany.com`（主域名）
     - `*.mycompany.com`（通配符，保护所有子域名）
     - 或者同时添加 `mycompany.com` 和 `www.mycompany.com`
7. 选择验证方法：
   - **DNS 验证**（推荐）：需要在域名 DNS 中添加 CNAME 记录
   - **电子邮件验证**：需要验证域名邮箱（需要访问域名相关的邮箱）
8. 点击 **请求**（Request）
9. 如果选择 DNS 验证：
   - ACM 会显示需要添加的 CNAME 记录
   - 你会看到类似这样的信息：
     - **CNAME 名称**：`_f88a7c77c95c4577ab85c339f3da8205.bkagent.onesteppms.com.`
     - **CNAME 值**：`_37a501346346618feb25f4f2f29cb082.jkddzztszm.acm-validations.aws.`
   
   **添加 CNAME 记录的方法：**
   
   ### 方法 1: 如果域名在 Route 53 管理（最简单）✅
   
   1. 在 ACM 证书验证页面，点击 **"在 Route 53 中创建记录"**（Create record in Route 53）按钮
   2. AWS 会自动在 Route 53 中创建 CNAME 记录
   3. 等待几分钟后，点击 **刷新状态**，证书状态会变为 **已颁发**（Issued）
   
   ### 方法 2: 如果域名不在 Route 53 管理（手动添加）
   
   1. 复制 CNAME 名称和 CNAME 值
   2. 登录你的域名 DNS 提供商（如 GoDaddy、阿里云 DNS 等）
   3. 进入 DNS 管理页面
   4. 添加新的 CNAME 记录：
      - **记录类型**：选择 `CNAME`
      - **主机记录/名称**：填写 `_f88a7c77c95c4577ab85c339f3da8205.bkagent`（注意：去掉域名后缀）
      - **记录值/目标**：填写 `_37a501346346618feb25f4f2f29cb082.jkddzztszm.acm-validations.aws.`
      - **TTL**：使用默认值（如 300 秒）
   5. 保存记录
   6. 等待几分钟后，返回 ACM 页面点击 **刷新状态**
   7. 证书状态会变为 **已颁发**（Issued）
   
   **注意：**
   - CNAME 名称中的下划线和随机字符串是 ACM 自动生成的，必须完全一致
   - 如果 DNS 提供商要求填写完整域名，确保包含最后的点（`.`）
   - DNS 传播可能需要几分钟到几小时，通常几分钟内就会生效
   
10. **等待验证完成**：
    - 配置 CNAME 记录后，返回 ACM 证书详情页面
    - 点击 **刷新状态**（Refresh status）按钮
    - 通常几分钟内，证书状态会从 **待验证**（Pending validation）变为 **已颁发**（Issued）
    - 如果超过 1 小时仍未验证通过，检查：
      - CNAME 记录是否正确添加
      - CNAME 名称和值是否完全匹配（包括下划线和点）
      - DNS 是否已生效（可以使用 `nslookup` 或 `dig` 命令检查）
   
11. **重要**：证书会自动续期，无需额外费用

### 如果你误入了私有 CA 界面：

如果你看到的是"创建私有证书颁发机构"的界面，说明你进入了错误的页面。请：
1. 返回 ACM 主页
2. 点击左侧菜单的 **公有证书**（Public certificates）
3. 然后按照上面的步骤操作

### 选项 B: 上传已有证书

1. 在 ACM 中点击 **导入证书**
2. 上传证书文件、私钥和证书链
3. 填写证书名称和描述

## 步骤 2: 在 ALB 上配置 HTTPS 监听器

### 方法 1: 添加新的 HTTPS 监听器（推荐）

1. 登录 AWS 控制台，进入 **EC2 > 负载均衡器**
2. 选择你的 ALB（名称：`agent`）
3. 切换到 **侦听器和规则** 标签页
4. 点击 **添加侦听器** 按钮
5. 配置新监听器：
   - **协议：端口**：选择 `HTTPS` 和 `443`
   - **默认操作**：
     - 选择 **转发到目标组**
     - 选择现有的目标组（例如：`agentAiGroup`）
   - **默认 SSL/TLS 证书**：
     - 选择 **从 ACM 选择证书**
     - 选择你在步骤 1 中创建的证书
   - **安全策略**：选择推荐的安全策略（例如：`ELBSecurityPolicy-TLS-1-2-2017-01`）
6. 点击 **添加** 保存配置

### 方法 2: 修改现有 HTTP 监听器（不推荐）

⚠️ **注意**：直接修改 HTTP 监听器会导致服务中断，建议使用方法 1。

## 步骤 3: 配置 HTTP 到 HTTPS 重定向（可选但推荐）

为了确保所有 HTTP 流量自动重定向到 HTTPS：

1. 在 ALB 的 **侦听器和规则** 标签页
2. 找到 HTTP:80 监听器
3. 点击 **管理规则**
4. 修改默认操作：
   - 选择 **重定向到 URL**
   - 协议：`HTTPS`
   - 端口：`443`
   - 状态码：`301 - 永久移动`
5. 保存配置

## 步骤 4: 更新应用配置

应用已经配置了 `forward-headers-strategy: framework`，这允许应用正确识别来自 ALB 的 HTTPS 请求。

配置文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080
  forward-headers-strategy: framework
```

这个配置确保：
- 应用能够正确读取 `X-Forwarded-Proto` 头（ALB 会设置此头为 `https`）
- 应用能够正确读取 `X-Forwarded-For` 头（客户端真实 IP）
- 应用能够正确读取 `X-Forwarded-Port` 头（客户端访问端口）

## 步骤 5: 更新 DNS 记录（如果使用自定义域名）

1. 在域名 DNS 提供商处更新 A 记录或 CNAME 记录
2. 指向 ALB 的 DNS 名称（例如：`agent-433135470.ap-northeast-1.elb.amazonaws.com`）
3. 等待 DNS 传播（通常几分钟到几小时）

## 步骤 6: 验证 HTTPS 配置

### 测试 HTTPS 连接

```bash
# 使用 curl 测试
curl -I https://your-domain.com/api/health

# 或使用浏览器访问
https://your-domain.com/api/health
```

### 检查 SSL 证书

1. 在浏览器中访问你的 HTTPS URL
2. 点击地址栏的锁图标
3. 查看证书详情，确认：
   - 证书有效
   - 证书颁发机构正确
   - 证书未过期

### 测试 HTTP 到 HTTPS 重定向

```bash
# 测试重定向
curl -I http://your-domain.com/api/health

# 应该返回 301 重定向到 HTTPS
```

## 步骤 7: 更新前端配置（如果适用）

如果你的前端应用直接调用后端 API，需要更新 API 基础 URL：

```javascript
// 从 HTTP 改为 HTTPS
const API_BASE_URL = 'https://your-domain.com/api/v1';
```

## 常见问题

### Q1: 证书验证失败怎么办？

**A:** 确保：
- 证书已通过 ACM 验证
- 证书域名与访问域名匹配
- DNS 记录正确指向 ALB

### Q2: 应用仍然认为请求是 HTTP？

**A:** 检查：
- `application.yml` 中是否配置了 `forward-headers-strategy: framework`
- ALB 是否配置了正确的安全策略
- 应用是否已重新部署

### Q3: WebSocket 连接失败？

**A:** WebSocket 需要特殊配置：
- 确保 WebSocket 路径也通过 HTTPS 访问
- 检查 WebSocket 配置中的 `allowedOrigins` 设置
- 确保 ALB 支持 WebSocket（ALB 默认支持）

### Q4: 如何强制所有流量使用 HTTPS？

**A:** 在 ALB 的 HTTP:80 监听器上配置重定向规则（见步骤 3）

## 成本说明 💰

### ACM 证书费用
- ✅ **不可导出的公有证书**：**完全免费**（用于 ALB、CloudFront 等 AWS 服务）
- ✅ **自动续期**：AWS 自动管理证书续期，无需额外费用
- ✅ **无隐藏费用**：证书本身不产生任何费用

### 其他相关费用
- **ALB 使用费**：按小时计费（与 HTTP/HTTPS 无关，费用相同）
- **数据传输费**：HTTPS 数据传输费用与 HTTP 相同
- **ACM 证书**：免费（用于 AWS 服务）

**总结**：将 ALB 从 HTTP 升级到 HTTPS，使用 ACM 证书**不会产生额外费用**！🎉

### 可导出的证书（仅在需要时）
如果你需要在 AWS 外部使用证书（如 EC2 实例、本地服务器等），可以选择可导出的公有证书：
- 标准域名证书：**15 美元/证书**
- 通配符证书：**149 美元/证书**
- 这些费用在证书颁发时收取，续期时再次收取

**对于 ALB HTTPS，完全不需要可导出证书，使用免费的不可导出证书即可！**

## 安全建议

1. **使用 ACM 证书**：AWS 自动管理证书续期，完全免费
2. **启用安全策略**：使用最新的 TLS 版本（TLS 1.2 或更高）
3. **配置 WAF**：在 ALB 前添加 AWS WAF 以增强安全性（可选，会产生额外费用）
4. **定期更新**：保持应用和依赖库的最新版本

## 参考资源

- [AWS ALB 文档](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/introduction.html)
- [AWS Certificate Manager 文档](https://docs.aws.amazon.com/acm/)
- [Spring Boot 代理配置](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.web.use-behind-a-proxy-server)

