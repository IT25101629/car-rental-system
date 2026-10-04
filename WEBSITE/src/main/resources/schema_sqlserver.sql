-- Three sprints: accounts/vehicles, reservations, pickup/return.
-- Seven tables matching the Java entities. No DROP or DELETE statements.
-- Run on SQLEXPRESS01 in SSMS with Windows Authentication.
USE master;
GO
IF DB_ID(N'CarRentalDB') IS NULL
    CREATE DATABASE CarRentalDB;
GO
USE CarRentalDB;
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    email NVARCHAR(255) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    phone NVARCHAR(255) NULL,
    driving_license NVARCHAR(255) NULL,
    role NVARCHAR(255) NOT NULL DEFAULT 'CUSTOMER'
        CHECK (role IN ('CUSTOMER','STAFF','FLEET_MANAGER','ADMIN','DRIVER')),
    created_at DATETIME2(6) NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID(N'dbo.vehicles', N'U') IS NULL
CREATE TABLE dbo.vehicles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    brand NVARCHAR(255) NOT NULL,
    model NVARCHAR(255) NOT NULL,
    vehicle_year INT NULL,
    category NVARCHAR(255) NOT NULL,
    registration_number NVARCHAR(255) NOT NULL UNIQUE,
    rental_price_per_day DECIMAL(38,2) NOT NULL,
    seating_capacity INT NULL,
    luggage_capacity INT NULL,
    fuel_type NVARCHAR(255) NULL,
    transmission NVARCHAR(255) NULL,
    mileage_rate_limit INT NULL DEFAULT 100,
    extra_mileage_rate DECIMAL(38,2) NULL DEFAULT 80.00,
    current_mileage INT NULL DEFAULT 0,
    fuel_level NVARCHAR(255) NULL DEFAULT 'Full',
    status NVARCHAR(255) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE','RESERVED','RENTED','UNDER_MAINTENANCE','BREAKDOWN')),
    image_url NVARCHAR(1000) NULL,
    features NVARCHAR(2000) NULL,
    assigned_driver_id BIGINT NULL REFERENCES dbo.users(id)
);
GO

IF OBJECT_ID(N'dbo.reservations', N'U') IS NULL
CREATE TABLE dbo.reservations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_reference NVARCHAR(255) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES dbo.users(id),
    vehicle_id BIGINT NOT NULL REFERENCES dbo.vehicles(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    pickup_location NVARCHAR(255) NOT NULL,
    return_location NVARCHAR(255) NOT NULL,
    driver_required BIT NULL DEFAULT 1,
    total_days INT NOT NULL,
    daily_rate DECIMAL(38,2) NOT NULL,
    total_amount DECIMAL(38,2) NOT NULL,
    status NVARCHAR(255) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','CONFIRMED','PICKED_UP','COMPLETED','CANCELLED')),
    payment_method NVARCHAR(50) NULL DEFAULT 'CASH',
    payment_status NVARCHAR(50) NULL DEFAULT 'PENDING',
    payment_reference NVARCHAR(100) NULL,
    notes NVARCHAR(1000) NULL,
    created_at DATETIME2(6) NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID(N'dbo.pickup_records', N'U') IS NULL
CREATE TABLE dbo.pickup_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE REFERENCES dbo.reservations(id),
    staff_id BIGINT NULL REFERENCES dbo.users(id),
    driver_id BIGINT NULL REFERENCES dbo.users(id),
    pickup_time DATETIME2(6) NULL DEFAULT SYSDATETIME(),
    initial_mileage INT NOT NULL,
    initial_fuel_level NVARCHAR(255) NOT NULL,
    condition_notes NVARCHAR(1000) NULL,
    verified_license_number NVARCHAR(255) NULL,
    customer_signature_confirmed BIT NULL DEFAULT 1
);
GO

IF OBJECT_ID(N'dbo.return_records', N'U') IS NULL
CREATE TABLE dbo.return_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE REFERENCES dbo.reservations(id),
    staff_id BIGINT NULL REFERENCES dbo.users(id),
    return_time DATETIME2(6) NULL DEFAULT SYSDATETIME(),
    final_mileage INT NOT NULL,
    final_fuel_level NVARCHAR(255) NOT NULL,
    damages_found NVARCHAR(1000) NULL,
    damage_fee DECIMAL(38,2) NULL DEFAULT 0,
    extra_mileage_fee DECIMAL(38,2) NULL DEFAULT 0,
    fuel_shortage_fee DECIMAL(38,2) NULL DEFAULT 0,
    late_return_fee DECIMAL(38,2) NULL DEFAULT 0,
    total_additional_charges DECIMAL(38,2) NULL DEFAULT 0,
    grand_total DECIMAL(38,2) NOT NULL,
    remarks NVARCHAR(1000) NULL
);
GO

-- Driver submissions are kept separately from the staff-approved final return.
IF OBJECT_ID(N'dbo.driver_return_reports', N'U') IS NULL
CREATE TABLE dbo.driver_return_reports (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE REFERENCES dbo.reservations(id),
    driver_id BIGINT NOT NULL REFERENCES dbo.users(id),
    final_mileage INT NOT NULL CHECK (final_mileage >= 0),
    final_fuel_level NVARCHAR(255) NOT NULL,
    damages_found NVARCHAR(1000) NULL,
    remarks NVARCHAR(1000) NULL,
    submitted_at DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    status NVARCHAR(255) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'APPROVED')),
    reviewed_by BIGINT NULL REFERENCES dbo.users(id),
    reviewed_at DATETIME2(6) NULL
);
GO

IF OBJECT_ID(N'dbo.maintenance_requests', N'U') IS NULL
CREATE TABLE dbo.maintenance_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES dbo.vehicles(id),
    return_record_id BIGINT NOT NULL UNIQUE REFERENCES dbo.return_records(id),
    reported_by BIGINT NOT NULL REFERENCES dbo.users(id),
    description NVARCHAR(1000) NOT NULL,
    status NVARCHAR(255) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    reported_at DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    started_at DATETIME2(6) NULL,
    completed_at DATETIME2(6) NULL,
    repair_notes NVARCHAR(1000) NULL
);
GO

-- Ensure payment columns exist on existing reservations table
IF COL_LENGTH('dbo.reservations', 'payment_method') IS NULL
    ALTER TABLE dbo.reservations ADD payment_method NVARCHAR(50) NULL DEFAULT 'CASH';
IF COL_LENGTH('dbo.reservations', 'payment_status') IS NULL
    ALTER TABLE dbo.reservations ADD payment_status NVARCHAR(50) NULL DEFAULT 'PENDING';
IF COL_LENGTH('dbo.reservations', 'payment_reference') IS NULL
    ALTER TABLE dbo.reservations ADD payment_reference NVARCHAR(100) NULL;
GO
