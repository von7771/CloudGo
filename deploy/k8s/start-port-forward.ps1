# 本地访问 K8s 服务（小程序 / Postman 必需）
# 用法: cd d:\wff\demo && .\deploy\k8s\start-port-forward.ps1
# 会打开两个新窗口，请勿关闭

$ErrorActionPreference = "Stop"
Write-Host ">>> 检查 carpool 命名空间 Pod ..."
$pods = kubectl get pods -n carpool --no-headers 2>&1
if ($LASTEXITCODE -ne 0) { throw "kubectl 失败，请确认 Docker Desktop Kubernetes 已启动" }
$notReady = $pods | Where-Object { $_ -notmatch "Running" -and $_ -notmatch "Completed" }
if ($notReady) {
    Write-Host $pods
    throw "有 Pod 未 Running，请先 kubectl get pods -n carpool"
}

Write-Host ">>> 启动 Gateway  port-forward  127.0.0.1:30080 -> gateway:8080"
Start-Process powershell -ArgumentList @(
    "-NoExit", "-Command",
    "Write-Host 'Gateway API - 保持此窗口打开'; kubectl port-forward -n carpool svc/gateway 30080:8080"
)

Start-Sleep -Seconds 2

Write-Host ">>> 启动 trip-service port-forward 127.0.0.1:30081 -> trip-service:8081 (WebSocket)"
Start-Process powershell -ArgumentList @(
    "-NoExit", "-Command",
    "Write-Host 'Driver WebSocket - 保持此窗口打开'; kubectl port-forward -n carpool svc/trip-service 30081:8081"
)

Start-Sleep -Seconds 3

Write-Host ">>> 探测 Gateway ..."
try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:30080/api/driver/auth/login?username=driver1&password=123456" `
        -Method POST -UseBasicParsing -TimeoutSec 15
    if ($r.StatusCode -eq 200) {
        Write-Host "OK  Gateway 可访问: http://127.0.0.1:30080"
        Write-Host "OK  WebSocket:       ws://127.0.0.1:30081/ws/driver/trips"
        Write-Host ""
        Write-Host "现在可以打开微信开发者工具测试小程序。两个 port-forward 窗口请保持运行。"
    }
} catch {
    Write-Host "WARN 探测失败: $($_.Exception.Message)"
    Write-Host "请稍等几秒后手动访问 http://127.0.0.1:30080"
}
