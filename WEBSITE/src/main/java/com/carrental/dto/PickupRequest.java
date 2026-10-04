package com.carrental.dto;

public class PickupRequest {
    private Long reservationId;
    private Long staffId;
    private Long driverId;
    private Integer initialMileage;
    private String initialFuelLevel;
    private String conditionNotes;
    private Boolean customerSignatureConfirmed = true;

    public PickupRequest() {}

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
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

    public Boolean getCustomerSignatureConfirmed() {
        return customerSignatureConfirmed;
    }

    public void setCustomerSignatureConfirmed(Boolean customerSignatureConfirmed) {
        this.customerSignatureConfirmed = customerSignatureConfirmed;
    }
}
