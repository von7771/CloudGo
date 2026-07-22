$ErrorActionPreference = "Stop"
$node = "desktop-control-plane"

$images = @(
    "minio/minio:RELEASE.2024-10-02T17-50-41Z"
)

foreach ($img in $images) {
    $name = ($img -replace "[:/]", "-") + ".tar"
    $local = Join-Path $env:TEMP $name
    Write-Host "Pull & import $img ..."
    docker pull "$img"
    if ($LASTEXITCODE -ne 0) {
        throw "docker pull failed: $img"
    }
    docker save $img -o $local
    docker cp $local "${node}:/var/$name"
    docker exec $node ctr -n k8s.io images import "/var/$name"
    Remove-Item $local -Force -ErrorAction SilentlyContinue
}

Write-Host "Done. Run: kubectl apply -f deploy/k8s/infra/minio.yaml"
