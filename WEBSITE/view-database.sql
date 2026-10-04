-- SSMS: connect to .\SQLEXPRESS01 with Windows Authentication.
-- Read-only queries: execute this file to see the website data.
USE CarRentalDB;
GO
SELECT id, name, email, password, phone, driving_license, role, created_at FROM dbo.users;
SELECT * FROM dbo.vehicles;
SELECT * FROM dbo.reservations;
SELECT * FROM dbo.pickup_records;
SELECT * FROM dbo.return_records;
SELECT * FROM dbo.driver_return_reports;
SELECT * FROM dbo.maintenance_requests;

-- Booking details with customer, vehicle, and payment status.
SELECT r.booking_reference, u.name AS customer,
       v.brand, v.model, r.start_date, r.end_date, r.status, r.total_amount,
       r.payment_method, r.payment_status, r.payment_reference
FROM dbo.reservations r
JOIN dbo.users u ON u.id = r.customer_id
JOIN dbo.vehicles v ON v.id = r.vehicle_id
ORDER BY r.id DESC;
