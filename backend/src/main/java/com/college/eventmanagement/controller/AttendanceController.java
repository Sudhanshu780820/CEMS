package com.college.eventmanagement.controller;

import com.college.eventmanagement.dto.AttendanceMarkRequest;
import com.college.eventmanagement.dto.AttendanceResponse;
import com.college.eventmanagement.dto.AttendanceStatsResponse;
import com.college.eventmanagement.dto.ManualAttendanceRequest;
import com.college.eventmanagement.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/organizer/events/{id}/attendance/start")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> startAttendanceSession(
            @PathVariable Long id,
            Authentication authentication) {
        String token = attendanceService.startAttendanceSession(id, authentication.getName());
        return ResponseEntity.ok(Collections.singletonMap("attendanceToken", token));
    }

    @PostMapping("/organizer/events/{id}/attendance/stop")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<Void> stopAttendanceSession(
            @PathVariable Long id,
            Authentication authentication) {
        attendanceService.stopAttendanceSession(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/events/{id}/attendance/mark")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<AttendanceResponse> markAttendanceViaQr(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceMarkRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(attendanceService.markAttendanceViaQr(id, request, authentication.getName()));
    }

    @PostMapping({"/organizer/events/{id}/attendance/manual", "/admin/events/{id}/attendance/manual"})
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> markManualAttendance(
            @PathVariable Long id,
            @Valid @RequestBody ManualAttendanceRequest request,
            Authentication authentication) {
        attendanceService.markManualAttendance(id, request, authentication.getName());
        return ResponseEntity.ok(Collections.singletonMap("message", "Attendance updated successfully"));
    }

    @GetMapping("/organizer/events/{id}/attendance/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<AttendanceStatsResponse> getAttendanceStats(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.getAttendanceStats(id));
    }

    @GetMapping("/student/attendance")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<List<AttendanceResponse>> getStudentAttendance(Authentication authentication) {
        return ResponseEntity.ok(attendanceService.getStudentAttendanceHistory(authentication.getName()));
    }
}
