package com.college.eventmanagement.service;

import com.college.eventmanagement.dto.AuthRequest;
import com.college.eventmanagement.dto.AuthResponse;
import com.college.eventmanagement.dto.StudentRegisterRequest;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.repository.OrganizerRepository;
import com.college.eventmanagement.repository.StudentRepository;
import com.college.eventmanagement.repository.UserRepository;
import com.college.eventmanagement.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final OrganizerRepository organizerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       OrganizerRepository organizerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.organizerRepository = organizerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.notificationService = notificationService;
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String identifier = request.getUsernameOrEnrollmentId().trim();
        Optional<User> userOpt = userRepository.findByEmail(identifier);

        if (userOpt.isEmpty()) {
            Optional<Student> studentOpt = studentRepository.findByEnrollmentId(identifier);
            if (studentOpt.isPresent()) {
                userOpt = Optional.of(studentOpt.get().getUser());
            }
        }

        User user = userOpt.orElseThrow(() ->
                new BadRequestException("Invalid email/enrollment ID or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email/enrollment ID or password");
        }

        if (!user.isActive()) {
            throw new BadRequestException("Your account has been deactivated. Please contact the administrator.");
        }

        String displayName = user.getEmail();
        ApprovalStatus studentStatus = null;
        String enrollmentId = null;
        String branch = null;

        // If user is a student, check approval status
        if (user.getRole() == Role.ROLE_STUDENT) {
            Student student = studentRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

            studentStatus = student.getApprovalStatus();
            enrollmentId = student.getEnrollmentId();
            branch = student.getBranch();
            displayName = student.getFullName();

            if (student.getApprovalStatus() == ApprovalStatus.PENDING) {
                throw new BadRequestException("Your account registration is currently PENDING administrative approval. You will receive access once college administrators verify your enrollment records.");
            } else if (student.getApprovalStatus() == ApprovalStatus.REJECTED) {
                String reason = student.getRejectionReason() != null ? student.getRejectionReason() : "Enrollment record mismatch";
                throw new BadRequestException("Your account registration was REJECTED by the college administrator. Reason: " + reason);
            } else if (student.getApprovalStatus() == ApprovalStatus.SUSPENDED) {
                throw new BadRequestException("Your account has been SUSPENDED by the administrator. Please contact the college administration office.");
            }
        } else if (user.getRole() == Role.ROLE_ORGANIZER) {
            Organizer organizer = organizerRepository.findByUserId(user.getId()).orElse(null);
            if (organizer != null) {
                displayName = organizer.getName();
            }
        } else if (user.getRole() == Role.ROLE_ADMIN) {
            displayName = "System Administrator";
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                displayName,
                studentStatus,
                enrollmentId,
                branch
        );
    }

    @Transactional
    public AuthResponse registerStudent(StudentRegisterRequest request) {
        validateRegistrationDetails(request);

        String email = request.getEmail().trim().toLowerCase();
        String enrollmentId = request.getEnrollmentId().trim().toUpperCase();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("This email is already registered.");
        }

        if (studentRepository.existsByEnrollmentId(enrollmentId)) {
            throw new ConflictException("This enrollment ID is already registered.");
        }

        // Create User account
        User user = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_STUDENT
        );
        user = userRepository.save(user);

        // Create Student profile in PENDING state
        Student student = new Student();
        student.setUser(user);
        student.setFullName(request.getFullName().trim());
        student.setEnrollmentId(enrollmentId);
        student.setBranch(request.getBranch().trim());
        student.setAcademicYear(request.getAcademicYear().trim());
        student.setCurrentYear(request.getCurrentYear().trim());
        student.setCurrentSemester(request.getCurrentSemester().trim());
        student.setSection(request.getSection().trim().toUpperCase());
        String phone = request.getPhoneNumber();
        student.setPhoneNumber(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);
        student.setApprovalStatus(ApprovalStatus.PENDING);
        student = studentRepository.save(student);

        // Welcome notification
        notificationService.createNotification(
                user,
                "Registration Submitted",
                "Your registration for " + student.getFullName() + " (" + enrollmentId + ") has been submitted. Status: PENDING administrative approval.",
                NotificationType.GENERAL
        );

        return new AuthResponse(
                null, // No token until approved
                user.getId(),
                user.getEmail(),
                user.getRole(),
                student.getFullName(),
                student.getApprovalStatus(),
                student.getEnrollmentId(),
                student.getBranch()
        );
    }

    public AuthResponse getCurrentUserContext(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String displayName = user.getEmail();
        ApprovalStatus studentStatus = null;
        String enrollmentId = null;
        String branch = null;

        if (user.getRole() == Role.ROLE_STUDENT) {
            Student student = studentRepository.findByUserId(user.getId()).orElse(null);
            if (student != null) {
                studentStatus = student.getApprovalStatus();
                enrollmentId = student.getEnrollmentId();
                branch = student.getBranch();
                displayName = student.getFullName();
            }
        } else if (user.getRole() == Role.ROLE_ORGANIZER) {
            Organizer organizer = organizerRepository.findByUserId(user.getId()).orElse(null);
            if (organizer != null) {
                displayName = organizer.getName();
            }
        } else if (user.getRole() == Role.ROLE_ADMIN) {
            displayName = "System Administrator";
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                displayName,
                studentStatus,
                enrollmentId,
                branch
        );
    }

    private void validateRegistrationDetails(StudentRegisterRequest request) {
        // 1. Required fields presence
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new BadRequestException("Full Name is required");
        }
        if (request.getEnrollmentId() == null || request.getEnrollmentId().trim().isEmpty()) {
            throw new BadRequestException("Enrollment ID is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (request.getBranch() == null || request.getBranch().trim().isEmpty()) {
            throw new BadRequestException("Branch is required");
        }
        if (request.getSection() == null || request.getSection().trim().isEmpty()) {
            throw new BadRequestException("Section is required");
        }

        // 2. Password validation: >= 6 chars, at least 1 letter, at least 1 digit
        String password = request.getPassword();
        if (password == null || password.length() < 6 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
            throw new BadRequestException("Password must contain at least one letter and one number.");
        }

        // 3. Academic Session format and duration validation
        String academicYear = request.getAcademicYear() != null ? request.getAcademicYear().trim() : "";
        Matcher sessionMatcher = Pattern.compile("^(\\d{4})-(\\d{4})$").matcher(academicYear);
        if (!sessionMatcher.matches()) {
            throw new BadRequestException("Academic session must be in format YYYY-YYYY.");
        }
        int startYear = Integer.parseInt(sessionMatcher.group(1));
        int endYear = Integer.parseInt(sessionMatcher.group(2));
        if (endYear <= startYear) {
            throw new BadRequestException("End year must be after start year.");
        }
        int duration = endYear - startYear;
        if (duration > 5) {
            throw new BadRequestException("Academic session cannot be longer than 5 years.");
        }
        if (duration < 1) {
            throw new BadRequestException("Academic session must be between 1 and 5 years.");
        }

        // 4. Current Year vs Academic Session Duration
        int yearNum = parseYearNumber(request.getCurrentYear());
        if (yearNum < 1 || yearNum > 4) {
            throw new BadRequestException("Invalid Current Year.");
        }
        if (yearNum > duration) {
            throw new BadRequestException("Selected year is not available for this academic session.");
        }

        // 5. Semester vs Current Year Mapping
        int semNum = parseSemesterNumber(request.getCurrentSemester());
        if (semNum < 1 || semNum > 8) {
            throw new BadRequestException("Invalid Current Semester.");
        }
        int expectedMinSem = 2 * yearNum - 1;
        int expectedMaxSem = 2 * yearNum;
        if (semNum != expectedMinSem && semNum != expectedMaxSem) {
            throw new BadRequestException("Selected semester is not valid for the selected academic year.");
        }

        // 6. Optional Phone Number validation (if provided, must be exactly 10 digits)
        String phone = request.getPhoneNumber();
        if (phone != null && !phone.trim().isEmpty()) {
            if (!phone.trim().matches("^[0-9]{10}$")) {
                throw new BadRequestException("Phone number must contain exactly 10 digits.");
            }
        }
    }

    private int parseYearNumber(String currentYear) {
        if (currentYear == null) return -1;
        String s = currentYear.trim().toLowerCase();
        if (s.startsWith("1")) return 1;
        if (s.startsWith("2")) return 2;
        if (s.startsWith("3")) return 3;
        if (s.startsWith("4")) return 4;
        return -1;
    }

    private int parseSemesterNumber(String currentSemester) {
        if (currentSemester == null) return -1;
        Matcher m = Pattern.compile("([1-8])").matcher(currentSemester.trim());
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }
}
