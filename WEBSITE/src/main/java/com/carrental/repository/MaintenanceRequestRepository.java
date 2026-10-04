package com.carrental.repository;

import com.carrental.model.MaintenanceRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findAllByOrderByReportedAtDesc();
    boolean existsByVehicleIdAndStatusNot(Long vehicleId, MaintenanceRequest.Status status);
    boolean existsByReturnRecordReservationId(Long reservationId);
    boolean existsByReturnRecordId(Long returnRecordId);
    Optional<MaintenanceRequest> findByReturnRecordId(Long returnRecordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MaintenanceRequest m where m.id = :id")
    Optional<MaintenanceRequest> findByIdForUpdate(@Param("id") Long id);
}
