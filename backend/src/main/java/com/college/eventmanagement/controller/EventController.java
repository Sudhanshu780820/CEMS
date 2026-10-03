package com.college.eventmanagement.controller;

import com.college.eventmanagement.dto.EventCreateRequest;
import com.college.eventmanagement.dto.EventResponse;
import com.college.eventmanagement.dto.OrganizerDashboardStats;
import com.college.eventmanagement.dto.RegistrationResponse;
import com.college.eventmanagement.entity.RegistrationStatus;
import com.college.eventmanagement.service.EventService;
import com.college.eventmanagement.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;
    private final RegistrationService registrationService;

    public EventController(EventService eventService, RegistrationService registrationService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
    }

    // Public / Student Event Discovery
    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getEvents(
            Authentication authentication,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(eventService.getEventsForDiscovery(email, query, category, startDate, endDate));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(eventService.getEventById(id, email));
    }

    @GetMapping("/events/check-conflict")
    public ResponseEntity<Map<String, Object>> checkConflict(
            @RequestParam Long venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) Long excludeEventId) {
        return ResponseEntity.ok(eventService.checkVenueConflict(venueId, eventDate, startTime, endTime, excludeEventId));
    }

    // Organizer Endpoints
    @GetMapping("/organizer/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<OrganizerDashboardStats> getOrganizerDashboard(Authentication authentication) {
        return ResponseEntity.ok(eventService.getOrganizerDashboardStats(authentication.getName()));
    }

    @GetMapping("/organizer/events")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<List<EventResponse>> getOrganizerEvents(Authentication authentication) {
        return ResponseEntity.ok(eventService.getOrganizerEvents(authentication.getName()));
    }

    @PostMapping("/organizer/events")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.createEvent(request, authentication.getName()));
    }

    @PutMapping("/organizer/events/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, authentication.getName()));
    }

    @DeleteMapping("/organizer/events/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<Void> cancelEvent(
            @PathVariable Long id,
            Authentication authentication) {
        eventService.cancelEvent(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/organizer/events/{id}/participants")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<List<RegistrationResponse>> getEventParticipants(
            @PathVariable Long id,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) RegistrationStatus status,
            Authentication authentication) {
        return ResponseEntity.ok(registrationService.getEventParticipants(id, authentication.getName(), query, branch, status));
    }

    @GetMapping(value = "/organizer/events/{id}/participants/export", produces = "text/csv")
    @PreAuthorize("hasAnyAuthority('ROLE_ORGANIZER', 'ROLE_ADMIN')")
    public ResponseEntity<String> exportParticipantsCsv(
            @PathVariable Long id,
            Authentication authentication) {
        String csv = registrationService.exportParticipantsCsv(id, authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"participants-event-" + id + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
