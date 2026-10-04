package com.carrental.service;

import com.carrental.model.*;
import com.carrental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaintenanceService {
    @Autowired private MaintenanceRequestRepository requests;
    @Autowired private VehicleRepository vehicles;
    @Autowired private UserRepository users;

    public List<MaintenanceRequest> list() { return requests.findAllByOrderByReportedAtDesc(); }

    @Transactional
    public MaintenanceRequest start(Long id, String email) {
        manager(email);
        MaintenanceRequest request = findForUpdate(id);
        if (request.getStatus() != MaintenanceRequest.Status.PENDING) {
            throw new IllegalArgumentException("Only pending maintenance can be started");
        }
        request.setStatus(MaintenanceRequest.Status.IN_PROGRESS);
        request.setStartedAt(LocalDateTime.now());
        return requests.save(request);
    }

    @Transactional
    public MaintenanceRequest complete(Long id, String notes, String email) {
        manager(email);
        if (notes == null || notes.isBlank() || notes.length() > 1000) {
            throw new IllegalArgumentException("Enter repair notes (maximum 1000 characters)");
        }
        MaintenanceRequest request = findForUpdate(id);
        if (request.getStatus() != MaintenanceRequest.Status.IN_PROGRESS) {
            throw new IllegalArgumentException("Start the repair before completing it; completed repairs cannot be completed again");
        }
        Vehicle vehicle = vehicles.findByIdForUpdate(request.getVehicle().getId()).orElseThrow();
        request.setStatus(MaintenanceRequest.Status.COMPLETED);
        request.setRepairNotes(notes.trim());
        request.setCompletedAt(LocalDateTime.now());
        requests.saveAndFlush(request);
        if (!requests.existsByVehicleIdAndStatusNot(vehicle.getId(), MaintenanceRequest.Status.COMPLETED)
                && vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicles.save(vehicle);
        }
        return request;
    }

    private MaintenanceRequest findForUpdate(Long id) {
        return requests.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));
    }

    private User manager(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != Role.FLEET_MANAGER && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return user;
    }
}
