$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "../..")
$dockerfile = Join-Path $root "deploy/docker/Dockerfile.skywalking"
$services = @("gateway", "passenger-service", "driver-service", "trip-service", "admin-service", "ai-assistant-service")
$agentImage = "apache/skywalking-java-agent:9.6.0-java21"

Write-Host "1/3 Pull SkyWalking agent base image (once, from Docker Hub)..."
docker pull $agentImage
if ($LASTEXITCODE -ne 0) {
    throw "docker pull failed: $agentImage"
}

Write-Host "2/3 Maven package (all modules)..."
Push-Location $root
mvn package -DskipTests -q
Pop-Location

Write-Host "3/3 Docker build with SkyWalking agent..."
foreach ($svc in $services) {
    Write-Host "Building carpool/$svc (skywalking) ..."
    docker build --build-arg MODULE=$svc --build-arg SW_AGENT_IMAGE=$agentImage -t "carpool/${svc}:latest" -f $dockerfile $root
    if ($LASTEXITCODE -ne 0) {
        throw "Docker build failed for $svc"
    }
}
Write-Host "Done. Next: import images to kind and restart pods (see deploy/docs/skywalking.md)"
