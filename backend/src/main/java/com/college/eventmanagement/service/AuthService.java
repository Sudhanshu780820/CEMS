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
        String email = request.getEmail().trim().toLowerCase();
        String enrollmentId = request.getEnrollmentId().trim().toUpperCase();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " is already registered");
        }

        if (studentRepository.existsByEnrollmentId(enrollmentId)) {
            throw new ConflictException("Enrollment ID " + enrollmentId + " is already registered");
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
        student.setPhoneNumber(request.getPhoneNumber());
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
}
