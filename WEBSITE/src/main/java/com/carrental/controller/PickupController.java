package com.carrental.controller;

import com.carrental.dto.PickupRequest;
import com.carrental.model.PickupRecord;
import com.carrental.model.User;
import com.carrental.service.PickupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PickupController {

    @Autowired
    private PickupService pickupService;

    @GetMapping("/api/pickups/available-drivers")
    public ResponseEntity<List<User>> getAvailableDrivers() {
        return ResponseEntity.ok(pickupService.getAvailableDrivers());
    }

    @GetMapping("/api/pickups")
    public ResponseEntity<List<PickupRecord>> getAllPickups() {
        return ResponseEntity.ok(pickupService.getAllPickupRecords());
    }

    @GetMapping("/api/pickups/reservation/{reservationId}")
    public ResponseEntity<?> getPickupByReservationId(@PathVariable Long reservationId) {
        return pickupService.getPickupByReservationId(reservationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/pickups")
    public ResponseEntity<?> processPickup(@RequestBody PickupRequest request) {
        return ResponseEntity.ok(pickupService.processPickup(request));
    }

    @DeleteMapping("/api/pickups/{id}")
    public ResponseEntity<?> deletePickup(@PathVariable Long id) {
        pickupService.deletePickup(id);
        return ResponseEntity.ok("Pickup record deleted successfully");
    }
}
