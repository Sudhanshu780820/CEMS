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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

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
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ROLE_ADMIN && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to manage attendance for this event.");
        }

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

    @Transactional
    public AttendanceResponse markAttendanceViaQr(Long eventId, AttendanceMarkRequest request, String studentEmail) {
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

        // 2. Verify attendance session is active
        if (!event.isAttendanceActive()) {
            throw new BadRequestException("Attendance session is not currently open for this event.");
        }

        // 3. Verify token matches
        if (event.getAttendanceToken() == null || !event.getAttendanceToken().equals(request.getAttendanceToken().trim())) {
            throw new BadRequestException("Invalid or expired attendance QR code.");
        }

        // 4. Verify student is registered for the event
        Registration registration = registrationRepository.findByEventIdAndStudentId(eventId, student.getId())
                .orElseThrow(() -> new BadRequestException("You are not registered for this event. Only registered students can mark attendance."));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BadRequestException("Your registration for this event was cancelled.");
        }

        // 5. Check if attendance already marked
        Optional<Attendance> existingAtt = attendanceRepository.findByEventIdAndStudentId(eventId, student.getId());
        if (existingAtt.isPresent() && existingAtt.get().getStatus() == AttendanceStatus.PRESENT) {
            throw new ConflictException("You have already checked in and your attendance is recorded.");
        }

        Attendance attendance;
        if (existingAtt.isPresent()) {
            attendance = existingAtt.get();
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendance.setMethod(AttendanceMethod.QR_SCAN);
            attendance.setCheckInTime(LocalDateTime.now());
        } else {
            attendance = new Attendance(event, student, AttendanceMethod.QR_SCAN, null, AttendanceStatus.PRESENT);
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
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ROLE_ADMIN && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to mark attendance for this event.");
        }

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
                    attendance.setCheckInTime(LocalDateTime.now());
                }
            } else {
                attendance = new Attendance(event, student, AttendanceMethod.MANUAL, user, request.getStatus());
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
