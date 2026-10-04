package com.carrental.repository;

import com.carrental.model.PickupRecord;
import com.carrental.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PickupRecordRepository extends JpaRepository<PickupRecord, Long> {
    java.util.List<PickupRecord> findByDriverIdAndReservationStatus(Long driverId, ReservationStatus status);
    Optional<PickupRecord> findByReservationId(Long reservationId);
    boolean existsByDriverIdAndReservationStatus(Long driverId, ReservationStatus status);
}
