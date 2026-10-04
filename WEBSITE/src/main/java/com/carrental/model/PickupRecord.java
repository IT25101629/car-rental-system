package com.carrental.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pickup_records")
public class PickupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private User staff;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    private LocalDateTime pickupTime = LocalDateTime.now();

    @Column(nullable = false)
    private Integer initialMileage;

    @Column(nullable = false)
    private String initialFuelLevel = "Full";

    @Column(length = 1000)
    private String conditionNotes;

    private String verifiedLicenseNumber;

    private Boolean customerSignatureConfirmed = true;

    public PickupRecord() {}

    public PickupRecord(Long id, Reservation reservation, User staff, User driver,
                        LocalDateTime pickupTime, Integer initialMileage, String initialFuelLevel,
                        String conditionNotes, String verifiedLicenseNumber, Boolean customerSignatureConfirmed) {
        this.id = id;
        this.reservation = reservation;
        this.staff = staff;
        this.driver = driver;
        this.pickupTime = pickupTime;
        this.initialMileage = initialMileage;
        this.initialFuelLevel = initialFuelLevel;
        this.conditionNotes = conditionNotes;
        this.verifiedLicenseNumber = verifiedLicenseNumber;
        this.customerSignatureConfirmed = customerSignatureConfirmed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public User getStaff() {
        return staff;
    }

    public void setStaff(User staff) {
        this.staff = staff;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public LocalDateTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(LocalDateTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public Integer getInitialMileage() {
        return initialMileage;
    }

    public void setInitialMileage(Integer initialMileage) {
        this.initialMileage = initialMileage;
    }

    public String getInitialFuelLevel() {
        return initialFuelLevel;
    }

    public void setInitialFuelLevel(String initialFuelLevel) {
        this.initialFuelLevel = initialFuelLevel;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }

    public String getVerifiedLicenseNumber() {
        return verifiedLicenseNumber;
    }

    public void setVerifiedLicenseNumber(String verifiedLicenseNumber) {
        this.verifiedLicenseNumber = verifiedLicenseNumber;
    }

    public Boolean getCustomerSignatureConfirmed() {
        return customerSignatureConfirmed;
    }

    public void setCustomerSignatureConfirmed(Boolean customerSignatureConfirmed) {
        this.customerSignatureConfirmed = customerSignatureConfirmed;
    }
}
