# Optional integration check. Creates only uniquely named test data and removes it.
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$tag = [Guid]::NewGuid().ToString('N')
$adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$customerSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$driverSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$customer = $null; $driver = $null; $vehicle = $null; $booking = $null

function Send-Api($Session, [string]$Method, [string]$Path, $Data = $null) {
    $args = @{ Uri="$base$Path"; Method=$Method; WebSession=$Session; ContentType='application/json' }
    if ($null -ne $Data) { $args.Body = $Data | ConvertTo-Json -Depth 8 -Compress }
    Invoke-RestMethod @args
}

try {
    $admin = Send-Api $adminSession POST '/api/auth/login' @{email='admin@carrental.lk';password='admin123'}
    if ($admin.role -ne 'ADMIN') { throw 'Admin login failed.' }
    $customer = Send-Api $customerSession POST '/api/auth/register' @{name='Database verification';email="check-$tag@example.invalid";password="Test-$tag!"}
    $driver = Send-Api $driverSession POST '/api/auth/register' @{name='Verification driver';email="driver-$tag@example.invalid";password="Test-$tag!"}
    $driver = Send-Api $adminSession PUT "/api/users/$($driver.id)" @{role='DRIVER';drivingLicense="TEST-$tag"}
    $driver = Send-Api $driverSession POST '/api/auth/login' @{email="driver-$tag@example.invalid";password="Test-$tag!"}
    $vehicle = Send-Api $adminSession POST '/api/vehicles' @{
        brand='Verification';model='Test vehicle';year=2026;category='Sedan';registrationNumber="TEST-$tag"
        rentalPricePerDay=10000;seatingCapacity=5;luggageCapacity=2;fuelType='Petrol';transmission='Automatic'
        currentMileage=1000;fuelLevel='Full';status='AVAILABLE'
    }
    $day = (Get-Date).AddDays(30).ToString('yyyy-MM-dd')
    $booking = Send-Api $customerSession POST '/api/reservations' @{
        customerId=$customer.id;vehicleId=$vehicle.id;startDate=$day;endDate=$day
        pickupLocation='Colombo';returnLocation='Colombo';notes="Database check $tag"
    }
    if ($booking.totalAmount -ne 12500) { throw 'Booking total was not 12500.' }
    $booking = Send-Api $adminSession PATCH "/api/reservations/$($booking.id)/confirm"
    $pickup = Send-Api $adminSession POST '/api/pickups' @{
        reservationId=$booking.id;staffId=$admin.id;driverId=$driver.id
        initialMileage=1000;initialFuelLevel='Full';customerSignatureConfirmed=$true
    }
    $available = Send-Api $adminSession GET '/api/pickups/available-drivers'
    if ($available.id -contains $driver.id) { throw 'Busy driver was still available.' }
    $report = Send-Api $driverSession POST '/api/driver-return-reports' @{
        reservationId=$booking.id;finalMileage=1150;finalFuelLevel='Full';damagesFound='None';remarks="Driver report $tag"
    }
    if ($report.status -ne 'SUBMITTED') { throw 'Driver report was not submitted.' }
    $available = Send-Api $adminSession GET '/api/pickups/available-drivers'
    if ($available.id -contains $driver.id) { throw 'Driver was released before staff confirmation.' }
    $returned = Send-Api $adminSession POST '/api/returns' @{
        reservationId=$booking.id;staffId=$admin.id;finalMileage=1150;finalFuelLevel='Full';remarks="Database check $tag"
        damageFee=1000;damagesFound='Bumper scratch';maintenanceRequired=$true;maintenanceDescription="Repair test scratch $tag"
    }
    # One day includes 100 km; 50 extra km at 80 = 4000.
    if ($returned.grandTotal -ne 17500) { throw 'Return total was not 17500 (including 1000 damage fee).' }
    $reports = Send-Api $driverSession GET '/api/driver-return-reports'
    $approved = $reports | Where-Object { $_.id -eq $report.id }
    if ($approved.status -ne 'APPROVED' -or $approved.reviewedBy.id -ne $admin.id) { throw 'Report approval was not saved.' }
    $booking = Send-Api $adminSession GET "/api/reservations/$($booking.id)"
    if ($booking.status -ne 'COMPLETED') { throw 'Booking was not completed.' }
    $available = Send-Api $adminSession GET '/api/pickups/available-drivers'
    if ($available.id -notcontains $driver.id) { throw 'Returned driver did not become available.' }
    $maintenance = Send-Api $adminSession GET '/api/maintenance' | Where-Object { $_.returnRecord.id -eq $returned.id }
    if ($maintenance.status -ne 'PENDING') { throw 'Maintenance request was not created.' }
    $heldVehicle = Send-Api $adminSession GET "/api/vehicles/$($vehicle.id)"
    if ($heldVehicle.status -ne 'UNDER_MAINTENANCE') { throw 'Damaged vehicle was released too early.' }
    $maintenance = Send-Api $adminSession PATCH "/api/maintenance/$($maintenance.id)/start"
    if ($maintenance.status -ne 'IN_PROGRESS') { throw 'Repair was not started.' }
    $maintenance = Send-Api $adminSession PATCH "/api/maintenance/$($maintenance.id)/complete" @{repairNotes="Scratch repaired $tag"}
    if ($maintenance.status -ne 'COMPLETED' -or $maintenance.vehicle.status -ne 'AVAILABLE') { throw 'Repair completion did not release vehicle.' }
    Write-Host 'PASS: booking, driver report, staff return, maintenance request, repair start/completion and driver/vehicle availability.'
    Write-Host 'PASS: booking total 12500; extra mileage 4000; damage fee 1000; final total 17500.'
} finally {
    # Scope every cleanup query to this run's two random email addresses.
    $projectPath = Split-Path $PSScriptRoot -Parent
    $line = Get-Content (Join-Path $projectPath 'database.local.properties') | Where-Object { $_ -like 'spring.datasource.password=*' } | Select-Object -First 1
    $tcp = Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS01\MSSQLServer\SuperSocketNetLib\Tcp\IPAll'
    $port = if ($tcp.TcpPort) { $tcp.TcpPort } else { $tcp.TcpDynamicPorts }
    $builder = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
    $builder['Data Source']="localhost,$port"; $builder['Initial Catalog']='CarRentalDB'
    $builder['User ID']='carrental_app'; $builder['Password']=$line.Substring('spring.datasource.password='.Length)
    $builder['Encrypt']=$true; $builder['TrustServerCertificate']=$true
    $connection = New-Object System.Data.SqlClient.SqlConnection($builder.ConnectionString)
    try {
        $connection.Open()
        $command = $connection.CreateCommand()
        $command.CommandText = @'
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DELETE m FROM dbo.maintenance_requests m JOIN dbo.return_records rr ON rr.id=m.return_record_id JOIN dbo.reservations r ON r.id=rr.reservation_id JOIN dbo.users u ON u.id=r.customer_id WHERE u.email=@email;
DELETE p FROM dbo.driver_return_reports p JOIN dbo.reservations r ON r.id=p.reservation_id JOIN dbo.users u ON u.id=r.customer_id WHERE u.email=@email;
DELETE p FROM dbo.pickup_records p JOIN dbo.reservations r ON r.id=p.reservation_id JOIN dbo.users u ON u.id=r.customer_id WHERE u.email=@email;
DELETE p FROM dbo.return_records p JOIN dbo.reservations r ON r.id=p.reservation_id JOIN dbo.users u ON u.id=r.customer_id WHERE u.email=@email;
DELETE r FROM dbo.reservations r JOIN dbo.users u ON u.id=r.customer_id WHERE u.email=@email;
DELETE FROM dbo.vehicles WHERE registration_number=@registration;
DELETE FROM dbo.users WHERE email IN (@email,@driver);
COMMIT;
'@
        [void]$command.Parameters.AddWithValue('@email', "check-$tag@example.invalid")
        [void]$command.Parameters.AddWithValue('@driver', "driver-$tag@example.invalid")
        [void]$command.Parameters.AddWithValue('@registration', "TEST-$tag")
        [void]$command.ExecuteNonQuery()
        $command.Dispose()
        Write-Host 'Removed only the temporary records created by this verification run.'
    } finally { $connection.Dispose() }
}
