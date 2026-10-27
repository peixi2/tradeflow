# TradeFlow 架构说明

```text
Browser -> Vue 3 SPA -> REST/JWT -> Spring Security
                                -> Controller -> Service (@Transactional)
                                             -> JPA -> MySQL
                                             -> Outbox event (same transaction)
Docker multi-stage build -> Render HTTPS
```

## 一致性设计

- 支付回调：先按 `event_id` 查询，随后锁定订单并写入支付事件；数据库唯一约束是并发场景的最终防线。
- 库存预占：商品实体带 `@Version`，冲突时返回 HTTP 409，避免后提交覆盖先提交。
- 状态机：合法迁移集中在服务层，Controller 不能随意赋值状态。
- Outbox：订单状态和事件记录共享本地事务。演示保留 `PENDING/PUBLISHED` 数据用于说明投递过程。

## 索引

- `order_no`、`event_id`、`sku` 使用唯一索引。
- `(status, created_at)` 支撑运营台按履约状态查看最近订单。
- `(status, created_at)` 支撑 Outbox Publisher 扫描待投递事件。

## 部署取舍

前端静态资源由 Spring Boot 提供，免费环境只维护一个 Web Service。MySQL 独立于容器，Render 休眠和重新部署不会清空数据。正式生产应增加指标、慢查询、连接池监控、Outbox Publisher 和消息消费者。

