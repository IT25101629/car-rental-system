package com.carrental.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_return_reports")
public class DriverReturnReport {
    public enum Status { SUBMITTED, APPROVED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;
    @ManyToOne @JoinColumn(name = "driver_id", nullable = false)
    private User driver;
    @Column(nullable = false)
    private Integer finalMileage;
    @Column(nullable = false)
    private String finalFuelLevel;
    @Column(length = 1000)
    private String damagesFound;
    @Column(length = 1000)
    private String remarks;
    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.SUBMITTED;
    @ManyToOne @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation value) { reservation = value; }
    public User getDriver() { return driver; }
    public void setDriver(User value) { driver = value; }
    public Integer getFinalMileage() { return finalMileage; }
    public void setFinalMileage(Integer value) { finalMileage = value; }
    public String getFinalFuelLevel() { return finalFuelLevel; }
    public void setFinalFuelLevel(String value) { finalFuelLevel = value; }
    public String getDamagesFound() { return damagesFound; }
    public void setDamagesFound(String value) { damagesFound = value; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String value) { remarks = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status value) { status = value; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User value) { reviewedBy = value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime value) { reviewedAt = value; }
}
