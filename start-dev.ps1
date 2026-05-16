# 本地开发启动脚本：从 env.local 加载 MySQL / Redis 密码后启动后端
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$envFile = Join-Path $PSScriptRoot "env.local"
if (-not (Test-Path $envFile)) {
    Write-Host "未找到 env.local，请先复制 env.local.example 为 env.local 并填写密码。" -ForegroundColor Yellow
    exit 1
}

Get-Content $envFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) { return }
    $name = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim().Trim([char]0xFEFF, [char]0x00)
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

Write-Host "已加载 env.local（MYSQL_PASSWORD、REDIS_PASSWORD 等）" -ForegroundColor Green

$jar = Join-Path $PSScriptRoot "target/exam-1.0-SNAPSHOT.jar"
if (Test-Path $jar) {
    Write-Host "启动: java -jar target/exam-1.0-SNAPSHOT.jar" -ForegroundColor Cyan
    java -jar $jar
} else {
    Write-Host "未找到 JAR，使用 Maven 启动（mvn spring-boot:run）..." -ForegroundColor Cyan
    mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
}
