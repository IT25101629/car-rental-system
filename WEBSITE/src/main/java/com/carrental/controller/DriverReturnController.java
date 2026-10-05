package com.carrental.controller;

import com.carrental.dto.DriverReturnRequest;
import com.carrental.model.*;
import com.carrental.service.DriverReturnService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class DriverReturnController {
    @Autowired private DriverReturnService service;

    @GetMapping("/api/driver/trips")
    public List<Reservation> trips(Authentication authentication) {
        return service.activeTrips(authentication.getName());
    }

    @GetMapping("/api/driver-return-reports")
    public List<DriverReturnReport> reports(Authentication authentication) {
        return service.list(authentication.getName());
    }

    @PostMapping("/api/driver-return-reports")
    public DriverReturnReport submit(@Valid @RequestBody DriverReturnRequest request, Authentication authentication) {
        return service.submit(request, authentication.getName());
    }
}
