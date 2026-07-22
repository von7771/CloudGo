# 拼车司机微信小程序（uni-app）

Vue3 + Vite 的 uni-app 项目，面向微信小程序。

## 功能亮点

- **上线开关**：一键上线/下线，自动连接 WebSocket
- **数据看板**：待接单数、今日完单、位置上报倒计时
- **当前行程卡片**：步骤进度条 + 路线小地图 + 状态操作按钮
- **待接订单列表**：卡片式展示，新单震动提醒
- **60 秒位置上报**：优先 GPS，失败时使用演示路线
- **个人中心**：完单统计、接单提示

## 开发

```bash
cd driver-uniapp
npm install
npm run dev:mp-weixin
```

```bash
kubectl port-forward -n carpool svc/gateway 30080:8080
# trip-service 容器端口是 8081，不是 8080
kubectl port-forward -n carpool svc/trip-service 30081:8081
```

若集群已暴露 NodePort，也可**不 port-forward**，直接连 `ws://127.0.0.1:30081/ws/driver/trips`。

测试账号：`driver1` / `123456`

## 联调

1. 司机上线 → 乘客发单 → 司机接单
2. 按「到达 → 开始 → 完单」推进
3. 乘客详情页查看司机位置
