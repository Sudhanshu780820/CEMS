package com.college.eventmanagement.controller;

import com.college.eventmanagement.dto.RegistrationResponse;
import com.college.eventmanagement.dto.StudentDashboardStats;
import com.college.eventmanagement.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/events/{id}/register")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<RegistrationResponse> registerForEvent(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(registrationService.registerForEvent(id, authentication.getName()));
    }

    @DeleteMapping("/events/{id}/register")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable Long id,
            Authentication authentication) {
        registrationService.cancelRegistration(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/student/registrations")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<List<RegistrationResponse>> getMyRegistrations(Authentication authentication) {
        return ResponseEntity.ok(registrationService.getStudentRegistrations(authentication.getName()));
    }

    @GetMapping("/student/dashboard")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<StudentDashboardStats> getStudentDashboard(Authentication authentication) {
        return ResponseEntity.ok(registrationService.getStudentDashboardStats(authentication.getName()));
    }
}
