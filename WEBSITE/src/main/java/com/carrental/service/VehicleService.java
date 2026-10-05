package com.carrental.service;

import com.carrental.model.Vehicle;
import com.carrental.model.VehicleStatus;
import com.carrental.repository.ReservationRepository;
import com.carrental.repository.VehicleRepository;
import com.carrental.repository.MaintenanceRequestRepository;
import com.carrental.model.MaintenanceRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private MaintenanceRequestRepository maintenanceRequests;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    public List<Vehicle> getVehiclesByStatus(VehicleStatus status) {
        return vehicleRepository.findByStatus(status);
    }

    public List<Vehicle> getVehiclesByCategory(String category) {
        return vehicleRepository.findByCategory(category);
    }

    public List<Vehicle> searchVehicles(String category, String fuelType, String transmission,
                                        BigDecimal maxPrice, String query, Boolean availableOnly,
                                        LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("Both start date and end date are required for availability search.");
        }
        if (startDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }

        List<Vehicle> results = new ArrayList<>();
        String searchText = query == null ? "" : query.toLowerCase().trim();

        for (Vehicle vehicle : vehicleRepository.findAll()) {
            if (!matchesFilter(vehicle.getCategory(), category)) continue;
            if (!matchesFilter(vehicle.getFuelType(), fuelType)) continue;
            if (!matchesFilter(vehicle.getTransmission(), transmission)) continue;

            if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
                if (vehicle.getRentalPricePerDay().compareTo(maxPrice) > 0) continue;
            }
            if (!searchText.isEmpty()) {
                boolean found = vehicle.getBrand().toLowerCase().contains(searchText)
                        || vehicle.getModel().toLowerCase().contains(searchText)
                        || vehicle.getRegistrationNumber().toLowerCase().contains(searchText);
                if (!found) continue;
            }
            if (Boolean.TRUE.equals(availableOnly) && vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                continue;
            }
            if (startDate != null) {
                if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE
                        || vehicle.getStatus() == VehicleStatus.BREAKDOWN
                        || vehicle.getStatus() == VehicleStatus.RENTED) continue;
                if (!reservationRepository.findOverlappingReservations(vehicle.getId(), startDate, endDate).isEmpty()) continue;
            }
            results.add(vehicle);
        }
        return results;
    }

    // An empty filter or "All" accepts every value.
    private boolean matchesFilter(String value, String filter) {
        if (filter == null || filter.isEmpty() || filter.equalsIgnoreCase("ALL")) return true;
        return filter.equalsIgnoreCase(value);
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

    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicleRepository.existsByRegistrationNumber(vehicle.getRegistrationNumber())) {
            throw new RuntimeException("Vehicle with registration number already exists: " + vehicle.getRegistrationNumber());
        }
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle updated) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        checkMaintenanceStatus(id, updated.getStatus());

        vehicle.setBrand(updated.getBrand());
        vehicle.setModel(updated.getModel());
        vehicle.setYear(updated.getYear());
        vehicle.setCategory(updated.getCategory());
        vehicle.setRegistrationNumber(updated.getRegistrationNumber());
        vehicle.setRentalPricePerDay(updated.getRentalPricePerDay());
        vehicle.setSeatingCapacity(updated.getSeatingCapacity());
        vehicle.setLuggageCapacity(updated.getLuggageCapacity());
        vehicle.setFuelType(updated.getFuelType());
        vehicle.setTransmission(updated.getTransmission());
        vehicle.setMileageRateLimit(updated.getMileageRateLimit());
        vehicle.setExtraMileageRate(updated.getExtraMileageRate());
        vehicle.setCurrentMileage(updated.getCurrentMileage());
        vehicle.setFuelLevel(updated.getFuelLevel());
        if (updated.getStatus() != null) {
            vehicle.setStatus(updated.getStatus());
        }
        vehicle.setImageUrl(updated.getImageUrl());
        vehicle.setFeatures(updated.getFeatures());

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        checkMaintenanceStatus(id, status);
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }

    private void checkMaintenanceStatus(Long id, VehicleStatus status) {
        if (status != null && status != VehicleStatus.UNDER_MAINTENANCE
                && maintenanceRequests.existsByVehicleIdAndStatusNot(id, MaintenanceRequest.Status.COMPLETED)) {
            throw new IllegalArgumentException("Complete the open repair in Maintenance before changing this vehicle's status");
        }
    }
}
