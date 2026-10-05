package com.carrental.repository;

import com.carrental.model.Vehicle;
import com.carrental.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Vehicle v where v.id = :id")
    java.util.Optional<Vehicle> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    List<Vehicle> findByStatus(VehicleStatus status);
    List<Vehicle> findByCategory(String category);
    List<Vehicle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(String brand, String model);
    boolean existsByRegistrationNumber(String registrationNumber);
}
