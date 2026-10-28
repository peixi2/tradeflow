# TradeFlow

TradeFlow 是一个可公开操作的交易履约系统，重点处理交易链路中三个容易被普通 CRUD 忽略的问题：第三方支付可能重复回调，同一笔订单的并发更新可能互相覆盖，业务状态落库与领域事件记录可能出现一边成功、一边失败。项目使用数据库事务、唯一约束、悲观锁、乐观锁和本地事务 Outbox 组合出一条可验证的一致性链路。

它不是电商页面的功能集合，而是一个用于解释“失败发生在哪里、系统怎样阻断错误状态、哪些问题仍需生产组件解决”的后端工程项目。Vue 3 工作台把支付、库存预占、订单流转和事件记录放到同一演示路径中，面试官可以直接触发并观察数据库状态变化。

- 在线演示：https://tradeflow-demo-63gh.onrender.com
- 演示账号：`admin` / `admin123`
- 技术栈：Java 21、Spring Boot 3.3、Spring Security、Spring Data JPA、MySQL、Vue 3、Docker、Render

## 要解决的故障窗口

| 故障场景 | 可能结果 | 当前实现 |
| --- | --- | --- |
| 支付平台因超时重试同一回调 | 重复记账、重复推进订单 | 先查 `event_id` 快速返回；唯一索引作为并发写入的最终防线 |
| 同一订单收到并发支付更新 | 状态判断和写入交错 | `PESSIMISTIC_WRITE` 锁定订单行，在事务内串行处理 |
| 回调金额被篡改或订单号错配 | 错单被标记为已支付 | 在写流水前比较回调金额与订单总额 |
| 订单已更新但事件写入失败 | 下游永远不知道状态变化 | 订单、支付流水和 Outbox 在同一本地事务提交 |
| 两个请求同时预占同一 SKU | 后提交者覆盖先提交者的数量 | 商品 `@Version` 进入更新条件，冲突时回滚并返回 409 |
| 接口绕过正常业务顺序 | 已完成订单重新变为已支付 | 服务层状态机只允许定义好的迁移 |

## 关键链路

支付回调不是简单的“查重后更新”。请求先按 `event_id` 查询，命中时直接返回 `duplicate=true`；未命中时锁定订单，校验金额，`saveAndFlush` 支付流水以尽早触发唯一约束，再更新支付和订单状态，最后写入 `PaymentConfirmed` Outbox 事件。所有写入共享一个 `@Transactional` 边界，任何一步失败都会整体回滚。

库存预占只允许从 `PAID` 进入 `RESERVED`。服务先计算 `stock - reserved`，再更新预占量；JPA 在 SQL 的更新条件中加入版本号，使并发修改不能静默覆盖。状态流转规则集中在服务层，因此 REST 接口、未来的批处理或消息消费者可以复用同一组业务约束。

## 可验证证据

- 公网数据库保留 28 条订单、6 个 SKU、17 条支付流水和 15 条 Outbox 事件，容器重启不会清空数据。
- 对同一 `event_id` 连续提交两次，第一次写入，第二次返回 `duplicate=true`，支付流水只保留一条。
- 集成测试覆盖支付重复回调和已完成订单非法回退；健康检查同时验证应用与数据库连接。
- 多阶段 Docker 构建先编译 Vue，再由 Maven 打包 Spring Boot，最终镜像只运行一个 Web 服务。
- 线上数据库使用 TiDB Cloud Starter 的 MySQL 兼容接口和独立 `tradeflow` schema，通过 JDBC TLS 连接。

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

## 已实现与未实现

当前实现的可靠边界到“业务数据与 Outbox 原子落库”为止。仓库没有部署 Kafka、RocketMQ、Redis，也没有把 `PENDING` 事件伪装成已经可靠送达。生产化需要增加 Publisher 扫描、投递确认、失败重试和消费者幂等；只有完成这些环节，才能宣称事件已经从数据库可靠传递到下游。

