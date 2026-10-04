$ErrorActionPreference = 'Stop'
try {
    $projectPath = Split-Path $PSScriptRoot -Parent
    Set-Location -LiteralPath $projectPath
    if (!(Test-Path -LiteralPath (Join-Path $projectPath 'database.local.properties'))) {
        throw 'Please run setup-database.bat once before starting the website.'
    }
    $sqlService = Get-Service -Name 'MSSQL$SQLEXPRESS01'
    if ($sqlService.Status -ne 'Running') {
        throw 'Start SQL Server (SQLEXPRESS01) in Windows Services, then run this file again.'
    }
    # SQL Express uses a dynamic port. Read it on every launch after PC restarts.
    $tcp = Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS01\MSSQLServer\SuperSocketNetLib\Tcp\IPAll'
    $sqlPort = if ($tcp.TcpPort) { $tcp.TcpPort } else { $tcp.TcpDynamicPorts }
    if ($sqlPort -notmatch '^\d+$' -or [int]$sqlPort -lt 1) {
        throw 'SQLEXPRESS01 has no TCP port. Enable TCP/IP and restart that SQL Server instance.'
    }
    $env:CARRENTAL_DB_PORT = $sqlPort
    $ideaPath = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1'
    if (Test-Path -LiteralPath "$ideaPath\jbr\bin\java.exe") {
        $env:JAVA_HOME = "$ideaPath\jbr"
    }
    $mavenPath = "$ideaPath\plugins\maven\lib\maven3\bin\mvn.cmd"
    if (!(Test-Path -LiteralPath $mavenPath)) { $mavenPath = (Get-Command mvn.cmd -ErrorAction Stop).Source }
    Write-Host "Starting helaCabs with SQL Server (port $sqlPort)."
    Write-Host 'After startup, open http://localhost:8080. Press Ctrl+C here to stop.'
    & $mavenPath spring-boot:run
    exit $LASTEXITCODE
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
