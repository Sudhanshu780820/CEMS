package com.college.eventmanagement.service;

import com.college.eventmanagement.dto.*;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final OrganizerRepository organizerRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                        StudentRepository studentRepository,
                        OrganizerRepository organizerRepository,
                        EventRepository eventRepository,
                        RegistrationRepository registrationRepository,
                        AttendanceRepository attendanceRepository,
                        PasswordEncoder passwordEncoder,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.organizerRepository = organizerRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        long totalStudents = studentRepository.count();
        long pendingStudents = studentRepository.countByApprovalStatus(ApprovalStatus.PENDING);
        long approvedStudents = studentRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
        long totalEvents = eventRepository.count();
        long upcomingEvents = eventRepository.findUpcomingPublishedEvents(LocalDate.now()).size();
        long totalOrganizers = organizerRepository.count();
        long totalRegistrations = registrationRepository.count();

        long attendedCount = attendanceRepository.count();
        double overallAttendanceRate = totalRegistrations > 0
                ? Math.round(((double) attendedCount / totalRegistrations) * 1000.0) / 10.0
                : 0.0;

        return new AdminDashboardStats(
                totalStudents,
                pendingStudents,
                approvedStudents,
                totalEvents,
                upcomingEvents,
                totalOrganizers,
                totalRegistrations,
                overallAttendanceRate
        );
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getPendingStudents() {
        return studentRepository.findByApprovalStatus(ApprovalStatus.PENDING).stream()
                .map(this::mapStudentToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> searchStudents(String query, String branch, String academicYear,
                                                String currentYear, String section, ApprovalStatus status) {
        return studentRepository.searchStudents(
                (query != null && !query.isBlank()) ? query.trim() : null,
                (branch != null && !branch.isBlank()) ? branch.trim() : null,
                (academicYear != null && !academicYear.isBlank()) ? academicYear.trim() : null,
                (currentYear != null && !currentYear.isBlank()) ? currentYear.trim() : null,
                (section != null && !section.isBlank()) ? section.trim() : null,
                status
        ).stream().map(this::mapStudentToResponse).collect(Collectors.toList());
    }

    @Transactional
    public StudentResponse approveStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setApprovalStatus(ApprovalStatus.APPROVED);
        student.setRejectionReason(null);
        student = studentRepository.save(student);

        // Notify student
        notificationService.createNotification(
                student.getUser(),
                "Account Approved",
                "Congratulations " + student.getFullName() + "! Your college student registration has been verified and APPROVED by the administrator. You now have full access to discover and register for events.",
                NotificationType.ACCOUNT_APPROVAL
        );

        return mapStudentToResponse(student);
    }

    @Transactional
    public StudentResponse rejectStudent(Long studentId, String rejectionReason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setApprovalStatus(ApprovalStatus.REJECTED);
        student.setRejectionReason(rejectionReason != null ? rejectionReason : "Enrollment verification failed.");
        student = studentRepository.save(student);

        // Notify student
        notificationService.createNotification(
                student.getUser(),
                "Registration Rejected",
                "Your student registration was rejected. Reason: " + student.getRejectionReason(),
                NotificationType.ACCOUNT_REJECTION
        );

        return mapStudentToResponse(student);
    }

    @Transactional
    public StudentResponse updateStudentStatus(Long studentId, ApprovalStatus status, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setApprovalStatus(status);
        if (reason != null && !reason.isBlank()) {
            student.setRejectionReason(reason);
        }
        student = studentRepository.save(student);

        return mapStudentToResponse(student);
    }

    @Transactional(readOnly = true)
    public List<OrganizerResponse> getAllOrganizers() {
        return organizerRepository.findAll().stream()
                .map(this::mapOrganizerToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrganizerResponse createOrganizer(OrganizerCreateRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " is already in use.");
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_ORGANIZER
        );
        user = userRepository.save(user);

        Organizer organizer = new Organizer();
        organizer.setUser(user);
        organizer.setName(request.getName().trim());
        organizer.setDepartment(request.getDepartment().trim());
        organizer.setContactEmail(email);
        organizer.setPhoneNumber(request.getPhoneNumber());
        organizer.setApproved(true);
        organizer = organizerRepository.save(organizer);

        return mapOrganizerToResponse(organizer);
    }

    @Transactional
    public OrganizerResponse toggleOrganizerStatus(Long organizerId) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found with id: " + organizerId));

        boolean newActive = !organizer.getUser().isActive();
        organizer.getUser().setActive(newActive);
        userRepository.save(organizer.getUser());
        organizer.setApproved(newActive);
        organizer = organizerRepository.save(organizer);

        return mapOrganizerToResponse(organizer);
    }

    @Transactional(readOnly = true)
    public AdminReportsResponse getReports() {
        AdminReportsResponse response = new AdminReportsResponse();
        long totalEvents = eventRepository.count();
        long totalRegistrations = registrationRepository.count();
        long totalAttendance = attendanceRepository.count();
        double overallRate = totalRegistrations > 0
                ? Math.round(((double) totalAttendance / totalRegistrations) * 1000.0) / 10.0
                : 0.0;

        response.setTotalEvents(totalEvents);
        response.setTotalRegistrations(totalRegistrations);
        response.setTotalAttendance(totalAttendance);
        response.setOverallAttendancePercentage(overallRate);

        // Group events by category
        Map<String, Long> eventsByCategory = eventRepository.findAll().stream()
                .collect(Collectors.groupingBy(Event::getCategory, Collectors.counting()));
        response.setEventsByCategory(eventsByCategory);

        // Group events by branch eligibility
        Map<String, Long> eventsByBranch = new HashMap<>();
        List<Event> allEvents = eventRepository.findAll();
        for (Event e : allEvents) {
            String branches = e.getEligibleBranches();
            if (branches == null || branches.isBlank()) {
                eventsByBranch.put("ALL BRANCHES", eventsByBranch.getOrDefault("ALL BRANCHES", 0L) + 1);
            } else {
                for (String b : branches.split(",")) {
                    String trim = b.trim();
                    if (!trim.isEmpty()) {
                        eventsByBranch.put(trim, eventsByBranch.getOrDefault(trim, 0L) + 1);
                    }
                }
            }
        }
        response.setEventsByBranch(eventsByBranch);

        // Most popular events (by registration count)
        List<Map<String, Object>> popularEvents = allEvents.stream()
                .sorted((a, b) -> Integer.compare(b.getRegisteredCount(), a.getRegisteredCount()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getId());
                    map.put("title", e.getTitle());
                    map.put("registeredCount", e.getRegisteredCount());
                    map.put("maxCapacity", e.getMaxCapacity());
                    map.put("category", e.getCategory());
                    return map;
                })
                .collect(Collectors.toList());
        response.setMostPopularEvents(popularEvents);

        // Student participation by branch
        List<Registration> registrations = registrationRepository.findAll();
        Map<String, Long> participation = registrations.stream()
                .map(r -> r.getStudent().getBranch())
                .collect(Collectors.groupingBy(b -> b, Collectors.counting()));
        response.setStudentParticipationByBranch(participation);

        return response;
    }

    private StudentResponse mapStudentToResponse(Student s) {
        StudentResponse r = new StudentResponse();
        r.setId(s.getId());
        r.setUserId(s.getUser().getId());
        r.setFullName(s.getFullName());
        r.setEmail(s.getUser().getEmail());
        r.setEnrollmentId(s.getEnrollmentId());
        r.setBranch(s.getBranch());
        r.setAcademicYear(s.getAcademicYear());
        r.setCurrentYear(s.getCurrentYear());
        r.setCurrentSemester(s.getCurrentSemester());
        r.setSection(s.getSection());
        r.setPhoneNumber(s.getPhoneNumber());
        r.setApprovalStatus(s.getApprovalStatus());
        r.setRejectionReason(s.getRejectionReason());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }

    private OrganizerResponse mapOrganizerToResponse(Organizer o) {
        OrganizerResponse r = new OrganizerResponse();
        r.setId(o.getId());
        r.setUserId(o.getUser().getId());
        r.setName(o.getName());
        r.setDepartment(o.getDepartment());
        r.setContactEmail(o.getContactEmail());
        r.setPhoneNumber(o.getPhoneNumber());
        r.setApproved(o.isApproved());
        r.setActive(o.getUser().isActive());
        r.setCreatedAt(o.getCreatedAt());
        return r;
    }
}
