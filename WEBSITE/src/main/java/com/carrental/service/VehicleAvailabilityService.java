package com.carrental.service;

import com.carrental.model.MaintenanceRequest;
import com.carrental.model.Vehicle;
import com.carrental.model.VehicleStatus;
import com.carrental.repository.MaintenanceRequestRepository;
import com.carrental.repository.ReservationRepository;
import com.carrental.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VehicleAvailabilityService {
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private MaintenanceRequestRepository maintenanceRequests;

    public List<Vehicle> getVehiclesByStatus(VehicleStatus status) {
        return vehicleRepository.findByStatus(status);
    }

    public boolean isVehicleAvailableForDates(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));

        if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE ||
            vehicle.getStatus() == VehicleStatus.BREAKDOWN) {
            return false;
        }

        var overlaps = reservationRepository.findOverlappingReservations(vehicleId, startDate, endDate);
        return overlaps.isEmpty();
    }

    @Transactional
    public Vehicle updateStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        checkMaintenanceStatus(id, status);
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }

    private void checkMaintenanceStatus(Long id, VehicleStatus status) {
        if (status != null && status != VehicleStatus.UNDER_MAINTENANCE
                && maintenanceRequests.existsByVehicleIdAndStatusNot(id, MaintenanceRequest.Status.COMPLETED)) {
            throw new IllegalArgumentException("Complete the open repair in Maintenance before changing this vehicle's status");
        }
    }
}
