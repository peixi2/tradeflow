# Java 岗位要求与项目证据

本文将 2027 届主流互联网公司 Java 后端校招岗位的常见要求映射到 TradeFlow 的可验证实现。调研时间为 2026 年 10 月，信息来源包括字节跳动、腾讯、阿里巴巴、美团和京东校园招聘入口，以及牛客公开面经。不同岗位的具体要求以投递当天的职位描述为准。

## 高频要求

| 岗位要求 | TradeFlow 中的证据 | 面试演示入口 |
| --- | --- | --- |
| Java 基础与 Spring 生态 | Java 21、Spring Boot 3.3、Spring Security、Spring Data JPA | 登录、统一异常、订单接口 |
| MySQL 与事务 | 支付回调在事务内写支付流水、更新订单并写 Outbox | 对同一事件连续回调两次 |
| 并发与一致性 | 订单查询使用行锁，支付事件唯一约束，库存使用 `@Version` 乐观锁 | 支付幂等、库存预占 |
| 业务建模 | 订单状态机限制合法流转，非法跳转返回明确错误 | 订单中心逐步操作 |
| 分布式系统意识 | 本地事务 Outbox 明确业务提交与异步投递边界 | 事件中心查看 PENDING 与 PUBLISHED |
| 工程化与交付 | 自动化测试、多阶段 Docker、健康检查、云部署 | `/health`、GitHub Actions 可继续扩展 |
| 前后端协作 | Vue 3 运营工作台通过 JWT 调用 REST API | 桌面与移动端完整工作流 |

## 可讲亮点

1. 支付幂等不是在 Controller 里写一个 `if`，而是由事件唯一键、数据库事务和订单行锁共同保证。
2. Outbox 解决的是“业务已提交但消息未发送”的一致性窗口；当前项目真实实现了事务内落库，没有虚构 Kafka 已上线。
3. 状态机把领域规则收敛在服务层，前端按钮只是交互提示，后端仍会拒绝非法跳转。
4. 演示数据覆盖六种订单状态，面试官打开页面即可观察业务分布并执行真实写操作。

## 下一步演进

- 增加 Outbox Publisher，并接入 Kafka 或 RocketMQ；消费者继续以事件 ID 去重。
- 对热点 SKU 引入 Redis 缓存和 Lua 原子扣减，再通过数据库校准库存。
- 使用 Testcontainers 覆盖真实 MySQL 并发场景，增加压测与 Prometheus 指标。

## 调研入口

- 字节跳动校园招聘：https://jobs.bytedance.com/campus
- 腾讯校园招聘：https://join.qq.com
- 阿里巴巴校园招聘：https://campus.alibaba.com
- 美团校园招聘：https://zhaopin.meituan.com/web/campus
- 京东校园招聘：https://campus.jd.com
- 牛客公开面经：https://www.nowcoder.com
