package com.carrental.service;

import com.carrental.dto.PickupRequest;
import com.carrental.dto.ReturnRequest;
import com.carrental.model.*;
import com.carrental.repository.PickupRecordRepository;
import com.carrental.repository.ReservationRepository;
import com.carrental.repository.ReturnRecordRepository;
import com.carrental.repository.UserRepository;
import com.carrental.repository.VehicleRepository;
import com.carrental.repository.DriverReturnReportRepository;
import com.carrental.repository.MaintenanceRequestRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RentalService {

    @Autowired
    private PickupRecordRepository pickupRecordRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReturnRecordRepository returnRecordRepository;

    @Autowired
    private DriverReturnReportRepository driverReturnReports;
    @Autowired
    private MaintenanceRequestRepository maintenanceRequests;

    // Vehicle pickup.
    public List<PickupRecord> getAllPickupRecords() {
        return pickupRecordRepository.findAll();
    }

    public Optional<PickupRecord> getPickupByReservationId(Long reservationId) {
        return pickupRecordRepository.findByReservationId(reservationId);
    }

    public List<User> getAvailableDrivers() {
        return userRepository.findByRole(Role.DRIVER).stream()
                .filter(driver -> driver.getDrivingLicense() != null && !driver.getDrivingLicense().isBlank())
                .filter(driver -> !hasActiveTrip(driver.getId()))
                .toList();
    }

    private boolean hasActiveTrip(Long driverId) {
        return pickupRecordRepository.existsByDriverIdAndReservationStatus(driverId, ReservationStatus.PICKED_UP);
    }

    @Transactional
    public PickupRecord processPickup(PickupRequest request) {
        Reservation reservation = reservationRepository.findByIdForUpdate(request.getReservationId())
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + request.getReservationId()));
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed reservations can be picked up");
        }
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(reservation.getVehicle().getId()).orElseThrow();
        if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE || vehicle.getStatus() == VehicleStatus.BREAKDOWN
                || maintenanceRequests.existsByVehicleIdAndStatusNot(vehicle.getId(), MaintenanceRequest.Status.COMPLETED)) {
            throw new IllegalArgumentException("Vehicle is under maintenance and cannot be picked up");
        }

        User staff = null;
        if (request.getStaffId() != null) {
            staff = userRepository.findById(request.getStaffId()).orElse(null);
        }

        if (request.getDriverId() == null) {
            throw new IllegalArgumentException("A driver is required for every vehicle pickup");
        }
        User driver = userRepository.findByIdForUpdate(request.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException("Selected driver was not found"));
        if (driver.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException("Selected user does not have the Driver role");
        }
        if (driver.getDrivingLicense() == null || driver.getDrivingLicense().isBlank()) {
            throw new IllegalArgumentException("Selected driver does not have a driving licence");
        }
        if (hasActiveTrip(driver.getId())) {
            throw new IllegalArgumentException("Selected driver is already on an active trip. Please select another driver.");
        }

        PickupRecord record = new PickupRecord();
        record.setReservation(reservation);
        record.setStaff(staff);
        record.setDriver(driver);
        record.setPickupTime(LocalDateTime.now());
        record.setInitialMileage(request.getInitialMileage());
        record.setInitialFuelLevel(request.getInitialFuelLevel() != null ? request.getInitialFuelLevel() : "Full");
        record.setConditionNotes(request.getConditionNotes());
        record.setVerifiedLicenseNumber(driver.getDrivingLicense());
        record.setCustomerSignatureConfirmed(request.getCustomerSignatureConfirmed());

        PickupRecord savedRecord = pickupRecordRepository.save(record);

        // Update reservation status to PICKED_UP
        reservation.setStatus(ReservationStatus.PICKED_UP);
        reservationRepository.save(reservation);

        // Update vehicle status to RENTED and update current mileage & fuel
        vehicle.setStatus(VehicleStatus.RENTED);
        vehicle.setCurrentMileage(request.getInitialMileage());
        vehicle.setFuelLevel(request.getInitialFuelLevel());
        vehicle.setAssignedDriver(driver);
        vehicleRepository.save(vehicle);

        return savedRecord;
    }

    public void deletePickup(Long id) {
        pickupRecordRepository.deleteById(id);
    }

    // Vehicle return and final charges.
    public List<ReturnRecord> getAllReturnRecords() {
        return returnRecordRepository.findAll();
    }

    public Optional<ReturnRecord> getReturnByReservationId(Long reservationId) {
        return returnRecordRepository.findByReservationId(reservationId);
    }

    @Transactional
    public ReturnRecord processReturn(ReturnRequest request) {
        if (request.getReservationId() == null) throw new IllegalArgumentException("Reservation is required");
        Reservation reservation = reservationRepository.findByIdForUpdate(request.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (reservation.getStatus() != ReservationStatus.PICKED_UP || returnRecordRepository.findByReservationId(reservation.getId()).isPresent()) {
            throw new IllegalArgumentException("Only active trips without a completed return can be confirmed");
        }
        DriverReturnReport report = driverReturnReports.findByReservationId(reservation.getId())
                .orElseThrow(() -> new IllegalArgumentException("Wait for the driver to submit return details"));
        if (report.getStatus() != DriverReturnReport.Status.SUBMITTED) {
            throw new IllegalArgumentException("This return has already been confirmed");
        }

        User staff = null;
        if (request.getStaffId() != null) {
            staff = userRepository.findById(request.getStaffId()).orElse(null);
        }

        if (staff == null || (staff.getRole() != Role.STAFF && staff.getRole() != Role.ADMIN)) {
            throw new IllegalArgumentException("A rental staff member must confirm this return");
        }
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(reservation.getVehicle().getId()).orElseThrow();
        if (request.isMaintenanceRequired() && (request.getMaintenanceDescription() == null
                || request.getMaintenanceDescription().isBlank() || request.getMaintenanceDescription().length() > 1000)) {
            throw new IllegalArgumentException("Describe the required maintenance (maximum 1000 characters)");
        }
        Optional<PickupRecord> pickupOpt = pickupRecordRepository.findByReservationId(reservation.getId());

        int initialMileage = pickupOpt.map(PickupRecord::getInitialMileage).orElse(vehicle.getCurrentMileage());
        DriverReturnService.validateDetails(request.getFinalMileage(), request.getFinalFuelLevel(), request.getDamagesFound(), request.getRemarks(), initialMileage);
        int finalMileage = request.getFinalMileage();
        int drivenDistance = Math.max(0, finalMileage - initialMileage);

        // Calculate allowed mileage
        int allowedMileage = reservation.getTotalDays() * (vehicle.getMileageRateLimit() != null ? vehicle.getMileageRateLimit() : 100);
        int extraKm = Math.max(0, drivenDistance - allowedMileage);

        BigDecimal extraMileageRate = vehicle.getExtraMileageRate() != null ? vehicle.getExtraMileageRate() : new BigDecimal("80.00");
        BigDecimal extraMileageFee = extraMileageRate.multiply(BigDecimal.valueOf(extraKm));

        BigDecimal damageFee = request.getDamageFee() != null ? request.getDamageFee() : BigDecimal.ZERO;
        BigDecimal fuelShortageFee = request.getFuelShortageFee() != null ? request.getFuelShortageFee() : BigDecimal.ZERO;
        BigDecimal lateReturnFee = request.getLateReturnFee() != null ? request.getLateReturnFee() : BigDecimal.ZERO;
        if (damageFee.signum() < 0 || fuelShortageFee.signum() < 0 || lateReturnFee.signum() < 0) {
            throw new IllegalArgumentException("Return fees cannot be negative");
        }

        BigDecimal totalAdditional = extraMileageFee.add(damageFee).add(fuelShortageFee).add(lateReturnFee);
        BigDecimal grandTotal = reservation.getTotalAmount().add(totalAdditional);

        ReturnRecord record = new ReturnRecord();
        record.setReservation(reservation);
        record.setStaff(staff);
        record.setReturnTime(report.getSubmittedAt());
        record.setFinalMileage(finalMileage);
        record.setFinalFuelLevel(request.getFinalFuelLevel() != null ? request.getFinalFuelLevel() : "Full");
        record.setDamagesFound(request.getDamagesFound());
        record.setDamageFee(damageFee);
        record.setExtraMileageFee(extraMileageFee);
        record.setFuelShortageFee(fuelShortageFee);
        record.setLateReturnFee(lateReturnFee);
        record.setTotalAdditionalCharges(totalAdditional);
        record.setGrandTotal(grandTotal);
        record.setRemarks(request.getRemarks());

        ReturnRecord savedRecord = returnRecordRepository.save(record);
        report.setStatus(DriverReturnReport.Status.APPROVED);
        report.setReviewedBy(staff);
        report.setReviewedAt(LocalDateTime.now());
        driverReturnReports.save(report);

        // Update reservation to COMPLETED
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);

        // Free up vehicle and update stats
        vehicle.setCurrentMileage(finalMileage);
        vehicle.setFuelLevel(request.getFinalFuelLevel());
        vehicle.setAssignedDriver(null);

        // Staff explicitly decides whether this return needs fleet maintenance.
        if (request.isMaintenanceRequired()) {
            MaintenanceRequest maintenance = new MaintenanceRequest();
            maintenance.setVehicle(vehicle);
            maintenance.setReturnRecord(savedRecord);
            maintenance.setReportedBy(staff);
            maintenance.setDescription(request.getMaintenanceDescription().trim());
            maintenanceRequests.save(maintenance);
            vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        } else if (maintenanceRequests.existsByVehicleIdAndStatusNot(vehicle.getId(), MaintenanceRequest.Status.COMPLETED)) {
            vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        } else {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
        vehicleRepository.save(vehicle);

        return savedRecord;
    }

    public void deleteReturn(Long id) {
        if (maintenanceRequests.existsByReturnRecordId(id)) {
            throw new IllegalArgumentException("This return is linked to maintenance history and cannot be deleted");
        }
        returnRecordRepository.deleteById(id);
    }
}
