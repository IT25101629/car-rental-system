package com.carrental.service;

import com.carrental.dto.ReservationRequest;
import com.carrental.model.*;
import com.carrental.repository.ReservationRepository;
import com.carrental.repository.PickupRecordRepository;
import com.carrental.repository.ReturnRecordRepository;
import com.carrental.repository.UserRepository;
import com.carrental.repository.VehicleRepository;
import com.carrental.repository.DriverReturnReportRepository;
import com.carrental.repository.MaintenanceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private static final BigDecimal DRIVER_FEE_PER_DAY = new BigDecimal("2500");

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PickupRecordRepository pickupRecordRepository;

    @Autowired
    private ReturnRecordRepository returnRecordRepository;

    @Autowired
    private DriverReturnReportRepository driverReturnReports;
    @Autowired
    private MaintenanceRequestRepository maintenanceRequests;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getCustomerReservations(Long customerId) {
        return reservationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<Reservation> getReservationsByStatus(ReservationStatus status) {
        return reservationRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    public Optional<Reservation> getReservationByReference(String reference) {
        return reservationRepository.findByBookingReference(reference);
    }

    public Reservation createReservation(ReservationRequest request) {
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + request.getCustomerId()));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + request.getVehicleId()));

        if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE || vehicle.getStatus() == VehicleStatus.BREAKDOWN) {
            throw new RuntimeException("Vehicle is currently under maintenance or breakdown and cannot be booked.");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("End date cannot be before start date.");
        }

        // Prevent overlapping bookings
        List<Reservation> overlaps = reservationRepository.findOverlappingReservations(
                vehicle.getId(), request.getStartDate(), request.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new RuntimeException("Vehicle is already reserved for the selected dates. Please choose another date range or vehicle.");
        }

        String ref = "RES-" + System.currentTimeMillis() % 1000000;

        Reservation reservation = new Reservation();
        reservation.setBookingReference(ref);
        reservation.setCustomer(customer);
        reservation.setVehicle(vehicle);
        reservation.setStartDate(request.getStartDate());
        reservation.setEndDate(request.getEndDate());
        reservation.setPickupLocation(request.getPickupLocation());
        reservation.setReturnLocation(request.getReturnLocation());
        // helaCabs provides every rental with a chauffeur.
        reservation.setDriverRequired(true);
        reservation.setDailyRate(vehicle.getRentalPricePerDay());
        updatePrice(reservation);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setNotes(request.getNotes());

        String method = request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                ? request.getPaymentMethod().trim().toUpperCase() : "CASH";
        reservation.setPaymentMethod(method);
        if ("CARD".equals(method)) {
            reservation.setPaymentStatus("PAID");
            reservation.setPaymentReference("PAY-" + (System.currentTimeMillis() % 10000000));
        } else {
            reservation.setPaymentStatus("PENDING");
            reservation.setPaymentReference(null);
        }

        Reservation saved = reservationRepository.save(reservation);

        // Update vehicle status if starting today
        if (request.getStartDate().equals(LocalDate.now())) {
            vehicle.setStatus(VehicleStatus.RESERVED);
            vehicleRepository.save(vehicle);
        }

        return saved;
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = findReservation(id);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void deleteCompletedReservation(Long id) {
        Reservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed reservations can be deleted");
        }

        if (maintenanceRequests.existsByReturnRecordReservationId(id)) {
            throw new IllegalArgumentException("This booking is linked to maintenance history and cannot be deleted");
        }
        returnRecordRepository.findByReservationId(id).ifPresent(returnRecordRepository::delete);
        driverReturnReports.findByReservationId(id).ifPresent(driverReturnReports::delete);
        pickupRecordRepository.findByReservationId(id).ifPresent(pickupRecordRepository::delete);
        reservationRepository.delete(reservation);
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));
    }

    // Both booking and modification use the same inclusive-day price calculation.
    private void updatePrice(Reservation reservation) {
        long days = ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate()) + 1;
        if (days <= 0) days = 1;

        BigDecimal total = reservation.getDailyRate().multiply(BigDecimal.valueOf(days));
        if (Boolean.TRUE.equals(reservation.getDriverRequired())) {
            total = total.add(DRIVER_FEE_PER_DAY.multiply(BigDecimal.valueOf(days)));
        }
        reservation.setTotalDays((int) days);
        reservation.setTotalAmount(total);
    }
}
