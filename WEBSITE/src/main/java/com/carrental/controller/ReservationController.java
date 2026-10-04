package com.carrental.controller;

import com.carrental.dto.ReservationRequest;
import com.carrental.model.Reservation;
import com.carrental.model.ReservationStatus;
import com.carrental.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<Reservation>> getAllReservations(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) ReservationStatus status
    ) {
        if (customerId != null) {
            return ResponseEntity.ok(reservationService.getCustomerReservations(customerId));
        }
        if (status != null) {
            return ResponseEntity.ok(reservationService.getReservationsByStatus(status));
        }
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationById(@PathVariable Long id) {
        return reservationService.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ref/{ref}")
    public ResponseEntity<?> getReservationByReference(@PathVariable String ref) {
        return reservationService.getReservationByReference(ref)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservation(request));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<?> confirmReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirmReservation(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompletedReservation(@PathVariable Long id) {
        try {
            reservationService.deleteCompletedReservation(id);
            return ResponseEntity.ok(Map.of("message", "Completed reservation deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
