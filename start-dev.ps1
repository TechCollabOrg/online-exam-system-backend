# 仅启动后端（推荐日常使用仓库根目录的 start-all.ps1 一键启前后端）
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
& (Join-Path $Root "start-all.ps1") -BackendOnly @args
