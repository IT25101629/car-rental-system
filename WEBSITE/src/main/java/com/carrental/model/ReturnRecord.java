package com.carrental.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_records")
public class ReturnRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private User staff;

    private LocalDateTime returnTime = LocalDateTime.now();

    @Column(nullable = false)
    private Integer finalMileage;

    @Column(nullable = false)
    private String finalFuelLevel;

    @Column(length = 1000)
    private String damagesFound;

    private BigDecimal damageFee = BigDecimal.ZERO;

    private BigDecimal extraMileageFee = BigDecimal.ZERO;

    private BigDecimal fuelShortageFee = BigDecimal.ZERO;

    private BigDecimal lateReturnFee = BigDecimal.ZERO;

    private BigDecimal totalAdditionalCharges = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal grandTotal;

    @Column(length = 1000)
    private String remarks;

    public ReturnRecord() {}

    public ReturnRecord(Long id, Reservation reservation, User staff, LocalDateTime returnTime,
                        Integer finalMileage, String finalFuelLevel, String damagesFound,
                        BigDecimal damageFee, BigDecimal extraMileageFee, BigDecimal fuelShortageFee,
                        BigDecimal lateReturnFee, BigDecimal totalAdditionalCharges,
                        BigDecimal grandTotal, String remarks) {
        this.id = id;
        this.reservation = reservation;
        this.staff = staff;
        this.returnTime = returnTime;
        this.finalMileage = finalMileage;
        this.finalFuelLevel = finalFuelLevel;
        this.damagesFound = damagesFound;
        this.damageFee = damageFee;
        this.extraMileageFee = extraMileageFee;
        this.fuelShortageFee = fuelShortageFee;
        this.lateReturnFee = lateReturnFee;
        this.totalAdditionalCharges = totalAdditionalCharges;
        this.grandTotal = grandTotal;
        this.remarks = remarks;
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

    public LocalDateTime getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(LocalDateTime returnTime) {
        this.returnTime = returnTime;
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

    public BigDecimal getExtraMileageFee() {
        return extraMileageFee;
    }

    public void setExtraMileageFee(BigDecimal extraMileageFee) {
        this.extraMileageFee = extraMileageFee;
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

    public BigDecimal getTotalAdditionalCharges() {
        return totalAdditionalCharges;
    }

    public void setTotalAdditionalCharges(BigDecimal totalAdditionalCharges) {
        this.totalAdditionalCharges = totalAdditionalCharges;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
