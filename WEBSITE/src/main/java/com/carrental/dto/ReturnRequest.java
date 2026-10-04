package com.carrental.dto;

import java.math.BigDecimal;

public class ReturnRequest {
    private Long reservationId;
    private Long staffId;
    private Integer finalMileage;
    private String finalFuelLevel;
    private String damagesFound;
    private BigDecimal damageFee = BigDecimal.ZERO;
    private BigDecimal fuelShortageFee = BigDecimal.ZERO;
    private BigDecimal lateReturnFee = BigDecimal.ZERO;
    private String remarks;
    private boolean maintenanceRequired;
    private String maintenanceDescription;

    public boolean isMaintenanceRequired() { return maintenanceRequired; }
    public void setMaintenanceRequired(boolean value) { maintenanceRequired = value; }
    public String getMaintenanceDescription() { return maintenanceDescription; }
    public void setMaintenanceDescription(String value) { maintenanceDescription = value; }

    public ReturnRequest() {}

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

    public Integer getFinalMileage() {
        return finalMileage;
    }

    public void setFinalMileage(Integer finalMileage) {
        this.finalMileage = finalMileage;
    }

    public String getFinalFuelLevel() {
        return finalFuelLevel;
    }

    public void setFinalFuelLevel(String finalFuelLevel) {
        this.finalFuelLevel = finalFuelLevel;
    }

    public String getDamagesFound() {
        return damagesFound;
    }

    public void setDamagesFound(String damagesFound) {
        this.damagesFound = damagesFound;
    }

    public BigDecimal getDamageFee() {
        return damageFee;
    }

    public void setDamageFee(BigDecimal damageFee) {
        this.damageFee = damageFee;
    }

    public BigDecimal getFuelShortageFee() {
        return fuelShortageFee;
    }

    public void setFuelShortageFee(BigDecimal fuelShortageFee) {
        this.fuelShortageFee = fuelShortageFee;
    }

    public BigDecimal getLateReturnFee() {
        return lateReturnFee;
    }

    public void setLateReturnFee(BigDecimal lateReturnFee) {
        this.lateReturnFee = lateReturnFee;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
