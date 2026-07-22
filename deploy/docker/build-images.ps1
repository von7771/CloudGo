$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "../..")
$services = @("gateway", "passenger-service", "driver-service", "trip-service", "admin-service", "ai-assistant-service")

foreach ($svc in $services) {
    Write-Host "Building carpool/$svc ..."
    docker build --build-arg MODULE=$svc -t "carpool/${svc}:latest" -f (Join-Path $root "deploy/docker/Dockerfile") $root
}
Write-Host "Done. Images: $($services | ForEach-Object { "carpool/$_:latest" })"
