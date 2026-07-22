# 配置 DeepSeek API Key（切勿提交到 Git）
# 用法: .\deploy\k8s\setup-deepseek-key.ps1 -ApiKey "sk-xxx"
# 说明: 仅 patch deepseek-api-key，不会覆盖 amap/openweather 等其他 key

param(
    [Parameter(Mandatory = $true)]
    [string]$ApiKey
)

$ErrorActionPreference = "Stop"
$ns = "carpool"
$secretName = "carpool-secrets"

Write-Host ">>> 检查 Secret $secretName ..."
$exists = kubectl get secret $secretName -n $ns 2>$null
if (-not $exists) {
    Write-Host ">>> 创建 Secret（仅 deepseek-api-key）..."
    kubectl create secret generic $secretName `
        --from-literal=deepseek-api-key=$ApiKey `
        -n $ns
} else {
    Write-Host ">>> Patch deepseek-api-key（保留其他 key）..."
    $patch = "{`"stringData`":{`"deepseek-api-key`":`"$ApiKey`"}}"
    kubectl patch secret $secretName -n $ns -p $patch
}

Write-Host ">>> 重启 ai-assistant-service ..."
kubectl rollout restart -n $ns deployment/ai-assistant-service
kubectl rollout status -n $ns deployment/ai-assistant-service --timeout=120s

Write-Host "OK 若尚未部署镜像，请运行: .\deploy\k8s\redeploy-service.ps1 ai-assistant-service"
