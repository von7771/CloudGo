# 恢复 K8s 内 MySQL 数据（schema + seata），并重启依赖 Seata 的微服务
# 用法: cd D:\wff\demo && .\deploy\k8s\restore-db.ps1

$ErrorActionPreference = "Stop"
$demoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
$schema = Join-Path $demoRoot "deploy/sql/schema.sql"
$seata = Join-Path $demoRoot "deploy/sql/seata.sql"

Write-Host ">>> 导入 schema.sql ..."
cmd /c "kubectl exec -i -n carpool deploy/mysql -- mysql -uroot -p123456 < `"$schema`""

Write-Host ">>> 导入 seata.sql ..."
cmd /c "kubectl exec -i -n carpool deploy/mysql -- mysql -uroot -p123456 < `"$seata`""

Write-Host ">>> 重启微服务 ..."
kubectl rollout restart -n carpool deploy/driver-service deploy/passenger-service deploy/trip-service

Write-Host ">>> 完成。等待约 30 秒后执行: kubectl get pods -n carpool"
