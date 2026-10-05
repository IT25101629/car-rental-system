package com.carrental.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "vehicle_year")
    private Integer year;

    @Column(nullable = false)
    private String category; // Economy, Sedan, SUV, Luxury, Van

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private BigDecimal rentalPricePerDay;

    private Integer seatingCapacity;

    private Integer luggageCapacity;

    private String fuelType; // Petrol, Diesel, Hybrid, Electric

    private String transmission; // Automatic, Manual

    private Integer mileageRateLimit = 100; // Free km per day

    private BigDecimal extraMileageRate = new BigDecimal("80.00"); // LKR per extra km

    private Integer currentMileage = 0;

    private String fuelLevel = "Full"; // Full, 3/4, 1/2, 1/4, Empty

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 2000)
    private String features;

    @ManyToOne
    @JoinColumn(name = "assigned_driver_id")
    private User assignedDriver;

    public Vehicle() {}

    public Vehicle(Long id, String brand, String model, Integer year, String category,
                   String registrationNumber, BigDecimal rentalPricePerDay, Integer seatingCapacity,
                   Integer luggageCapacity, String fuelType, String transmission,
                   Integer currentMileage, String fuelLevel, VehicleStatus status,
                   String imageUrl, String features) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.category = category;
        this.registrationNumber = registrationNumber;
        this.rentalPricePerDay = rentalPricePerDay;
        this.seatingCapacity = seatingCapacity;
        this.luggageCapacity = luggageCapacity;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.currentMileage = currentMileage;
        this.fuelLevel = fuelLevel;
        this.status = status;
        this.imageUrl = imageUrl;
        this.features = features;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public BigDecimal getRentalPricePerDay() {
        return rentalPricePerDay;
    }

    public void setRentalPricePerDay(BigDecimal rentalPricePerDay) {
        this.rentalPricePerDay = rentalPricePerDay;
    }

    public Integer getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(Integer seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public Integer getLuggageCapacity() {
        return luggageCapacity;
    }

    public void setLuggageCapacity(Integer luggageCapacity) {
        this.luggageCapacity = luggageCapacity;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public Integer getMileageRateLimit() {
        return mileageRateLimit;
    }

    public void setMileageRateLimit(Integer mileageRateLimit) {
        this.mileageRateLimit = mileageRateLimit;
    }

    public BigDecimal getExtraMileageRate() {
        return extraMileageRate;
    }

    public void setExtraMileageRate(BigDecimal extraMileageRate) {
        this.extraMileageRate = extraMileageRate;
    }

    public Integer getCurrentMileage() {
        return currentMileage;
    }

    public void setCurrentMileage(Integer currentMileage) {
        this.currentMileage = currentMileage;
    }

    public String getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(String fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public User getAssignedDriver() {
        return assignedDriver;
    }

    public void setAssignedDriver(User assignedDriver) {
        this.assignedDriver = assignedDriver;
    }
}
