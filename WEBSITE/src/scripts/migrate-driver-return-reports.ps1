$ErrorActionPreference = 'Stop'
$projectPath = Split-Path $PSScriptRoot -Parent
$schema = Get-Content -LiteralPath (Join-Path $projectPath 'src/main/resources/schema_sqlserver.sql') -Raw
$tableBatch = [regex]::Match($schema, "(?s)IF OBJECT_ID\(N'dbo\.driver_return_reports', N'U'\) IS NULL\s*CREATE TABLE dbo\.driver_return_reports \(.*?\);"
).Value
if (!$tableBatch) { throw 'Driver return report table definition was not found.' }

# Connect only to the existing project database. No login, credential or permission changes.
$connection = New-Object System.Data.SqlClient.SqlConnection
$connection.ConnectionString = 'Server=.\SQLEXPRESS01;Database=CarRentalDB;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=10'
try {
    $connection.Open()
    $command = $connection.CreateCommand()
    $command.CommandText = $tableBatch
    try { [void]$command.ExecuteNonQuery() } finally { $command.Dispose() }
    Write-Host 'driver_return_reports table is ready. Existing rows and permissions were preserved.'
} finally { $connection.Dispose() }
