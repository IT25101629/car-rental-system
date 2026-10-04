package com.carrental.repository;

import com.carrental.model.Reservation;
import com.carrental.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    List<Reservation> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Reservation> findByStatusOrderByCreatedAtDesc(ReservationStatus status);
    Optional<Reservation> findByBookingReference(String bookingReference);

    @Query("SELECT r FROM Reservation r WHERE r.vehicle.id = :vehicleId " +
           "AND r.status IN ('PENDING', 'CONFIRMED', 'PICKED_UP') " +
           "AND (:startDate <= r.endDate AND :endDate >= r.startDate)")
    List<Reservation> findOverlappingReservations(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
