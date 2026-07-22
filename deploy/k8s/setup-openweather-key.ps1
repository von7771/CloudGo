# 配置 OpenWeather API Key（切勿提交 openweather.env 到 Git）
# 用法:
#   .\deploy\k8s\setup-openweather-key.ps1
#   .\deploy\k8s\setup-openweather-key.ps1 -ApiKey "your-key"
# 说明: 仅 patch openweather-api-key，不会覆盖 deepseek/amap 等其他 key

param(
    [string]$ApiKey = ""
)

$ErrorActionPreference = "Stop"
$ns = "carpool"
$secretName = "carpool-secrets"
$envFile = Join-Path $PSScriptRoot "../local/openweather.env"

if (-not $ApiKey) {
    if (-not (Test-Path $envFile)) {
        Write-Error "未提供 -ApiKey，且找不到 $envFile`n请复制 openweather.env.example 为 openweather.env 并填入 Key"
    }
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*OPENWEATHER_API_KEY\s*=\s*(.+)\s*$') {
            $ApiKey = $Matches[1].Trim()
        }
    }
}

if (-not $ApiKey) {
    Write-Error "OPENWEATHER_API_KEY 为空，请检查 $envFile 或使用 -ApiKey 参数"
}

Write-Host ">>> 检查 Secret $secretName ..."
$exists = kubectl get secret $secretName -n $ns 2>$null
if (-not $exists) {
    Write-Host ">>> 创建 Secret（仅 openweather-api-key）..."
    kubectl create secret generic $secretName `
        --from-literal=openweather-api-key=$ApiKey `
        -n $ns
} else {
    Write-Host ">>> Patch openweather-api-key（保留其他 key）..."
    $patchFile = Join-Path $env:TEMP "carpool-openweather-patch.json"
    @"
{"stringData":{"openweather-api-key":"$ApiKey"}}
"@ | Set-Content -Path $patchFile -Encoding UTF8 -NoNewline
    kubectl patch secret $secretName -n $ns --type=merge --patch-file $patchFile
    Remove-Item $patchFile -ErrorAction SilentlyContinue
}

Write-Host ">>> 重启 trip-service ..."
kubectl rollout restart -n $ns deployment/trip-service
kubectl rollout status -n $ns deployment/trip-service --timeout=120s

Write-Host "OK 天气 Key 已写入集群。以后重建集群后只需再运行: .\deploy\k8s\setup-openweather-key.ps1"
