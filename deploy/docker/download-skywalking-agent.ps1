# 在本机下载 SkyWalking Java Agent（避免 Docker build 内 curl 极慢/卡死）
$ErrorActionPreference = "Stop"
$version = "9.6.0"
$tarName = "apache-skywalking-java-agent-$version.tgz"
$cacheDir = Join-Path $PSScriptRoot ".cache"
$dest = Join-Path $cacheDir $tarName

New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null

if (Test-Path $dest) {
    $sizeMb = [math]::Round((Get-Item $dest).Length / 1MB, 1)
    if ($sizeMb -gt 5) {
        Write-Host ("SkyWalking Agent cached: " + $dest + " [" + $sizeMb + " MB], skip download")
        exit 0
    }
    Remove-Item $dest -Force
}

$urls = @(
    "https://archive.apache.org/dist/skywalking/java-agent/$version/$tarName",
    "https://downloads.apache.org/skywalking/java-agent/$version/$tarName"
)

foreach ($url in $urls) {
    Write-Host "Downloading $url ..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing -TimeoutSec 600
        $sizeMb = [math]::Round((Get-Item $dest).Length / 1MB, 1)
        if ($sizeMb -lt 5) {
            throw ("Download too small: " + $sizeMb + " MB")
        }
        Write-Host ("OK: " + $dest + " [" + $sizeMb + " MB]")
        exit 0
    } catch {
        Write-Warning "Failed: $($_.Exception.Message)"
        if (Test-Path $dest) { Remove-Item $dest -Force }
    }
}

throw "Cannot download SkyWalking Agent $version. Save manually to: $dest"
