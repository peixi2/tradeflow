# TradeFlow 架构说明

TradeFlow 的架构目标不是堆叠中间件，而是在单体和单库范围内先建立清晰的一致性边界。支付、库存和订单都属于强业务约束；领域事件则是未来异步扩展的接口。当前版本优先证明事务和并发控制是否正确，再把消息投递留给独立组件。

```text
Browser -> Vue 3 SPA -> REST/JWT -> Spring Security
                                -> Controller -> Service (@Transactional)
                                             -> JPA -> MySQL
                                             -> Outbox event (same transaction)
Docker multi-stage build -> Render HTTPS
```

## 支付事务边界

```text
paymentCallback(eventId, orderNo, amount)
  1. exists(eventId)                -> 命中则快速返回 duplicate=true
  2. SELECT order FOR UPDATE        -> 串行化同一订单的支付更新
  3. compare callback/order amount  -> 拒绝错单或金额篡改
  4. INSERT payment_event           -> event_id 唯一索引兜底并发重复写
  5. UPDATE order status            -> payment_status=PAID, status=PAID
  6. INSERT outbox_event            -> PaymentConfirmed, status=PENDING
  7. COMMIT                         -> 三类数据同时可见
```

“先查再写”只减少常见重复请求的成本，不能单独保证幂等，因为两个事务可能同时查到不存在。真正的最终防线是数据库唯一约束。订单行锁解决的是另一类问题：即使两个事件 ID 不同，对同一订单的状态判断和更新也必须串行执行。金额校验则阻止合法订单号与错误金额组成的回调进入账务数据。

`saveAndFlush` 让支付流水尽早发送到数据库，唯一约束冲突会在事务尚未提交前暴露。支付流水、订单状态和 Outbox 任何一步失败，Spring 事务都会回滚整个操作。

## 库存并发与状态机

库存使用乐观锁而不是把所有商品读取都升级为行锁。`Product.version` 会加入更新条件；两个事务读取相同版本后，只有一个能够成功更新，另一个得到乐观锁冲突并映射为 HTTP 409。这个选择适合冲突概率较低、读多写少的演示场景。高并发热点 SKU 需要压测后再决定是否切换为原子扣减 SQL、分段库存或排队串行化。

订单状态机允许 `CREATED -> PAID -> RESERVED -> SHIPPED -> COMPLETED`，并在部分阶段允许取消。规则保存在服务层的迁移表中，Controller 只提交目标状态，不能直接修改实体。这样可以避免“前端隐藏按钮”被误当成业务约束，也为后续消息消费和批处理复用规则保留入口。

## Outbox 的保证范围

Outbox 解决数据库更新与消息发送的双写窗口。当前事务只保证业务状态和事件记录一起落库；`PENDING` 代表待投递，不代表 Kafka 或 RocketMQ 已收到。完整生产链路还需要：

1. Publisher 按 `(status, created_at)` 索引批量锁定待投递事件。
2. 消息系统确认成功后把事件标记为 `PUBLISHED`。
3. 失败事件按退避策略重试，并监控长期滞留记录。
4. 消费者继续使用事件 ID 或业务键去重，因为消息投递通常只能做到至少一次。

## 索引

- `order_no`、`event_id`、`sku` 使用唯一索引。
- `(status, created_at)` 支撑运营台按履约状态查看最近订单。
- `(status, created_at)` 支撑 Outbox Publisher 扫描待投递事件。

索引服务于已经存在的访问路径，而不是为了数量好看。运营台主要按状态和时间查看订单，未来 Publisher 也需要按状态和创建时间扫描。若数据量扩大，应通过执行计划验证选择性，并避免无边界地返回全部订单；当前接口最多展示最近 50 条。

## 部署与运行边界

Vue 静态资源由 Spring Boot 提供，免费环境只维护一个 Render Web Service。Docker 构建分为前端、后端和运行阶段，运行镜像不包含 Node 构建依赖。MySQL 位于独立 TiDB Cloud 服务，Render 休眠或重新部署不会清空业务数据。

当前架构适合面试演示和中小规模单体业务，但尚未提供生产级 SLO。上线前需要补充连接池与慢查询指标、事务冲突率、Outbox 滞留时长、审计日志、备份恢复演练和基于真实流量模型的压测。Redis、MQ、读写分离只能在指标证明瓶颈后引入。

