package com.college.eventmanagement.controller;

import com.college.eventmanagement.dto.*;
import com.college.eventmanagement.entity.ApprovalStatus;
import com.college.eventmanagement.service.AdminService;
import com.college.eventmanagement.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final EventService eventService;

    public AdminController(AdminService adminService, EventService eventService) {
        this.adminService = adminService;
        this.eventService = eventService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStats> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/students/pending")
    public ResponseEntity<List<StudentResponse>> getPendingStudents() {
        return ResponseEntity.ok(adminService.getPendingStudents());
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentResponse>> getStudents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String currentYear,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) ApprovalStatus status) {
        return ResponseEntity.ok(adminService.searchStudents(query, branch, academicYear, currentYear, section, status));
    }

    @PutMapping("/students/{id}/approve")
    public ResponseEntity<StudentResponse> approveStudent(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveStudent(id));
    }

    @PutMapping("/students/{id}/reject")
    public ResponseEntity<StudentResponse> rejectStudent(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("rejectionReason") : "Administrative rejection";
        return ResponseEntity.ok(adminService.rejectStudent(id, reason));
    }

    @PutMapping("/students/{id}/status")
    public ResponseEntity<StudentResponse> updateStudentStatus(
            @PathVariable Long id,
            @Valid @RequestBody StudentApprovalRequest request) {
        return ResponseEntity.ok(adminService.updateStudentStatus(id, request.getStatus(), request.getRejectionReason()));
    }

    @GetMapping("/organizers")
    public ResponseEntity<List<OrganizerResponse>> getOrganizers() {
        return ResponseEntity.ok(adminService.getAllOrganizers());
    }

    @PostMapping("/organizers")
    public ResponseEntity<OrganizerResponse> createOrganizer(@Valid @RequestBody OrganizerCreateRequest request) {
        return ResponseEntity.ok(adminService.createOrganizer(request));
    }

    @PutMapping("/organizers/{id}/toggle-status")
    public ResponseEntity<OrganizerResponse> toggleOrganizerStatus(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleOrganizerStatus(id));
    }

    @GetMapping("/reports")
    public ResponseEntity<AdminReportsResponse> getReports() {
        return ResponseEntity.ok(adminService.getReports());
    }

    @GetMapping("/events/pending")
    public ResponseEntity<List<EventResponse>> getPendingEvents() {
        return ResponseEntity.ok(eventService.getPendingEvents());
    }

    @PutMapping("/events/{id}/approve")
    public ResponseEntity<EventResponse> approveEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.approveEvent(id));
    }

    @PutMapping("/events/{id}/reject")
    public ResponseEntity<EventResponse> rejectEvent(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("rejectionReason") : "Administrative rejection";
        return ResponseEntity.ok(eventService.rejectEvent(id, reason));
    }
}
