package com.carrental.repository;

import com.carrental.model.ReturnRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {
    Optional<ReturnRecord> findByReservationId(Long reservationId);
}
