-- Step 1: Create database if not exists
USE master;
GO
IF DB_ID(N'CarRentalDB') IS NULL
    CREATE DATABASE CarRentalDB;
GO

-- Step 2: Create login if not exists
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'carrental_app')
BEGIN
    CREATE LOGIN carrental_app WITH PASSWORD = N'CarRental@2024!', CHECK_POLICY = OFF;
END
GO

-- Step 3: Create database user and grant permissions
USE CarRentalDB;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'carrental_app')
BEGIN
    CREATE USER carrental_app FOR LOGIN carrental_app;
END
GO
ALTER ROLE db_owner ADD MEMBER carrental_app;
GO
PRINT 'Setup complete! carrental_app user created successfully.';
GO
