# 拼车管理后台 (admin-web)

Vue 3 + Vite + TypeScript + Element Plus

## 功能

- 管理员登录
- 数据概览 Dashboard
- 行程 / 司机 / 乘客管理
- 司机审核、乘客封禁
- 计价规则配置
- 在线司机位置地图（内嵌 Leaflet + 高德瓦片，60 秒刷新，多司机多点标注）

## 启动

### 1. 确保后端 Gateway 可访问

```powershell
kubectl port-forward -n carpool svc/gateway 30080:8080
```

### 2. 安装依赖并启动前端

```powershell
cd D:\wff\demo\admin-web
npm install
npm run dev
```

浏览器打开 http://localhost:5173

默认账号：`admin` / `admin123`

## 开发说明

- `vite.config.ts` 已将 `/api` 代理到 `http://127.0.0.1:30080`，开发时无跨域问题
- 生产构建：`npm run build`，产物在 `dist/`，需配置 `VITE_API_BASE` 指向 Gateway

## 目录结构

```
src/
  api/          # 接口封装
  layouts/      # 后台布局
  router/       # 路由
  stores/       # Pinia 状态
  types/        # TypeScript 类型
  utils/        # Axios 封装
  views/        # 页面
```
