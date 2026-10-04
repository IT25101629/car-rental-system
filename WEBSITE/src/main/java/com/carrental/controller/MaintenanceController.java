package com.carrental.controller;

import com.carrental.model.MaintenanceRequest;
import com.carrental.service.MaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {
    @Autowired private MaintenanceService service;
    public record CompleteRepairRequest(@NotBlank @Size(max = 1000) String repairNotes) {}

    @GetMapping
    public List<MaintenanceRequest> list() { return service.list(); }

    @PatchMapping("/{id}/start")
    public MaintenanceRequest start(@PathVariable Long id, Authentication authentication) {
        return service.start(id, authentication.getName());
    }

    @PatchMapping("/{id}/complete")
    public MaintenanceRequest complete(@PathVariable Long id, @Valid @RequestBody CompleteRepairRequest request, Authentication authentication) {
        return service.complete(id, request.repairNotes(), authentication.getName());
    }
}
