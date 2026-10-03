package com.college.eventmanagement.service;

import com.college.eventmanagement.dto.AttendanceMarkRequest;
import com.college.eventmanagement.dto.AttendanceResponse;
import com.college.eventmanagement.dto.AttendanceStatsResponse;
import com.college.eventmanagement.dto.ManualAttendanceRequest;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.repository.*;
import com.college.eventmanagement.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EventRepository eventRepository,
                             RegistrationRepository registrationRepository,
                             StudentRepository studentRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.attendanceRepository = attendanceRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public String startAttendanceSession(Long eventId, String userEmail) {
        return startAttendanceSession(eventId, userEmail, DateTimeUtil.now());
    }

    @Transactional
    public String startAttendanceSession(Long eventId, String userEmail, LocalDateTime currentTime) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ROLE_ADMIN && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to manage attendance for this event.");
        }

        validateAttendanceWindow(event, currentTime);

        String token = UUID.randomUUID().toString();
        event.setAttendanceToken(token);
        event.setAttendanceActive(true);
        eventRepository.save(event);

        return token;
    }

    @Transactional
    public void stopAttendanceSession(Long eventId, String userEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ROLE_ADMIN && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to manage attendance for this event.");
        }

        event.setAttendanceActive(false);
        eventRepository.save(event);
    }

    public boolean isAttendanceWindowOpen(Event event) {
        return isAttendanceWindowOpen(event, DateTimeUtil.now());
    }

    public boolean isAttendanceWindowOpen(Event event, LocalDateTime currentTime) {
        if (event == null) return false;
        return isWindowOpen(event.getEventDate(), event.getStartTime(), event.getEndTime(), currentTime);
    }

    public static boolean isWindowOpen(java.time.LocalDate eventDate, java.time.LocalTime startTime, java.time.LocalTime endTime, LocalDateTime currentTime) {
        if (eventDate == null || startTime == null || endTime == null || currentTime == null) return false;
        LocalDateTime startDateTime = LocalDateTime.of(eventDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(eventDate, endTime);
        LocalDateTime gracePeriodEnd = endDateTime.plusHours(24);
        return !currentTime.isBefore(startDateTime) && !currentTime.isAfter(gracePeriodEnd);
    }

    public void validateAttendanceWindow(Event event) {
        validateAttendanceWindow(event, DateTimeUtil.now());
    }

    public void validateAttendanceWindow(Event event, LocalDateTime currentTime) {
        if (event == null) {
            throw new BadRequestException("Event not found.");
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Attendance cannot be marked for a cancelled event.");
        }
        if (event.getStatus() == EventStatus.REJECTED) {
            throw new BadRequestException("Attendance cannot be marked for a rejected event.");
        }
        if (event.getStatus() == EventStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Attendance cannot be marked for an unapproved event.");
        }

        LocalDateTime startDateTime = LocalDateTime.of(event.getEventDate(), event.getStartTime());
        LocalDateTime endDateTime = LocalDateTime.of(event.getEventDate(), event.getEndTime());
        LocalDateTime gracePeriodEnd = endDateTime.plusHours(24);

        log.info("Attendance Window Check: eventId={}, eventDate={}, startTime={}, endTime={}, startDateTime={}, endDateTime={}, gracePeriodEnd={}, currentTime={}, zoneId={}",
                event.getId(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                startDateTime,
                endDateTime,
                gracePeriodEnd,
                currentTime,
                DateTimeUtil.getZoneId());

        if (currentTime.isBefore(startDateTime)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm a");
            throw new BadRequestException("Check-in has not started yet. Check-in opens at " + startDateTime.format(formatter) + ".");
        }

        if (currentTime.isAfter(gracePeriodEnd)) {
            throw new BadRequestException("Check-in is closed. The 24-hour check-in window has ended.");
        }
    }

    public static String determineAttendanceState(Event event, Attendance attendance, LocalDateTime currentTime) {
        if (attendance != null && attendance.getStatus() == AttendanceStatus.PRESENT) {
            return "PRESENT";
        }
        if (event == null || event.getEventDate() == null || event.getStartTime() == null || event.getEndTime() == null) {
            return "NOT_STARTED";
        }
        LocalDateTime startDateTime = LocalDateTime.of(event.getEventDate(), event.getStartTime());
        LocalDateTime endDateTime = LocalDateTime.of(event.getEventDate(), event.getEndTime());
        LocalDateTime gracePeriodEnd = endDateTime.plusHours(24);

        if (currentTime.isBefore(startDateTime)) {
            return "NOT_STARTED";
        } else if (!currentTime.isAfter(gracePeriodEnd)) {
            return "CHECK_IN_OPEN";
        } else {
            return "CHECK_IN_CLOSED";
        }
    }

    @Transactional
    public AttendanceResponse markAttendanceViaQr(Long eventId, AttendanceMarkRequest request, String studentEmail) {
        return markAttendanceViaQr(eventId, request, studentEmail, DateTimeUtil.now());
    }

    @Transactional
    public AttendanceResponse markAttendanceViaQr(Long eventId, AttendanceMarkRequest request, String studentEmail, LocalDateTime currentTime) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        // 1. Verify student account is APPROVED
        if (student.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BadRequestException("Your account is not approved. Only approved students can mark attendance.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // 2. Verify student is registered for the event
        Registration registration = registrationRepository.findByEventIdAndStudentId(eventId, student.getId())
                .orElseThrow(() -> new BadRequestException("You are not registered for this event. Only registered students can mark attendance."));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BadRequestException("Your registration for this event was cancelled.");
        }

        // 3. Check if attendance already marked PRESENT (Prevent duplicate / repeated check-in)
        Optional<Attendance> existingAtt = attendanceRepository.findByEventIdAndStudentId(eventId, student.getId());
        if (existingAtt.isPresent() && existingAtt.get().getStatus() == AttendanceStatus.PRESENT) {
            throw new ConflictException("Attendance has already been marked PRESENT.");
        }

        // 4. Validate authoritative attendance window
        validateAttendanceWindow(event, currentTime);

        // 5. Verify attendance session is active
        if (!event.isAttendanceActive()) {
            throw new BadRequestException("Attendance session is not currently open for this event.");
        }

        // 6. Verify token matches
        if (event.getAttendanceToken() == null || !event.getAttendanceToken().equals(request.getAttendanceToken().trim())) {
            throw new BadRequestException("Invalid or expired attendance QR code.");
        }

        Attendance attendance;
        if (existingAtt.isPresent()) {
            attendance = existingAtt.get();
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendance.setMethod(AttendanceMethod.QR_SCAN);
            attendance.setCheckInTime(currentTime);
        } else {
            attendance = new Attendance(event, student, AttendanceMethod.QR_SCAN, null, AttendanceStatus.PRESENT);
            attendance.setCheckInTime(currentTime);
        }
        attendance = attendanceRepository.save(attendance);

        // Update registration status to ATTENDED
        registration.setStatus(RegistrationStatus.ATTENDED);
        registrationRepository.save(registration);

        // Send confirmation notification
        notificationService.createNotification(
                user,
                "Attendance Confirmed",
                "Your attendance for '" + event.getTitle() + "' has been successfully verified and marked PRESENT.",
                NotificationType.ATTENDANCE_CONFIRMATION
        );

        return mapToResponse(attendance);
    }

    @Transactional
    public void markManualAttendance(Long eventId, ManualAttendanceRequest request, String userEmail) {
        markManualAttendance(eventId, request, userEmail, DateTimeUtil.now());
    }

    @Transactional
    public void markManualAttendance(Long eventId, ManualAttendanceRequest request, String userEmail, LocalDateTime currentTime) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ROLE_ADMIN && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to mark attendance for this event.");
        }

        // Authoritative attendance window validation for organizers and admins
        validateAttendanceWindow(event, currentTime);

        for (Long studentId : request.getStudentIds()) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) continue;

            Optional<Registration> regOpt = registrationRepository.findByEventIdAndStudentId(eventId, studentId);
            if (regOpt.isEmpty()) continue;

            Registration reg = regOpt.get();
            Optional<Attendance> attOpt = attendanceRepository.findByEventIdAndStudentId(eventId, studentId);

            Attendance attendance;
            if (attOpt.isPresent()) {
                attendance = attOpt.get();
                attendance.setStatus(request.getStatus());
                attendance.setMethod(AttendanceMethod.MANUAL);
                attendance.setMarkedByUser(user);
                if (request.getStatus() == AttendanceStatus.PRESENT && attendance.getCheckInTime() == null) {
                    attendance.setCheckInTime(currentTime);
                }
            } else {
                attendance = new Attendance(event, student, AttendanceMethod.MANUAL, user, request.getStatus());
                if (request.getStatus() == AttendanceStatus.PRESENT) {
                    attendance.setCheckInTime(currentTime);
                }
            }
            attendanceRepository.save(attendance);

            if (request.getStatus() == AttendanceStatus.PRESENT) {
                reg.setStatus(RegistrationStatus.ATTENDED);
            } else {
                reg.setStatus(RegistrationStatus.ABSENT);
            }
            registrationRepository.save(reg);
        }
    }

    @Transactional(readOnly = true)
    public AttendanceStatsResponse getAttendanceStats(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        long totalRegistered = registrationRepository.countByEventId(eventId);
        long presentCount = attendanceRepository.countByEventIdAndStatus(eventId, AttendanceStatus.PRESENT);
        long absentCount = Math.max(0, totalRegistered - presentCount);

        double percentage = totalRegistered > 0
                ? Math.round(((double) presentCount / totalRegistered) * 1000.0) / 10.0
                : 0.0;

        return new AttendanceStatsResponse(totalRegistered, presentCount, absentCount, percentage);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getStudentAttendanceHistory(String studentEmail) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        return attendanceRepository.findByStudentOrderByCheckInTimeDesc(student).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AttendanceResponse mapToResponse(Attendance a) {
        AttendanceResponse res = new AttendanceResponse();
        res.setId(a.getId());
        res.setEventId(a.getEvent().getId());
        res.setEventTitle(a.getEvent().getTitle());
        res.setStudentId(a.getStudent().getId());
        res.setStudentName(a.getStudent().getFullName());
        res.setEnrollmentId(a.getStudent().getEnrollmentId());
        res.setBranch(a.getStudent().getBranch());
        res.setSection(a.getStudent().getSection());
        res.setCheckInTime(a.getCheckInTime());
        res.setMethod(a.getMethod());
        res.setStatus(a.getStatus());
        return res;
    }
}
