# SkyWalking 链路追踪集成

本项目采用 **Java Agent 无侵入** 方式接入 [Apache SkyWalking](https://skywalking.apache.org/)，适合 Spring Cloud 微服务 + Gateway + Feign + MySQL/Redis 的 Demo 环境。

## 架构

```
请求 → gateway → trip/passenger/driver/admin → MySQL/Redis/Feign
         │              │
         └──── SkyWalking Java Agent ────┘
                        │
                        ▼ gRPC :11800
                 skywalking-oap
                        │
                        ▼
                 skywalking-ui  (NodePort 30088)
```

- **OAP**：收集、聚合 Trace
- **UI**：拓扑图、链路详情、服务指标
- **Agent**：打进各微服务镜像，自动追踪 HTTP、Feign、JDBC、Redis 等

## 一、部署 SkyWalking（OAP + UI）

### 1. 导入镜像到 kind

```powershell
cd d:\wff\demo
.\deploy\k8s\import-skywalking-images.ps1
```

### 2. 启动 OAP / UI

```powershell
kubectl apply -f deploy/k8s/infra/skywalking.yaml
kubectl get pods -n carpool -l app=skywalking-oap
kubectl get pods -n carpool -l app=skywalking-ui
```

等两个 Pod 都是 `Running`。

### 3. 访问 UI

**Docker Desktop / kind 集群请用 port-forward**（NodePort 30088 在本地往往打不开）：

```powershell
kubectl port-forward -n carpool svc/skywalking-ui 8088:8080
```

浏览器打开：**http://localhost:8088**

> 若坚持用 NodePort，需确认 kind 是否映射了 30088；Demo 环境推荐 port-forward。

> Demo 使用 **H2 存储**，OAP 重启后历史链路会清空。生产环境应换 Elasticsearch / BanyanDB。

---

## 二、给微服务挂上 Agent

### 1. 构建带 Agent 的镜像

```powershell
cd d:\wff\demo
.\deploy\docker\build-images-skywalking.ps1
```

脚本会：

1. `docker pull apache/skywalking-java-agent:9.6.0-java21`（从 Docker Hub 拉 Agent，比 curl Apache 快得多）
2. `mvn package`
3. 多阶段构建，把 Agent COPY 进 5 个服务镜像

> 若曾卡在 `RUN curl ... agent.tgz` 超过 5 分钟，请 **Ctrl+C** 停掉，拉取最新代码后重跑本脚本。

### 2. 导入 kind 并重启 Pod

对每个服务（或一次性循环）：

```powershell
$services = @("gateway","passenger-service","driver-service","trip-service","admin-service")
foreach ($svc in $services) {
  docker save "carpool/${svc}:latest" -o "D:\wff\demo\${svc}.tar"
  docker cp "D:\wff\demo\${svc}.tar" desktop-control-plane:/var/${svc}.tar
  docker exec desktop-control-plane ctr -n k8s.io images import "/var/${svc}.tar"
}
kubectl apply -f deploy/k8s/apps/microservices.yaml
kubectl delete pod -n carpool --all
```

> `microservices.yaml` 已为每个服务配置 `SW_AGENT_NAME`、`SW_AGENT_COLLECTOR_BACKEND_SERVICES`。

### 3. 确认 Agent 已加载

```powershell
kubectl logs -n carpool deploy/gateway --tail=5
```

应看到类似：

```text
[skywalking] agent enabled, service=gateway, oap=skywalking-oap:11800
```

---

## 三、产生链路并查看

1. 保持 Gateway port-forward：
   ```powershell
   kubectl port-forward -n carpool svc/gateway 30080:8080
   ```
2. 发几个请求（登录、叫车、司机接单等）
3. 打开 SkyWalking UI → **Topology** / **Trace**
4. 应看到 `gateway` → `trip-service` → `passenger-service` 等调用链

---

## 四、本地 IDEA 调试（可选）

不挂 Agent 时正常运行；需要本地追踪时：

1. 下载 [Java Agent 10.2.0](https://archive.apache.org/dist/skywalking/java-agent/10.2.0/)
2. VM options 增加：
   ```
   -javaagent:D:/tools/skywalking-agent/agent/skywalking-agent.jar
   -DSW_AGENT_NAME=trip-service
   -DSW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800
   ```
3. 本地 port-forward OAP：
   ```powershell
   kubectl port-forward -n carpool svc/skywalking-oap 11800:11800
   ```

---

## 五、临时关闭 Agent

某 Pod 不想上报时，把环境变量改为：

```yaml
- name: SW_AGENT_ENABLED
  value: "false"
```

然后 `kubectl apply` + 重启 Pod。无需换镜像（Agent 仍在镜像里，但不会 `-javaagent` 加载）。

---

## 六、与现有组件的关系

| 组件 | SkyWalking 支持 |
|------|-----------------|
| Spring Cloud Gateway | Agent 自动追踪 |
| OpenFeign | Agent 自动追踪 |
| MyBatis / JDBC | Agent 自动追踪 |
| Redis | Agent 自动追踪 |
| Seata 分布式事务 | 可看到跨服务 Span；Seata 专用插件需额外配置 |
| Sentinel | 与 SkyWalking 可共存，职责不同（限流 vs 追踪） |

---

## 七、常见问题

| 问题 | 处理 |
|------|------|
| UI 无服务 | 先确认 OAP Running；微服务日志有 `agent enabled`；发几次 API |
| OAP CrashLoop | kind 未 import 镜像 → 跑 `import-skywalking-images.ps1` |
| 只有 gateway 有链路 | 其他服务仍用旧镜像 → 重新 build + import + delete pod |
| Agent 报连不上 OAP | 检查 `skywalking-oap:11800` 在集群内是否可达 |

---

## 版本说明

| 组件 | 版本 | 说明 |
|------|------|------|
| OAP / UI | **10.2.0** | 必须配 **BanyanDB**（10.x 已移除 H2） |
| BanyanDB | **0.8.0** | OAP 10.2 要求的存储版本（勿用 `-slim`，部分镜像源无此 tag） |
| Java Agent | **9.6.0** | `apache/skywalking-java-agent:9.6.0-java21` |

---

## 相关文件

| 文件 | 说明 |
|------|------|
| `deploy/k8s/infra/skywalking.yaml` | OAP + UI 部署 |
| `deploy/docker/Dockerfile.skywalking` | 带 Agent 的镜像 |
| `deploy/docker/build-images-skywalking.ps1` | 一键构建 5 个服务 |
| `deploy/k8s/apps/microservices.yaml` | `SW_AGENT_*` 环境变量 |
