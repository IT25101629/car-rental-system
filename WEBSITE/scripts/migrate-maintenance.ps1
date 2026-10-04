$ErrorActionPreference = 'Stop'
$projectPath = Split-Path $PSScriptRoot -Parent
$schema = Get-Content -LiteralPath (Join-Path $projectPath 'src/main/resources/schema_sqlserver.sql') -Raw
$tableBatch = [regex]::Match($schema, "(?s)IF OBJECT_ID\(N'dbo\.maintenance_requests', N'U'\) IS NULL\s*CREATE TABLE dbo\.maintenance_requests \(.*?\);").Value
if (!$tableBatch) { throw 'Maintenance table definition was not found.' }
# Add only this table in the existing database; no logins, permissions or existing rows change.
$connection = New-Object System.Data.SqlClient.SqlConnection
$connection.ConnectionString = 'Server=.\SQLEXPRESS01;Database=CarRentalDB;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=10'
try {
    $connection.Open()
    $command = $connection.CreateCommand()
    $command.CommandText = $tableBatch
    try { [void]$command.ExecuteNonQuery() } finally { $command.Dispose() }
    $command = $connection.CreateCommand()
    $command.CommandText = @'
IF COL_LENGTH(N'dbo.maintenance_requests', N'handled_by') IS NOT NULL
BEGIN
    DECLARE @handledByConstraint sysname;
    SELECT TOP (1) @handledByConstraint = fk.name
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
    WHERE fk.parent_object_id = OBJECT_ID(N'dbo.maintenance_requests')
      AND fkc.parent_column_id = COLUMNPROPERTY(OBJECT_ID(N'dbo.maintenance_requests'), N'handled_by', 'ColumnId');
    IF @handledByConstraint IS NOT NULL
    BEGIN
        DECLARE @dropConstraintSql nvarchar(1000) =
            N'ALTER TABLE dbo.maintenance_requests DROP CONSTRAINT ' + QUOTENAME(@handledByConstraint);
        EXEC sys.sp_executesql @dropConstraintSql;
    END
    ALTER TABLE dbo.maintenance_requests DROP COLUMN handled_by;
END
'@
    try { [void]$command.ExecuteNonQuery() } finally { $command.Dispose() }
    Write-Host 'maintenance_requests is ready without handled_by. Existing repair records were preserved.'
} finally { $connection.Dispose() }
