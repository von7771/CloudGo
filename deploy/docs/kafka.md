# Kafka 行程事件集成

本项目使用 **Redpanda**（Kafka 协议兼容）在 K8s 内广播行程领域事件，实现 trip-service 与 admin-service 的异步解耦。

## 架构

```
trip-service (发单/接单/完单...)
       │ TripEventPublisher
       ▼
  topic: trip.events  (kafka:9092)
       │
       ├── TripEventMetricsCollector（按状态计数）
       ├── TripNotificationSimulator（模拟乘客/司机通知日志）
       └── TripReceiptHandler（完单 → MinIO 电子凭证）
```

- **Producer**：`trip-service`，每次写 `TripEvent` 表时同步发 Kafka
- **Consumer**：`admin-service` 多 Handler 分发消费，暴露 `/api/admin/kafka/stats`
- **Redis Pub/Sub**：仍保留，司机 WebSocket 实时收单不受影响

## 一、部署 Kafka（Redpanda）

### 1. 导入镜像到 kind（Docker Desktop K8s）

```powershell
cd d:\wff\demo
.\deploy\k8s\import-kafka-images.ps1
```

### 2. 启动

```powershell
kubectl apply -f deploy/k8s/infra/kafka.yaml
kubectl get pods -n carpool -l app=kafka
```

等 Pod 为 `Running`。

### 3. 验证（可选）

```powershell
kubectl exec -n carpool deploy/kafka -- rpk topic list
```

首次发单后会自动创建 `trip.events` topic。

---

## 二、发布 Nacos 配置

Kafka 相关配置在 `deploy/nacos/k8s/trip-service.yml` 与 `k8s/admin-service.yml`。

```powershell
kubectl port-forward -n carpool svc/nacos 8848:8848
# 另开终端
cd d:\wff\demo\deploy\k8s
.\publish-nacos-k8s.ps1
```

---

## 三、构建并更新微服务镜像

Kafka 代码在 **trip-service** 与 **admin-service**，需重新构建这两个镜像：

```powershell
cd d:\wff\demo
mvn -pl trip-service,admin-service -am package -DskipTests -q

docker build -t carpool/trip-service:latest -f deploy/docker/Dockerfile.quick --build-arg MODULE=trip-service .
docker build -t carpool/admin-service:latest -f deploy/docker/Dockerfile.quick --build-arg MODULE=admin-service .

# 导入 kind（与 trip-service redeploy 相同流程）
$node = "desktop-control-plane"
foreach ($svc in @("trip-service", "admin-service")) {
  docker save "carpool/${svc}:latest" -o "$env:TEMP/${svc}.tar"
  docker cp "$env:TEMP/${svc}.tar" "${node}:/var/${svc}.tar"
  docker exec $node ctr -n k8s.io images import "/var/${svc}.tar"
}

kubectl delete pod -n carpool -l app=trip-service
kubectl delete pod -n carpool -l app=admin-service
```

或使用 `deploy/docker/build-images.ps1` 全量构建。

---

## 四、验证

### 1. 发一笔行程

Gateway port-forward 后，乘客发单、司机接单/完单（与平时联调相同）。

### 2. 看 admin-service 日志

```powershell
kubectl logs -n carpool deploy/admin-service --tail=50
```

应出现类似：

```
[Kafka] tripId=1 CREATED -> DISPATCHING operator=SYSTEM remark=...
```

### 3. Kafka 消费统计

管理员登录后：

```http
GET http://127.0.0.1:30080/api/admin/kafka/stats
Authorization: Bearer <admin-token>
```

响应示例：

```json
{
  "eventsReceived": 5,
  "completedEvents": 1,
  "topic": "trip.events",
  "consumerGroup": "admin-service"
}
```

---

## 五、本地 IDEA 调试（可选）

本地 Nacos 配置默认 `app.kafka.enabled: false`，不连 Kafka 也能跑。

若要在 IDEA 里调试 Kafka：

1. `kubectl port-forward -n carpool svc/kafka 9092:9092`
2. 在 Nacos 本地 `trip-service.yml` 改为：
   - `app.kafka.enabled: true`
   - 去掉 `spring.autoconfigure.exclude` 中的 Kafka
   - `spring.kafka.bootstrap-servers: 127.0.0.1:9092`

---

## 六、配置说明

| 配置项 | K8s 值 | 说明 |
|--------|--------|------|
| `spring.kafka.bootstrap-servers` | `kafka:9092` | 集群内 DNS |
| `app.kafka.topic` | `trip.events` | 行程事件 topic |
| `app.kafka.enabled` | `true` | K8s 开启；本地默认 `false` |

消息体见 `common` 模块 `TripEventMessage`（tripId、状态、乘客/司机 ID、金额、时间等）。

---

## 七、故障排查

| 现象 | 处理 |
|------|------|
| trip-service 启动报 Kafka 连接失败 | 确认 kafka Pod Running；Nacos 已 publish k8s 版配置 |
| admin 收不到事件 | `kubectl logs deploy/admin-service` 看 consumer 报错；确认 trip-service 已 rebuild |
| `eventsReceived` 始终 0 | 先发单产生事件；确认两个服务 `APP_KAFKA_ENABLED=true` |
| kind 拉镜像失败 | 运行 `import-kafka-images.ps1` |
