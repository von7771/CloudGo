# 重新构建并部署单个微服务（避免 Docker :latest 缓存导致 K8s 仍跑旧镜像）
# 用法: .\deploy\k8s\redeploy-service.ps1 driver-service
#       .\deploy\k8s\redeploy-service.ps1 passenger-service trip-service gateway

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("gateway", "passenger-service", "driver-service", "trip-service", "admin-service", "ai-assistant-service")]
    [string]$Service
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "../..")
$tag = Get-Date -Format "yyyyMMdd-HHmmss"
$image = "carpool/${Service}:$tag"

Write-Host ">>> mvn package $Service ..."
Push-Location $root
mvn clean package -DskipTests -pl $Service -am -q

Write-Host ">>> docker build $image ..."
docker build -t $image -f (Join-Path $root "deploy/docker/Dockerfile.quick") --build-arg MODULE=$Service $root

Write-Host ">>> kubectl set image ..."
kubectl set image -n carpool "deployment/$Service" "${Service}=${image}"
kubectl rollout status -n carpool "deployment/$Service" --timeout=120s

Write-Host ">>> 完成: $Service -> $image"
Write-Host ">>> 请确认已运行: .\deploy\k8s\start-port-forward.ps1"
Pop-Location
