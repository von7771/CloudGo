# MinIO 对象存储集成

MinIO 用于 **司机证件图片** 与 **行程完单电子凭证**（由 Kafka 消费者生成）。

## 架构

```
driver-uniapp 上传证件 → driver-service → MinIO drivers/{id}/...
完单 Kafka 事件 → admin-service TripReceiptHandler → MinIO trips/{id}/receipt.json
admin-web 审核 → 预签名 URL 预览图片
```

## 一、部署 MinIO

```powershell
cd d:\wff\demo
.\deploy\k8s\import-minio-images.ps1
kubectl apply -f deploy/k8s/infra/minio.yaml
kubectl get pods -n carpool -l app=minio
kubectl get jobs -n carpool minio-init-bucket
```

控制台（可选）：

```powershell
kubectl port-forward -n carpool svc/minio 9001:9001
```

浏览器打开 http://localhost:9001 ，使用你在 `deploy/k8s/infra/minio.yaml` 里设置的账号密码。

## 二、数据库迁移

已有库需执行：

```powershell
kubectl exec -i -n carpool deploy/mysql -- mysql -uroot -p<MYSQL_ROOT_PASSWORD> < deploy/sql/migrate-minio.sql
```

## 三、发布配置并重建服务

```powershell
kubectl port-forward -n carpool svc/nacos 8848:8848
cd deploy/k8s; .\publish-nacos-k8s.ps1

mvn -pl driver-service,admin-service -am package -DskipTests -q
docker build -t carpool/driver-service:latest -f deploy/docker/Dockerfile.quick --build-arg MODULE=driver-service .
docker build -t carpool/admin-service:latest -f deploy/docker/Dockerfile.quick --build-arg MODULE=admin-service .
# import kind + delete pods（同 kafka.md）
kubectl apply -f deploy/k8s/apps/microservices.yaml
```

## 四、API

### 司机上传证件

```http
POST /api/driver/documents?docType=license
Authorization: Bearer {driver_token}
Content-Type: multipart/form-data
file: (图片)
```

`docType`: `license` | `id_card`

```http
GET /api/driver/documents/status
Authorization: Bearer {driver_token}
```

### 管理端

- 司机列表返回 `licenseImageUrl` / `idCardImageUrl`（预签名，1 小时有效）
- 完单后：`GET /api/admin/trips/{tripId}/receipt-url`

## 五、测试账号

- `driver3/<password>` — PENDING，可登录上传证件，不能上线接单
- admin 后台审核通过后可上线
