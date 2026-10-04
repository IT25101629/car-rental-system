package com.carrental.controller;

import com.carrental.dto.ReturnRequest;
import com.carrental.model.ReturnRecord;
import com.carrental.service.RentalService;
import com.carrental.service.DriverReturnService;
import org.springframework.security.core.Authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RentalController {

    @Autowired
    private RentalService rentalService;

    @Autowired
    private DriverReturnService driverReturnService;

    @GetMapping("/api/returns")
    public ResponseEntity<List<ReturnRecord>> getAllReturns() {
        return ResponseEntity.ok(rentalService.getAllReturnRecords());
    }

    @GetMapping("/api/returns/reservation/{reservationId}")
    public ResponseEntity<?> getReturnByReservationId(@PathVariable Long reservationId) {
        return rentalService.getReturnByReservationId(reservationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/returns")
    public ResponseEntity<?> processReturn(@RequestBody ReturnRequest request, Authentication authentication) {
        request.setStaffId(driverReturnService.currentUser(authentication.getName()).getId());
        return ResponseEntity.ok(rentalService.processReturn(request));
    }

    @DeleteMapping("/api/returns/{id}")
    public ResponseEntity<?> deleteReturn(@PathVariable Long id) {
        rentalService.deleteReturn(id);
        return ResponseEntity.ok("Return record deleted successfully");
    }
}
