# 天气动态调价

## 架构选择

当前天气能力集成在 **trip-service** 内（`weather` 包），与路线规划、计价同属下单链路，**无需单独微服务**。

适合拆成 `weather-service` 的时机：

- 多个业务线都要查天气
- 需要独立扩容、独立 SLA
- 要接入多家天气供应商

## 配置 OpenWeather API Key

**不要**把 Key 写进 Git。使用 K8s Secret 或本地 `openweather-local.yml`。

### K8s

```powershell
# 合并进已有 carpool-secrets（保留 amap-api-key）
kubectl create secret generic carpool-secrets -n carpool `
  --from-literal=amap-api-key=CHANGE_ME_AMAP_API_KEY `
  --from-literal=openweather-api-key=CHANGE_ME_OPENWEATHER_API_KEY `
  --dry-run=client -o yaml | kubectl apply -f -

kubectl rollout restart -n carpool deploy/trip-service
```

### 本地 IDEA

复制 `trip-service/src/main/resources/openweather-local.yml.example` 为 `openweather-local.yml`，填入你的 OpenWeather Key。

或设置环境变量：`OPENWEATHER_API_KEY`

## 接口

| 接口 | 说明 |
|------|------|
| `GET /api/map/weather?lat=&lon=` | 查询天气 + 价格倍率 |
| `GET /api/map/route?origin=&destination=` | 路线规划，**已含天气调价** |
| 乘客发单 | 使用调价后的 `estimatedAmount` |

## 调价规则（起点天气）

| 条件 | 倍率 |
|------|------|
| 晴/多云 | 1.0 |
| 小雨 Drizzle | 1.10 |
| 雨 Rain | 1.15 |
| 雷雨 Thunderstorm | 1.25 |
| 雪 Snow | 1.30 |
| 雾/霾 | 1.10 |
| 气温 ≥ 35℃ | ×1.05（叠加） |
| 气温 ≤ 0℃ | ×1.10（叠加） |
| 上限 | 1.50 |

天气 API 失败时倍率为 1.0，不影响发单。

## 更新 trip-service 镜像（K8s）

```powershell
mvn -pl trip-service -am package -DskipTests -q
docker build -t carpool/trip-service:latest -f deploy/docker/Dockerfile.quick --build-arg MODULE=trip-service .
docker save carpool/trip-service:latest -o D:\wff\demo\trip-service.tar
docker cp D:\wff\demo\trip-service.tar desktop-control-plane:/var/trip-service.tar
docker exec desktop-control-plane ctr -n k8s.io images import /var/trip-service.tar
kubectl delete pod -n carpool -l app=trip-service
```
