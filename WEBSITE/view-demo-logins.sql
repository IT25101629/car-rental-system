-- SSMS: connect to .\SQLEXPRESS01, then execute this file.
-- Displays the actual passwords stored for demo users, including new users.
USE CarRentalDB;
GO

SELECT id, name, email, password, role
FROM dbo.users
ORDER BY id;
