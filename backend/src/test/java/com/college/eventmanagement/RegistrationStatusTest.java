package com.college.eventmanagement;

import com.college.eventmanagement.dto.EventResponse;
import com.college.eventmanagement.dto.RegistrationResponse;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.repository.AttendanceRepository;
import com.college.eventmanagement.repository.EventRepository;
import com.college.eventmanagement.repository.NotificationRepository;
import com.college.eventmanagement.repository.RegistrationRepository;
import com.college.eventmanagement.repository.StudentRepository;
import com.college.eventmanagement.repository.UserRepository;
import com.college.eventmanagement.service.EventService;
import com.college.eventmanagement.service.NotificationService;
import com.college.eventmanagement.service.RegistrationService;
import com.college.eventmanagement.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RegistrationStatusTest {

    private EventService eventService;
    private RegistrationService registrationService;

    private User testUser;
    private Student testStudent;
    private Venue testVenue;
    private Organizer testOrganizer;

    private Event currentEvent;
    private boolean registrationSaved = false;

    @BeforeEach
    void setUp() {
        registrationSaved = false;

        testUser = new User();
        testUser.setId(101L);
        testUser.setEmail("priya@college.edu");
        testUser.setRole(Role.ROLE_STUDENT);

        testStudent = new Student();
        testStudent.setId(50L);
        testStudent.setUser(testUser);
        testStudent.setFullName("Priya Sharma");
        testStudent.setBranch("CSE");
        testStudent.setCurrentYear("3rd Year");
        testStudent.setSection("A");
        testStudent.setApprovalStatus(ApprovalStatus.APPROVED);

        testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Seminar Hall B");
        testVenue.setBuilding("Academic Block 2");
        testVenue.setCapacity(100);
        testVenue.setActive(true);

        User orgUser = new User();
        orgUser.setId(201L);
        orgUser.setEmail("gupta@college.edu");
        orgUser.setRole(Role.ROLE_ORGANIZER);

        testOrganizer = new Organizer();
        testOrganizer.setId(1L);
        testOrganizer.setUser(orgUser);
        testOrganizer.setName("Prof. Gupta");
        testOrganizer.setDepartment("Computer Science");
        testOrganizer.setContactEmail("gupta@college.edu");

        // Proxy UserRepository
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String email = (String) args[0];
                    if ("priya@college.edu".equals(email)) return Optional.of(testUser);
                    return Optional.empty();
                }
        );

        // Proxy StudentRepository
        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> {
                    Long userId = (Long) args[0];
                    if (Long.valueOf(101L).equals(userId)) return Optional.of(testStudent);
                    return Optional.empty();
                }
        );

        // Proxy EventRepository
        EventRepository eventRepository = (EventRepository) Proxy.newProxyInstance(
                EventRepository.class.getClassLoader(),
                new Class<?>[]{EventRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) {
                        return Optional.ofNullable(currentEvent);
                    }
                    if ("incrementRegisteredCountIfAvailable".equals(name)) {
                        if (currentEvent != null && currentEvent.getRegisteredCount() < currentEvent.getMaxCapacity()) {
                            currentEvent.setRegisteredCount(currentEvent.getRegisteredCount() + 1);
                            return 1;
                        }
                        return 0;
                    }
                    return null;
                }
        );

        // Proxy RegistrationRepository
        RegistrationRepository registrationRepository = (RegistrationRepository) Proxy.newProxyInstance(
                RegistrationRepository.class.getClassLoader(),
                new Class<?>[]{RegistrationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByEventIdAndStudentId".equals(name)) {
                        return Optional.empty();
                    }
                    if ("save".equals(name)) {
                        registrationSaved = true;
                        Registration reg = (Registration) args[0];
                        reg.setId(999L);
                        return reg;
                    }
                    return null;
                }
        );

        NotificationRepository notificationRepository = (NotificationRepository) Proxy.newProxyInstance(
                NotificationRepository.class.getClassLoader(),
                new Class<?>[]{NotificationRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return null;
                }
        );

        NotificationService notificationService = new NotificationService(notificationRepository, userRepository);

        VenueService venueService = new VenueService(null);

        eventService = new EventService(
                eventRepository,
                null,
                null,
                userRepository,
                studentRepository,
                registrationRepository,
                null,
                notificationService,
                venueService
        );

        AttendanceRepository attendanceRepository = (AttendanceRepository) Proxy.newProxyInstance(
                AttendanceRepository.class.getClassLoader(),
                new Class<?>[]{AttendanceRepository.class},
                (proxy, method, args) -> Optional.empty()
        );

        registrationService = new RegistrationService(
                registrationRepository,
                eventRepository,
                studentRepository,
                userRepository,
                attendanceRepository,
                eventService,
                notificationService
        );
    }

    private Event buildEvent(LocalDate regStart, LocalDate regEnd) {
        LocalDate eventDate = regEnd.plusDays(2);
        Event e = new Event();
        e.setId(10L);
        e.setTitle("Cloud Computing Workshop");
        e.setDescription("Hands-on AWS and Google Cloud lab");
        e.setCategory("Technical");
        e.setVenue(testVenue);
        e.setOrganizer(testOrganizer);
        e.setEventDate(eventDate);
        e.setStartTime(LocalTime.of(10, 0));
        e.setEndTime(LocalTime.of(13, 0));
        e.setMaxCapacity(60);
        e.setRegisteredCount(5);
        e.setRegistrationStartDate(regStart);
        e.setRegistrationEndDate(regEnd);
        e.setEligibleBranches("CSE,IT");
        e.setEligibleAcademicYears("3rd Year");
        e.setStatus(EventStatus.PUBLISHED);
        return e;
    }

    @Test
    @DisplayName("When current date is BEFORE registrationStartDate: status is NOT_STARTED and direct API registration is rejected")
    void testRegistrationBeforeStartDate() {
        LocalDate today = LocalDate.now();
        currentEvent = buildEvent(today.plusDays(2), today.plusDays(5));

        // 1. Verify DTO mapping reflects NOT_STARTED state
        EventResponse response = eventService.mapToEventResponse(currentEvent, testStudent);
        assertEquals("NOT_STARTED", response.getRegistrationState());
        assertEquals("REGISTRATION NOT STARTED", response.getActionStatus());
        assertFalse(response.isRegistrationOpen());

        // 2. Verify backend registration rejects with clean 400 error
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            registrationService.registerForEvent(currentEvent.getId(), "priya@college.edu");
        });

        assertTrue(ex.getMessage().contains("Registration has not started yet"),
                "Expected message indicating registration hasn't started, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(currentEvent.getRegistrationStartDate().toString()),
                "Expected message to include start date: " + ex.getMessage());
        assertFalse(registrationSaved);
    }

    @Test
    @DisplayName("When current date is WITHIN registration window: status is OPEN and registration succeeds")
    void testRegistrationWithinWindow() {
        LocalDate today = LocalDate.now();
        currentEvent = buildEvent(today.minusDays(1), today.plusDays(2));

        // 1. Verify DTO mapping reflects OPEN state
        EventResponse response = eventService.mapToEventResponse(currentEvent, testStudent);
        assertEquals("OPEN", response.getRegistrationState());
        assertEquals("REGISTER NOW", response.getActionStatus());
        assertTrue(response.isRegistrationOpen());

        // 2. Verify backend registration succeeds
        RegistrationResponse regResponse = registrationService.registerForEvent(currentEvent.getId(), "priya@college.edu");
        assertNotNull(regResponse);
        assertEquals(RegistrationStatus.REGISTERED, regResponse.getStatus());
        assertTrue(registrationSaved);
        assertEquals(6, currentEvent.getRegisteredCount());
    }

    @Test
    @DisplayName("When current date is AFTER registrationEndDate: status is CLOSED and direct API registration is rejected")
    void testRegistrationAfterEndDate() {
        LocalDate today = LocalDate.now();
        currentEvent = buildEvent(today.minusDays(5), today.minusDays(1));

        // 1. Verify DTO mapping reflects CLOSED state
        EventResponse response = eventService.mapToEventResponse(currentEvent, testStudent);
        assertEquals("CLOSED", response.getRegistrationState());
        assertEquals("REGISTRATION CLOSED", response.getActionStatus());
        assertFalse(response.isRegistrationOpen());

        // 2. Verify backend registration rejects with clean 400 error
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            registrationService.registerForEvent(currentEvent.getId(), "priya@college.edu");
        });

        assertTrue(ex.getMessage().contains("Registration is closed"),
                "Expected message indicating registration is closed, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(currentEvent.getRegistrationEndDate().toString()),
                "Expected message to include end date: " + ex.getMessage());
        assertFalse(registrationSaved);
    }
}
