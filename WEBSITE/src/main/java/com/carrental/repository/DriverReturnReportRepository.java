package com.carrental.repository;

import com.carrental.model.DriverReturnReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DriverReturnReportRepository extends JpaRepository<DriverReturnReport, Long> {
    Optional<DriverReturnReport> findByReservationId(Long reservationId);
    List<DriverReturnReport> findByDriverIdOrderBySubmittedAtDesc(Long driverId);
    List<DriverReturnReport> findAllByOrderBySubmittedAtDesc();
}
