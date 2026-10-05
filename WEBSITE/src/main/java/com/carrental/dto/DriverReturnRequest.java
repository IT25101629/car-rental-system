package com.carrental.dto;

import jakarta.validation.constraints.*;

public record DriverReturnRequest(
        @NotNull Long reservationId,
        @NotNull @Min(0) Integer finalMileage,
        @NotNull @Pattern(regexp = "Full|3/4|1/2|1/4|Empty") String finalFuelLevel,
        @Size(max = 1000) String damagesFound,
        @Size(max = 1000) String remarks) {}
