# H5 演示页面

在浏览器直接打开本地 HTML 文件即可（或通过任意静态服务器托管）。

| 文件 | 用途 |
|------|------|
| `index.html` | 乘客发单 |
| `driver.html` | 司机登录、上线、**每 60 秒上报位置**、WebSocket 收单 |
| `track.html` | **地图每 60 秒刷新**司机位置（全量在线 / 按行程追踪） |

## 前置条件

1. Gateway 可访问（K8s 示例）：
   ```powershell
   kubectl port-forward -n carpool svc/gateway 30080:8080
   ```
2. WebSocket 收单（可选）：
   ```powershell
   kubectl port-forward -n carpool svc/trip-service 30081:8081
   ```

## 演示流程：位置上报 + 地图追踪

1. 打开 `driver.html` → 登录 `driver1` → 上线 → **开始上报位置(60s)**
2. 打开 `track.html` → 模式选「全量在线司机」→ **开始追踪(60s)**，地图上会看到司机 marker 每分钟移动
3. 若要按行程追踪：
   - `index.html` 乘客发单，记下 `tripId`
   - 司机在 Postman 或接口里接单
   - `track.html` 切到「按行程追踪」，填 `tripId`，乘客登录后开始追踪

## 相关 API

| 接口 | 说明 |
|------|------|
| `POST /api/driver/location?location=lng,lat` | 司机上报位置 |
| `GET /api/map/drivers/locations` | 所有在线司机位置（公开） |
| `GET /api/passenger/trips/{id}/driver-location` | 乘客查本单司机位置 |

## 重新部署后端

修改了 `driver-service` 与 `trip-service` 后需重新构建镜像并 rollout：

```powershell
cd D:\wff\demo
mvn -pl driver-service,trip-service -am package -DskipTests -q
docker build -t carpool/driver-service:latest -f deploy/docker/Dockerfile.quick --build-arg MODULE=driver-service .
docker build -t carpool/trip-service:latest -f deploy/docker/Dockerfile.quick --build-arg MODULE=trip-service .
kubectl rollout restart -n carpool deploy/driver-service deploy/trip-service
```
