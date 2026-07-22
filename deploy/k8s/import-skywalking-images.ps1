$ErrorActionPreference = "Stop"
$node = "desktop-control-plane"

$images = @(
    "apache/skywalking-banyandb:0.8.0",
    "apache/skywalking-oap-server:10.2.0",
    "apache/skywalking-ui:10.2.0"
)

foreach ($img in $images) {
    $name = ($img -replace "[:/]", "-") + ".tar"
    $local = Join-Path $env:TEMP $name
    Write-Host "Pull & import $img ..."
    docker pull $img
    if ($LASTEXITCODE -ne 0) {
        throw "docker pull failed: $img"
    }
    docker save $img -o $local
    docker cp $local "${node}:/var/$name"
    docker exec $node ctr -n k8s.io images import "/var/$name"
    Remove-Item $local -Force -ErrorAction SilentlyContinue
}

Write-Host "Done. Run: kubectl apply -f deploy/k8s/infra/skywalking.yaml"
