$ErrorActionPreference = 'Stop'
$projectPath = Split-Path $PSScriptRoot -Parent
$credentialPath = Join-Path $projectPath 'database.local.properties'
$sqlPath = Join-Path $projectPath 'src\main\resources\schema_sqlserver.sql'

# This setup is for the existing local SQLEXPRESS01 instance only.
$connection = New-Object System.Data.SqlClient.SqlConnection
$connection.ConnectionString = 'Server=.\SQLEXPRESS01;Database=master;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=10'

function Invoke-DatabaseCommand([string]$Sql) {
    $command = $connection.CreateCommand()
    $command.CommandText = $Sql
    $command.CommandTimeout = 60
    try { [void]$command.ExecuteNonQuery() } finally { $command.Dispose() }
}

try {
    $connection.Open()
    $command = $connection.CreateCommand()
    $command.CommandText = "SELECT CONVERT(int,SERVERPROPERTY('IsIntegratedSecurityOnly'))"
    $windowsOnly = $command.ExecuteScalar()
    $command.Dispose()
    if ($windowsOnly -ne 0) { throw 'SQLEXPRESS01 must allow SQL Server Authentication before running setup.' }

    $command = $connection.CreateCommand()
    $command.CommandText = "SELECT COUNT(*) FROM sys.server_principals WHERE name=N'carrental_app'"
    $loginExists = $command.ExecuteScalar() -gt 0
    $command.Dispose()
    if ($loginExists -and !(Test-Path -LiteralPath $credentialPath)) {
        throw 'carrental_app already exists but its local settings file is missing. Setup will not reset an existing login.'
    }

    # Re-running creates only missing tables and preserves all existing rows.
    $schema = Get-Content -LiteralPath $sqlPath -Raw
    foreach ($batch in [regex]::Split($schema, '(?im)^GO\s*$')) {
        if ($batch.Trim()) { Invoke-DatabaseCommand $batch }
    }

    if (!(Test-Path -LiteralPath $credentialPath)) {
        $randomBytes = New-Object byte[] 32
        $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($randomBytes) } finally { $random.Dispose() }
        $appPassword = 'Cr1!' + [Convert]::ToBase64String($randomBytes)
        # Generated locally; never print the database password or commit it.
        [IO.File]::WriteAllText($credentialPath,
            "# Local database password. Do not share or commit this file.`r`nspring.datasource.password=$appPassword`r`n",
            (New-Object System.Text.UTF8Encoding($false)))
    } else {
        $passwordLine = Get-Content -LiteralPath $credentialPath | Where-Object { $_ -like 'spring.datasource.password=*' } | Select-Object -First 1
        if (!$passwordLine) { throw 'The local settings file does not contain a database password.' }
        $appPassword = $passwordLine.Substring('spring.datasource.password='.Length)
    }

    if (!$loginExists) {
        $escapedPassword = $appPassword.Replace("'", "''")
        Invoke-DatabaseCommand "USE master; CREATE LOGIN carrental_app WITH PASSWORD=N'$escapedPassword', CHECK_POLICY=ON, DEFAULT_DATABASE=CarRentalDB;"
    }
    Invoke-DatabaseCommand @'
USE CarRentalDB;
IF USER_ID(N'carrental_app') IS NULL
    CREATE USER carrental_app FOR LOGIN carrental_app WITH DEFAULT_SCHEMA=dbo;
GRANT SELECT, INSERT, UPDATE, DELETE ON SCHEMA::dbo TO carrental_app;
'@
    Write-Host 'Database setup completed: SQLEXPRESS01 / CarRentalDB / seven tables.'
    Write-Host 'Next: double-click run-app.bat, then open http://localhost:8080.'
    Write-Host 'SSMS: .\SQLEXPRESS01, Windows Authentication, Trust server certificate.'
} catch {
    Write-Host ('Setup failed: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    $connection.Dispose()
}
