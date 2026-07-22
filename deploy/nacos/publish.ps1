$baseUrl = "http://127.0.0.1:8848/nacos/v1/cs/configs"
$dir = $PSScriptRoot

$files = @(
    @{ DataId = "shared-common.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "shared-common.yml" },
    @{ DataId = "passenger-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "passenger-service.yml" },
    @{ DataId = "driver-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "driver-service.yml" },
    @{ DataId = "trip-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "trip-service.yml" },
    @{ DataId = "admin-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "admin-service.yml" },
    @{ DataId = "ai-assistant-service.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "ai-assistant-service.yml" },
    @{ DataId = "gateway.yml"; Group = "DEFAULT_GROUP"; Type = "yaml"; File = "gateway.yml" },
    @{ DataId = "seataServer.properties"; Group = "SEATA_GROUP"; Type = "properties"; File = "seataServer.properties" },
    @{ DataId = "passenger-service-flow-rules"; Group = "SENTINEL_GROUP"; Type = "json"; File = "sentinel/passenger-service-flow-rules.json" },
    @{ DataId = "passenger-service-degrade-rules"; Group = "SENTINEL_GROUP"; Type = "json"; File = "sentinel/passenger-service-degrade-rules.json" },
    @{ DataId = "trip-service-flow-rules"; Group = "SENTINEL_GROUP"; Type = "json"; File = "sentinel/trip-service-flow-rules.json" },
    @{ DataId = "trip-service-degrade-rules"; Group = "SENTINEL_GROUP"; Type = "json"; File = "sentinel/trip-service-degrade-rules.json" },
    @{ DataId = "gateway-flow-rules"; Group = "SENTINEL_GROUP"; Type = "json"; File = "sentinel/gateway-flow-rules.json" }
)

foreach ($item in $files) {
    $path = Join-Path $dir $item.File
    if (-not (Test-Path $path)) {
        Write-Host "Skip missing $($item.File)"
        continue
    }
    $content = Get-Content $path -Raw -Encoding UTF8
    $content = ($content -split "`n" | Where-Object { $_ -notmatch '^\s*#' }) -join "`n"
    $result = Invoke-RestMethod -Method Post -Uri $baseUrl -Body @{
        dataId  = $item.DataId
        group   = $item.Group
        content = $content.Trim()
        type    = $item.Type
    }
    Write-Host "Published $($item.DataId): $result"
}
