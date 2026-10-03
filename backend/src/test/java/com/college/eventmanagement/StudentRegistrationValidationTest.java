package com.college.eventmanagement;

import com.college.eventmanagement.dto.StudentRegisterRequest;
import com.college.eventmanagement.entity.ApprovalStatus;
import com.college.eventmanagement.entity.Role;
import com.college.eventmanagement.entity.Student;
import com.college.eventmanagement.entity.User;
import com.college.eventmanagement.entity.Notification;
import com.college.eventmanagement.entity.NotificationType;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.repository.OrganizerRepository;
import com.college.eventmanagement.repository.StudentRepository;
import com.college.eventmanagement.repository.UserRepository;
import com.college.eventmanagement.security.JwtUtils;
import com.college.eventmanagement.service.AuthService;
import com.college.eventmanagement.service.NotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class StudentRegistrationValidationTest {

    private AuthService authService;
    private Validator validator;

    private boolean emailExists = false;
    private boolean enrollmentExists = false;
    private boolean userSaved = false;
    private boolean studentSaved = false;

    @BeforeEach
    void setUp() {
        emailExists = false;
        enrollmentExists = false;
        userSaved = false;
        studentSaved = false;

        // Dynamic Proxy for UserRepository
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("existsByEmail".equals(name)) {
                        return emailExists;
                    }
                    if ("save".equals(name)) {
                        userSaved = true;
                        User u = (User) args[0];
                        u.setId(101L);
                        return u;
                    }
                    if ("findByEmail".equals(name)) {
                        return Optional.empty();
                    }
                    return null;
                }
        );

        // Dynamic Proxy for StudentRepository
        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("existsByEnrollmentId".equals(name)) {
                        return enrollmentExists;
                    }
                    if ("save".equals(name)) {
                        studentSaved = true;
                        Student s = (Student) args[0];
                        s.setId(202L);
                        s.setApprovalStatus(ApprovalStatus.PENDING);
                        return s;
                    }
                    return null;
                }
        );

        OrganizerRepository organizerRepository = (OrganizerRepository) Proxy.newProxyInstance(
                OrganizerRepository.class.getClassLoader(),
                new Class<?>[]{OrganizerRepository.class},
                (proxy, method, args) -> null
        );

        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "$2a$10$hashedPasswordSample";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return true;
            }
        };

        JwtUtils jwtUtils = new JwtUtils();

        NotificationService notificationService = new NotificationService(null, null) {
            @Override
            public Notification createNotification(User user, String title, String message, NotificationType type) {
                return null;
            }
        };

        authService = new AuthService(
                userRepository,
                studentRepository,
                organizerRepository,
                passwordEncoder,
                jwtUtils,
                notificationService
        );

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private StudentRegisterRequest createValidRequest() {
        StudentRegisterRequest req = new StudentRegisterRequest();
        req.setFullName("Rahul Sharma");
        req.setEmail("rahul.sharma@college.edu");
        req.setPassword("Rahul123");
        req.setEnrollmentId("EN2026CSE001");
        req.setBranch("Computer Science");
        req.setAcademicYear("2026-2030");
        req.setCurrentYear("1st Year");
        req.setCurrentSemester("Semester 1");
        req.setSection("A");
        req.setPhoneNumber("9876543210");
        return req;
    }

    @Nested
    @DisplayName("1. Password Validation Tests")
    class PasswordTests {
        @Test
        @DisplayName("123456 -> rejected (no letters)")
        void testOnlyNumbersRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setPassword("123456");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Password must contain at least one letter and one number.", ex.getMessage());

            Set<ConstraintViolation<StudentRegisterRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("abcdef -> rejected (no numbers)")
        void testOnlyLettersRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setPassword("abcdef");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Password must contain at least one letter and one number.", ex.getMessage());

            Set<ConstraintViolation<StudentRegisterRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("12345 -> rejected (too short)")
        void testTooShortRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setPassword("12345");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Password must contain at least one letter and one number.", ex.getMessage());

            Set<ConstraintViolation<StudentRegisterRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("abc123 -> accepted")
        void testAbc123Accepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setPassword("abc123");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(userSaved);
            assertTrue(studentSaved);
        }

        @Test
        @DisplayName("Password1 -> accepted")
        void testPassword1Accepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setPassword("Password1");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(userSaved);
            assertTrue(studentSaved);
        }
    }

    @Nested
    @DisplayName("2. Academic Session Validation Tests")
    class AcademicSessionTests {
        @Test
        @DisplayName("2026 -> 2027 accepted (1 year)")
        void testOneYearAccepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("2026-2027");
            req.setCurrentYear("1st Year");
            req.setCurrentSemester("Semester 1");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(userSaved);
        }

        @Test
        @DisplayName("2026 -> 2031 accepted (5 years)")
        void testFiveYearsAccepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("2026-2031");
            req.setCurrentYear("4th Year");
            req.setCurrentSemester("Semester 7");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(userSaved);
        }

        @Test
        @DisplayName("2026 -> 2032 rejected (6 years)")
        void testSixYearsRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("2026-2032");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Academic session cannot be longer than 5 years.", ex.getMessage());
        }

        @Test
        @DisplayName("2026 -> 2026 rejected (0 years)")
        void testZeroYearsRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("2026-2026");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("End year must be after start year.", ex.getMessage());
        }

        @Test
        @DisplayName("2027 -> 2026 rejected (negative)")
        void testNegativeYearsRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("2027-2026");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("End year must be after start year.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("3. Current Year Tests")
    class CurrentYearTests {
        @Test
        @DisplayName("Selecting 3rd Year for a 2-year program is rejected")
        void testThirdYearInTwoYearProgramRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("2026-2028"); // 2-year program
            req.setCurrentYear("3rd Year");
            req.setCurrentSemester("Semester 5");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Selected year is not available for this academic session.", ex.getMessage());
        }

        @Test
        @DisplayName("Selecting 1st and 2nd Year for a 2-year program is accepted")
        void testYearsInTwoYearProgramAccepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("2026-2028");
            req.setCurrentYear("2nd Year");
            req.setCurrentSemester("Semester 3");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(userSaved);
        }

        @Test
        @DisplayName("Selecting 2nd Year for a 1-year program is rejected")
        void testSecondYearInOneYearProgramRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("2026-2027"); // 1-year program
            req.setCurrentYear("2nd Year");
            req.setCurrentSemester("Semester 3");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Selected year is not available for this academic session.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("4. Current Semester Tests")
    class SemesterTests {
        @Test
        @DisplayName("1st Year allows only Semester 1 and 2; Semester 3 is rejected")
        void testSemester3InFirstYearRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setCurrentYear("1st Year");
            req.setCurrentSemester("Semester 3");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Selected semester is not valid for the selected academic year.", ex.getMessage());
        }

        @Test
        @DisplayName("2nd Year with Semester 1 is rejected")
        void testSemester1InSecondYearRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setCurrentYear("2nd Year");
            req.setCurrentSemester("Semester 1");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Selected semester is not valid for the selected academic year.", ex.getMessage());
        }

        @Test
        @DisplayName("3rd Year with Semester 5 is accepted")
        void testSemester5InThirdYearAccepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setCurrentYear("3rd Year");
            req.setCurrentSemester("Semester 5");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(userSaved);
        }

        @Test
        @DisplayName("4th Year with Semester 8 is accepted")
        void testSemester8InFourthYearAccepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setCurrentYear("4th Year");
            req.setCurrentSemester("Semester 8");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(userSaved);
        }
    }

    @Nested
    @DisplayName("5. Phone Number Tests")
    class PhoneNumberTests {
        @Test
        @DisplayName("Empty or null phone number -> accepted")
        void testEmptyPhoneAccepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setPhoneNumber("");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(validator.validate(req).isEmpty());
        }

        @Test
        @DisplayName("9876543210 -> accepted")
        void testValid10DigitPhoneAccepted() {
            StudentRegisterRequest req = createValidRequest();
            req.setPhoneNumber("9876543210");

            assertDoesNotThrow(() -> authService.registerStudent(req));
            assertTrue(validator.validate(req).isEmpty());
        }

        @Test
        @DisplayName("123 -> rejected")
        void testShortPhoneRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setPhoneNumber("123");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Phone number must contain exactly 10 digits.", ex.getMessage());

            Set<ConstraintViolation<StudentRegisterRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("123456789012345 -> rejected")
        void testLongPhoneRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setPhoneNumber("123456789012345");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Phone number must contain exactly 10 digits.", ex.getMessage());

            Set<ConstraintViolation<StudentRegisterRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("98765abc10 -> rejected")
        void testAlphaPhoneRejected() {
            StudentRegisterRequest req = createValidRequest();
            req.setPhoneNumber("98765abc10");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Phone number must contain exactly 10 digits.", ex.getMessage());

            Set<ConstraintViolation<StudentRegisterRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("6. Duplicate Detection Tests")
    class DuplicateTests {
        @Test
        @DisplayName("Existing email -> rejected with friendly message")
        void testDuplicateEmailRejected() {
            StudentRegisterRequest req = createValidRequest();
            emailExists = true;

            ConflictException ex = assertThrows(ConflictException.class, () -> authService.registerStudent(req));
            assertEquals("This email is already registered.", ex.getMessage());
            assertFalse(userSaved);
            assertFalse(studentSaved);
        }

        @Test
        @DisplayName("Existing enrollment ID -> rejected with friendly message")
        void testDuplicateEnrollmentIdRejected() {
            StudentRegisterRequest req = createValidRequest();
            enrollmentExists = true;

            ConflictException ex = assertThrows(ConflictException.class, () -> authService.registerStudent(req));
            assertEquals("This enrollment ID is already registered.", ex.getMessage());
            assertFalse(userSaved);
            assertFalse(studentSaved);
        }
    }

    @Nested
    @DisplayName("7. Direct API / Missing Field Tests")
    class DirectApiBypassTests {
        @Test
        @DisplayName("Missing full name rejected")
        void testMissingFullName() {
            StudentRegisterRequest req = createValidRequest();
            req.setFullName("");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Full Name is required", ex.getMessage());
        }

        @Test
        @DisplayName("Missing academic session rejected")
        void testMissingAcademicSession() {
            StudentRegisterRequest req = createValidRequest();
            req.setAcademicYear("");

            BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerStudent(req));
            assertEquals("Academic session must be in format YYYY-YYYY.", ex.getMessage());
        }
    }
}
