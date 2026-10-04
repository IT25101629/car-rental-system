package com.carrental.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {
    public enum Status { PENDING, IN_PROGRESS, COMPLETED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    @OneToOne @JoinColumn(name = "return_record_id", nullable = false, unique = true)
    private ReturnRecord returnRecord;
    @ManyToOne @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;
    @Column(nullable = false, length = 1000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.PENDING;
    @Column(nullable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    @Column(length = 1000)
    private String repairNotes;

    public Long getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle value) { vehicle = value; }
    public ReturnRecord getReturnRecord() { return returnRecord; }
    public void setReturnRecord(ReturnRecord value) { returnRecord = value; }
    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User value) { reportedBy = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public Status getStatus() { return status; }
    public void setStatus(Status value) { status = value; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime value) { startedAt = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { completedAt = value; }
    public String getRepairNotes() { return repairNotes; }
    public void setRepairNotes(String value) { repairNotes = value; }
}
