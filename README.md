# TradeFlow

面向 2027 届 Java 开发岗位的交易履约与运营协同平台。项目围绕支付回调、库存预占、订单状态机和可靠事件投递构建完整业务链路，并提供可公开操作的 Vue 3 工作台。

- 在线演示：https://tradeflow-demo-63gh.onrender.com
- 演示账号：`admin` / `admin123`
- 技术栈：Java 21、Spring Boot 3.3、Spring Security、Spring Data JPA、MySQL、Vue 3、Docker、Render

## 核心能力

- 支付回调以 `event_id` 唯一约束、事务和订单行锁保证幂等。
- 订单状态机限制 `CREATED -> PAID -> RESERVED -> SHIPPED -> COMPLETED`，拒绝非法跳转。
- 商品库存通过 JPA `@Version` 乐观锁避免并发预占的静默覆盖。
- 业务状态和 Outbox 事件在同一数据库事务提交，为异步投递提供可靠边界。
- JWT + BCrypt 无状态认证，统一异常响应和参数校验。
- 幂等种子数据包含 28 条订单、6 个 SKU、支付流水和领域事件，可直接演示。
- 多阶段 Docker 构建将 Vue 静态资源和 Spring Boot 打包到同一运行镜像。
- 线上数据库使用 TiDB Cloud Starter 的 MySQL 兼容接口，独立 `tradeflow` schema，并通过 JDBC TLS 连接。

## 本地运行

```bash
docker compose up --build
```

访问 http://localhost:8080。停止容器但保留 MySQL 数据：

```bash
docker compose down
```

## 验证

```bash
docker build --target backend-builder -t tradeflow-test .
docker run --rm tradeflow-test mvn test
```

健康检查：`GET /health`，成功时返回 `{"application":"ok","database":"ok"}`。

## 文档

- [使用与演示路线](docs/USAGE_GUIDE.md)
- [架构与技术取舍](docs/ARCHITECTURE.md)
- [Java 面试问答](docs/INTERVIEW_GUIDE.md)
- [岗位要求与项目证据](docs/JOB_ALIGNMENT.md)

## 真实性边界

当前演示实现了本地事务 Outbox 的可靠写入，但没有声称已经部署 Kafka、RocketMQ 或 Redis。生产环境可由独立 Publisher 扫描 `PENDING` 事件并投递到消息系统，消费者继续使用事件 ID 做幂等。

