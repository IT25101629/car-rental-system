package com.carrental.service;

import com.carrental.dto.DriverReturnRequest;
import com.carrental.model.*;
import com.carrental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class DriverReturnService {
    @Autowired private DriverReturnReportRepository reports;
    @Autowired private ReservationRepository reservations;
    @Autowired private PickupRecordRepository pickups;
    @Autowired private UserRepository users;

    public User currentUser(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public List<Reservation> activeTrips(String email) {
        User driver = currentUser(email);
        requireDriver(driver);
        return pickups.findByDriverIdAndReservationStatus(driver.getId(), ReservationStatus.PICKED_UP)
                .stream().map(PickupRecord::getReservation).toList();
    }

    public List<DriverReturnReport> list(String email) {
        User user = currentUser(email);
        if (user.getRole() == Role.STAFF || user.getRole() == Role.ADMIN) {
            return reports.findAllByOrderBySubmittedAtDesc();
        }
        requireDriver(user);
        return reports.findByDriverIdOrderBySubmittedAtDesc(user.getId());
    }

    @Transactional
    public DriverReturnReport submit(DriverReturnRequest request, String email) {
        User driver = currentUser(email);
        requireDriver(driver);
        Reservation reservation = reservations.findByIdForUpdate(request.reservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        PickupRecord pickup = pickups.findByReservationId(reservation.getId())
                .orElseThrow(() -> new IllegalArgumentException("Trip has not been picked up"));
        if (pickup.getDriver() == null || !pickup.getDriver().getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This trip is assigned to another driver");
        }
        if (reservation.getStatus() != ReservationStatus.PICKED_UP) {
            throw new IllegalArgumentException("Only active trips can be returned");
        }
        if (reports.findByReservationId(reservation.getId()).isPresent()) {
            throw new IllegalArgumentException("Return details have already been submitted");
        }
        validateDetails(request.finalMileage(), request.finalFuelLevel(), request.damagesFound(), request.remarks(), pickup.getInitialMileage());
        DriverReturnReport report = new DriverReturnReport();
        report.setReservation(reservation);
        report.setDriver(driver);
        report.setFinalMileage(request.finalMileage());
        report.setFinalFuelLevel(request.finalFuelLevel());
        report.setDamagesFound(request.damagesFound());
        report.setRemarks(request.remarks());
        return reports.save(report);
    }

    public static void validateDetails(Integer mileage, String fuel, String damages, String remarks, int initialMileage) {
        if (mileage == null || mileage < initialMileage || mileage < 0) {
            throw new IllegalArgumentException("Final mileage cannot be less than pickup mileage (" + initialMileage + " km)");
        }
        if (fuel == null || !List.of("Full", "3/4", "1/2", "1/4", "Empty").contains(fuel)) {
            throw new IllegalArgumentException("Select a valid fuel level");
        }
        if ((damages != null && damages.length() > 1000) || (remarks != null && remarks.length() > 1000)) {
            throw new IllegalArgumentException("Notes must be 1000 characters or fewer");
        }
    }

    private void requireDriver(User user) {
        if (user.getRole() != Role.DRIVER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
