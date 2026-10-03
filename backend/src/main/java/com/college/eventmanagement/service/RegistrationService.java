package com.college.eventmanagement.service;

import com.college.eventmanagement.dto.RegistrationResponse;
import com.college.eventmanagement.dto.StudentDashboardStats;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.college.eventmanagement.util.DateTimeUtil;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventService eventService;
    private final NotificationService notificationService;

    public RegistrationService(RegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               StudentRepository studentRepository,
                               UserRepository userRepository,
                               AttendanceRepository attendanceRepository,
                               EventService eventService,
                               NotificationService notificationService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.eventService = eventService;
        this.notificationService = notificationService;
    }

    @Transactional
    public RegistrationResponse registerForEvent(Long eventId, String studentEmail) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        // 1. Verify student account is APPROVED
        if (student.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BadRequestException("Your account is not approved yet. Only approved students can register for events.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // 2. Verify event is published
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BadRequestException("Registration is not available for this event (Status: " + event.getStatus() + ").");
        }

        // 3. Verify registration window
        LocalDate today = DateTimeUtil.today();
        if (today.isBefore(event.getRegistrationStartDate())) {
            throw new BadRequestException("Registration has not started yet. Registration opens on "
                    + event.getRegistrationStartDate() + ".");
        }
        if (today.isAfter(event.getRegistrationEndDate())) {
            throw new BadRequestException("Registration is closed. Registration ended on "
                    + event.getRegistrationEndDate() + ".");
        }

        // 4. Verify student eligibility (Double-checked on backend!)
        if (!eventService.checkStudentEligibility(event, student)) {
            throw new BadRequestException("You are not eligible to register for this event.");
        }

        // 5. Verify student has not already registered
        Optional<Registration> existingReg = registrationRepository.findByEventIdAndStudentId(eventId, student.getId());
        if (existingReg.isPresent()) {
            if (existingReg.get().getStatus() == RegistrationStatus.CANCELLED) {
                registrationRepository.delete(existingReg.get());
                registrationRepository.flush();
            } else {
                throw new ConflictException("You have already registered for this event.");
            }
        }

        // 6. Concurrency-safe atomic capacity increment
        int rowsUpdated = eventRepository.incrementRegisteredCountIfAvailable(eventId);
        if (rowsUpdated == 0) {
            throw new ConflictException("This event has reached its maximum seat capacity.");
        }

        // 7. Save Registration
        Registration registration = new Registration(event, student);
        registration = registrationRepository.save(registration);

        // 8. In-App Notifications
        notificationService.createNotification(
                user,
                "Registration Confirmed",
                "You have successfully registered for '" + event.getTitle() + "' scheduled on "
                + event.getEventDate() + " at " + event.getStartTime() + " (" + event.getVenue().getName() + ").",
                NotificationType.REGISTRATION_CONFIRMATION
        );

        // Notify organizer if nearly full (>= 90% capacity)
        if (event.getRegisteredCount() + 1 >= (event.getMaxCapacity() * 0.9)) {
            notificationService.createNotification(
                    event.getOrganizer().getUser(),
                    "Event Capacity Alert",
                    "Event '" + event.getTitle() + "' is nearly full! Current registrations: "
                    + (event.getRegisteredCount() + 1) + " / " + event.getMaxCapacity(),
                    NotificationType.CAPACITY_ALERT
            );
        }

        return mapToResponse(registration);
    }

    @Transactional
    public void cancelRegistration(Long eventId, String studentEmail) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        Registration registration = registrationRepository.findByEventIdAndStudentId(eventId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registration record not found for this event."));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BadRequestException("Registration is already cancelled.");
        }

        // Decrement capacity
        eventRepository.decrementRegisteredCount(eventId);

        // Delete attendance record if any exists
        attendanceRepository.findByEventIdAndStudentId(eventId, student.getId())
                .ifPresent(attendanceRepository::delete);

        // Delete registration record to free the slot and allow re-registration
        registrationRepository.delete(registration);

        notificationService.createNotification(
                user,
                "Registration Cancelled",
                "Your registration for '" + registration.getEvent().getTitle() + "' has been cancelled.",
                NotificationType.REGISTRATION_CONFIRMATION
        );
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getStudentRegistrations(String studentEmail) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        return registrationRepository.findByStudentOrderByRegisteredAtDesc(student).stream()
                .filter(r -> r.getStatus() != RegistrationStatus.CANCELLED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getEventParticipants(Long eventId, String userEmail, String query, String branch, RegistrationStatus status) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ROLE_ADMIN && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to view participants for this event.");
        }

        return registrationRepository.filterParticipants(
                eventId,
                (query != null && !query.isBlank()) ? query.trim() : null,
                (branch != null && !branch.isBlank()) ? branch.trim() : null,
                status
        ).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String exportParticipantsCsv(Long eventId, String userEmail) {
        List<RegistrationResponse> participants = getEventParticipants(eventId, userEmail, null, null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("Name,Enrollment ID,Branch,Section,Registration Date,Registration Status,Attendance Status\n");
        for (RegistrationResponse p : participants) {
            sb.append("\"").append(p.getStudentName()).append("\",")
              .append("\"").append(p.getEnrollmentId()).append("\",")
              .append("\"").append(p.getBranch()).append("\",")
              .append("\"").append(p.getSection()).append("\",")
              .append("\"").append(p.getRegisteredAt()).append("\",")
              .append("\"").append(p.getStatus()).append("\",")
              .append("\"").append(p.getAttendanceStatus() != null ? p.getAttendanceStatus() : "ABSENT").append("\"\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public StudentDashboardStats getStudentDashboardStats(String studentEmail) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        long registered = registrationRepository.countByStudentAndStatus(student, RegistrationStatus.REGISTERED)
                + registrationRepository.countByStudentAndStatus(student, RegistrationStatus.ATTENDED);
        long attended = attendanceRepository.countByStudentAndStatus(student, AttendanceStatus.PRESENT);
        long available = eventRepository.findUpcomingPublishedEvents(DateTimeUtil.today(), DateTimeUtil.currentTime()).size();

        return new StudentDashboardStats(registered, attended, available);
    }

    private RegistrationResponse mapToResponse(Registration r) {
        RegistrationResponse res = new RegistrationResponse();
        res.setId(r.getId());
        res.setEventId(r.getEvent().getId());
        res.setEventTitle(r.getEvent().getTitle());
        res.setCategory(r.getEvent().getCategory());
        res.setEventDate(r.getEvent().getEventDate());
        res.setStartTime(r.getEvent().getStartTime());
        res.setEndTime(r.getEvent().getEndTime());
        res.setVenueName(r.getEvent().getVenue().getName());
        res.setOrganizerName(r.getEvent().getOrganizer().getName());

        res.setStudentId(r.getStudent().getId());
        res.setStudentName(r.getStudent().getFullName());
        res.setEnrollmentId(r.getStudent().getEnrollmentId());
        res.setBranch(r.getStudent().getBranch());
        res.setSection(r.getStudent().getSection());
        res.setStatus(r.getStatus());
        res.setRegisteredAt(r.getRegisteredAt());

        Optional<Attendance> att = attendanceRepository.findByEventIdAndStudentId(r.getEvent().getId(), r.getStudent().getId());
        Attendance attendance = att.orElse(null);
        String attRecordStatus = (attendance != null && attendance.getStatus() == AttendanceStatus.PRESENT) ? "PRESENT" : "ABSENT";
        res.setAttendanceStatus(attRecordStatus);

        LocalDateTime now = DateTimeUtil.now();
        String attState = AttendanceService.determineAttendanceState(r.getEvent(), attendance, now);
        res.setAttendanceState(attState);

        boolean checkInAllowed = "CHECK_IN_OPEN".equals(attState) && (attendance == null || attendance.getStatus() != AttendanceStatus.PRESENT);
        res.setCheckInAllowed(checkInAllowed);

        if (r.getEvent().getEventDate() != null && r.getEvent().getStartTime() != null) {
            res.setCheckInOpensAt(LocalDateTime.of(r.getEvent().getEventDate(), r.getEvent().getStartTime()));
        }
        if (r.getEvent().getEventDate() != null && r.getEvent().getEndTime() != null) {
            res.setCheckInClosesAt(LocalDateTime.of(r.getEvent().getEventDate(), r.getEvent().getEndTime()).plusHours(24));
        }

        return res;
    }
}
