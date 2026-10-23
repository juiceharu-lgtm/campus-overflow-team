[CmdletBinding()]
param(
    [string]$ComposeFile = (Join-Path $PSScriptRoot '..\deploy\docker-compose.yml')
)

$ErrorActionPreference = 'Stop'

function Get-ToolVersion {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [Parameter(Mandatory)] [string[]]$Arguments
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        return [pscustomobject]@{ Tool = $Name; Available = $false; Detail = 'PATH 中不可用' }
    }

    $detail = (& $command.Source @Arguments 2>&1 | Select-Object -First 1 | Out-String).Trim()
    return [pscustomobject]@{ Tool = $Name; Available = $true; Detail = $detail }
}

$tools = @(
    Get-ToolVersion -Name 'java' -Arguments @('-version')
    Get-ToolVersion -Name 'javac' -Arguments @('-version')
    Get-ToolVersion -Name 'mvn' -Arguments @('-v')
    Get-ToolVersion -Name 'node' -Arguments @('--version')
    Get-ToolVersion -Name 'npm' -Arguments @('--version')
    Get-ToolVersion -Name 'pnpm' -Arguments @('--version')
    Get-ToolVersion -Name 'docker' -Arguments @('--version')
)

$tools | Format-Table -AutoSize

$composePath = [IO.Path]::GetFullPath($ComposeFile)
if (-not (Test-Path -LiteralPath $composePath)) {
    throw "Compose 文件不存在：$composePath"
}

$docker = $tools | Where-Object Tool -eq 'docker'
if (-not $docker.Available) {
    Write-Warning 'Docker CLI 不可用；未执行 daemon、Compose、端口或容器检查。'
    exit 2
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Warning 'Docker daemon 未就绪；请启动 Docker Desktop 后重试。'
    exit 3
}

docker compose version
docker compose -f $composePath config --quiet
if ($LASTEXITCODE -ne 0) {
    throw 'Compose 配置校验失败。'
}

'项目使用端口的当前监听状态：'
$portStatus = foreach ($port in 3306, 6379, 8080, 80, 9090, 3000) {
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
    [pscustomobject]@{ Port = $port; Listening = ($listeners.Count -gt 0); ProcessIds = ($listeners.OwningProcess -join ',') }
}
$portStatus | Format-Table -AutoSize

'同名容器（只读）：'
docker ps -a --filter 'name=^/co-(mysql|redis|backend|web|prometheus|grafana)$' --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

'项目相关数据卷（只读）：'
docker volume ls --filter 'name=campus' --format 'table {{.Name}}\t{{.Driver}}'

'预检查通过。此脚本不会启动、停止或删除容器和数据卷。'
