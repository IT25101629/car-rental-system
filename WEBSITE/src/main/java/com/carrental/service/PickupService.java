package com.carrental.service;

import com.carrental.dto.PickupRequest;
import com.carrental.model.*;
import com.carrental.repository.PickupRecordRepository;
import com.carrental.repository.ReservationRepository;
import com.carrental.repository.UserRepository;
import com.carrental.repository.VehicleRepository;
import com.carrental.repository.MaintenanceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PickupService {
    @Autowired private PickupRecordRepository pickupRecordRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MaintenanceRequestRepository maintenanceRequests;

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
    private boolean hasActiveTrip(Long driverId) {
        return pickupRecordRepository
                .existsByDriverIdAndReservationStatus(
                        driverId,
                        ReservationStatus.PICKED_UP
                );
    }
}
