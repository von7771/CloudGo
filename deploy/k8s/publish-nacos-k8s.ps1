# 将 K8s 版 Nacos 配置发布到集群内的 Nacos（需先 port-forward）
# kubectl port-forward -n carpool svc/nacos 8848:8848
$baseUrl = "http://127.0.0.1:8848/nacos/v1/cs/configs"
$dir = Join-Path $PSScriptRoot "../nacos"

$files = @(
    @{ DataId = "gateway.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "gateway.yml" },
    @{ DataId = "shared-common.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "k8s/shared-common.yml" },
    @{ DataId = "passenger-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "k8s/passenger-service.yml" },
    @{ DataId = "driver-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "k8s/driver-service.yml" },
    @{ DataId = "trip-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "k8s/trip-service.yml" },
    @{ DataId = "admin-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "k8s/admin-service.yml" },
    @{ DataId = "ai-assistant-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "k8s/ai-assistant-service.yml" },
    @{ DataId = "seataServer.properties"; Group = "SEATA_GROUP"; Type = "properties"; File = "k8s/seataServer.properties" }
)

foreach ($item in $files) {
    $path = Join-Path $dir $item.File
    if (-not (Test-Path $path)) {
        Write-Host "Skip missing $($item.File)"
        continue
    }
    $content = (Get-Content $path -Raw -Encoding UTF8) -replace '(?m)^\s*#.*$', ''
    $result = Invoke-RestMethod -Method Post -Uri $baseUrl -Body @{
        dataId  = $item.DataId
        group   = $item.Group
        content = $content.Trim()
        type    = $item.Type
    }
    Write-Host "Published $($item.DataId): $result"
}
