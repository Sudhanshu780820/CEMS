package com.college.eventmanagement;

import com.college.eventmanagement.dto.*;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.repository.*;
import com.college.eventmanagement.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Authoritative Backend Verification for CEMS:
 * Covers all 38 test specifications required in Section 20:
 * 1-9: Attendance Window & QR Check-in
 * 10-13: Organizer Manual Attendance
 * 14-17: Admin Manual Attendance
 * 18-20: Registration Window
 * 21-24: Recommendations
 * 25-29: Event Visibility
 * 30-36: Venue Conflict
 * 37-38: Duplicate Attendance
 */
public class AttendanceWindowTest {

    private AttendanceService attendanceService;
    private EventService eventService;
    private RegistrationService registrationService;
    private VenueService venueService;
    private NotificationService notificationService;

    // Test Data Stores
    private List<Event> repoEvents;
    private List<Registration> repoRegistrations;
    private List<Attendance> repoAttendances;
    private List<Notification> repoNotifications;

    private User studentUser;
    private Student student;
    private User organizerUser;
    private Organizer organizer;
    private User adminUser;
    private Venue venue1;
    private Venue venue2;

    private Event standardEvent;
    private Registration standardRegistration;

    @BeforeEach
    void setUp() {
        repoEvents = new ArrayList<>();
        repoRegistrations = new ArrayList<>();
        repoAttendances = new ArrayList<>();
        repoNotifications = new ArrayList<>();

        // 1. Roles & Users
        studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("student@college.edu");
        studentUser.setRole(Role.ROLE_STUDENT);

        student = new Student();
        student.setId(100L);
        student.setUser(studentUser);
        student.setFullName("Aarav Patel");
        student.setEnrollmentId("ENR2026001");
        student.setBranch("CSE");
        student.setCurrentYear("3rd Year");
        student.setSection("A");
        student.setApprovalStatus(ApprovalStatus.APPROVED);

        organizerUser = new User();
        organizerUser.setId(20L);
        organizerUser.setEmail("organizer@college.edu");
        organizerUser.setRole(Role.ROLE_ORGANIZER);

        organizer = new Organizer();
        organizer.setId(200L);
        organizer.setUser(organizerUser);
        organizer.setName("Prof. Mehta");
        organizer.setDepartment("Computer Science");
        organizer.setContactEmail("organizer@college.edu");

        adminUser = new User();
        adminUser.setId(30L);
        adminUser.setEmail("admin@college.edu");
        adminUser.setRole(Role.ROLE_ADMIN);

        venue1 = new Venue();
        venue1.setId(1L);
        venue1.setName("Auditorium Hall 1");
        venue1.setBuilding("Academic Complex");
        venue1.setCapacity(250);
        venue1.setActive(true);

        venue2 = new Venue();
        venue2.setId(2L);
        venue2.setName("Lab 402");
        venue2.setBuilding("IT Block");
        venue2.setCapacity(60);
        venue2.setActive(true);

        // Standard event on 2026-10-10, 10:00 to 12:00
        standardEvent = new Event();
        standardEvent.setId(500L);
        standardEvent.setTitle("AI & Cloud Symposium");
        standardEvent.setDescription("Annual Tech Conference");
        standardEvent.setCategory("Technical");
        standardEvent.setOrganizer(organizer);
        standardEvent.setVenue(venue1);
        standardEvent.setEventDate(LocalDate.of(2026, 10, 10));
        standardEvent.setStartTime(LocalTime.of(10, 0));
        standardEvent.setEndTime(LocalTime.of(12, 0));
        standardEvent.setMaxCapacity(100);
        standardEvent.setRegisteredCount(1);
        standardEvent.setRegistrationStartDate(LocalDate.of(2026, 10, 1));
        standardEvent.setRegistrationEndDate(LocalDate.of(2026, 10, 9));
        standardEvent.setEligibleBranches("CSE,IT");
        standardEvent.setEligibleAcademicYears("3rd Year,4th Year");
        standardEvent.setStatus(EventStatus.PUBLISHED);
        standardEvent.setAttendanceActive(true);
        standardEvent.setAttendanceToken("VALID-QR-TOKEN-123");

        repoEvents.add(standardEvent);

        standardRegistration = new Registration();
        standardRegistration.setId(1000L);
        standardRegistration.setEvent(standardEvent);
        standardRegistration.setStudent(student);
        standardRegistration.setStatus(RegistrationStatus.REGISTERED);
        standardRegistration.setRegisteredAt(LocalDateTime.of(2026, 10, 2, 10, 0));

        repoRegistrations.add(standardRegistration);

        // Proxies
        EventRepository eventRepository = (EventRepository) Proxy.newProxyInstance(
                EventRepository.class.getClassLoader(),
                new Class<?>[]{EventRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return repoEvents.stream().filter(e -> e.getId().equals(id)).findFirst();
                    }
                    if ("save".equals(name)) {
                        Event e = (Event) args[0];
                        if (e.getId() == null) e.setId((long) (repoEvents.size() + 100));
                        repoEvents.removeIf(existing -> existing.getId().equals(e.getId()));
                        repoEvents.add(e);
                        return e;
                    }
                    if ("searchUpcomingPublishedEvents".equals(name) || "findUpcomingPublishedEvents".equals(name)) {
                        LocalDate today = (LocalDate) args[0];
                        LocalTime now = (LocalTime) args[1];
                        List<Event> matches = new ArrayList<>();
                        for (Event e : repoEvents) {
                            if (e.getStatus() == EventStatus.PUBLISHED) {
                                boolean isFuture = e.getEventDate().isAfter(today);
                                boolean isTodayUpcoming = e.getEventDate().isEqual(today) && e.getEndTime().isAfter(now);
                                if (isFuture || isTodayUpcoming) {
                                    matches.add(e);
                                }
                            }
                        }
                        return matches;
                    }
                    if ("searchEvents".equals(name)) {
                        return new ArrayList<>(repoEvents);
                    }
                    if ("findByVenueIdAndEventDate".equals(name)) {
                        Long vId = (Long) args[0];
                        LocalDate date = (LocalDate) args[1];
                        List<Event> matches = new ArrayList<>();
                        for (Event e : repoEvents) {
                            if (e.getVenue().getId().equals(vId) && e.getEventDate().isEqual(date)) {
                                matches.add(e);
                            }
                        }
                        return matches;
                    }
                    if ("findConflictingEvents".equals(name)) {
                        Long vId = (Long) args[0];
                        LocalDate eDate = (LocalDate) args[1];
                        LocalTime sTime = (LocalTime) args[2];
                        LocalTime eTime = (LocalTime) args[3];
                        Long excludeId = (Long) args[4];

                        List<Event> conflicts = new ArrayList<>();
                        for (Event e : repoEvents) {
                            if (excludeId != null && e.getId() != null && e.getId().equals(excludeId)) {
                                continue;
                            }
                            boolean isBookingActive = e.getStatus() == EventStatus.PUBLISHED
                                    || e.getStatus() == EventStatus.REGISTRATION_OPEN
                                    || e.getStatus() == EventStatus.REGISTRATION_CLOSED
                                    || e.getStatus() == EventStatus.ONGOING
                                    || e.getStatus() == EventStatus.PENDING_APPROVAL;
                            if (!isBookingActive) continue;

                            if (e.getVenue() != null && e.getVenue().getId().equals(vId) && e.getEventDate().isEqual(eDate)) {
                                if (e.getStartTime().isBefore(eTime) && e.getEndTime().isAfter(sTime)) {
                                    conflicts.add(e);
                                }
                            }
                        }
                        return conflicts;
                    }
                    if ("incrementRegisteredCountIfAvailable".equals(name)) {
                        Long eId = (Long) args[0];
                        Event e = repoEvents.stream().filter(ev -> ev.getId().equals(eId)).findFirst().orElse(null);
                        if (e != null && e.getRegisteredCount() < e.getMaxCapacity()) {
                            e.setRegisteredCount(e.getRegisteredCount() + 1);
                            return 1;
                        }
                        return 0;
                    }
                    return null;
                }
        );

        RegistrationRepository registrationRepository = (RegistrationRepository) Proxy.newProxyInstance(
                RegistrationRepository.class.getClassLoader(),
                new Class<?>[]{RegistrationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByEventIdAndStudentId".equals(name)) {
                        Long eId = (Long) args[0];
                        Long sId = (Long) args[1];
                        return repoRegistrations.stream()
                                .filter(r -> r.getEvent().getId().equals(eId) && r.getStudent().getId().equals(sId))
                                .findFirst();
                    }
                    if ("findByStudentId".equals(name)) {
                        Long sId = (Long) args[0];
                        List<Registration> matches = new ArrayList<>();
                        for (Registration r : repoRegistrations) {
                            if (r.getStudent().getId().equals(sId)) {
                                matches.add(r);
                            }
                        }
                        return matches;
                    }
                    if ("findByStudentOrderByRegisteredAtDesc".equals(name)) {
                        Student s = (Student) args[0];
                        List<Registration> matches = new ArrayList<>();
                        for (Registration r : repoRegistrations) {
                            if (r.getStudent().getId().equals(s.getId())) {
                                matches.add(r);
                            }
                        }
                        return matches;
                    }
                    if ("save".equals(name)) {
                        Registration r = (Registration) args[0];
                        if (r.getId() == null) r.setId((long) (repoRegistrations.size() + 500));
                        repoRegistrations.removeIf(existing -> existing.getId().equals(r.getId()));
                        repoRegistrations.add(r);
                        return r;
                    }
                    if ("countByEventId".equals(name)) {
                        Long eId = (Long) args[0];
                        return repoRegistrations.stream().filter(r -> r.getEvent().getId().equals(eId)).count();
                    }
                    return null;
                }
        );

        AttendanceRepository attendanceRepository = (AttendanceRepository) Proxy.newProxyInstance(
                AttendanceRepository.class.getClassLoader(),
                new Class<?>[]{AttendanceRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByEventIdAndStudentId".equals(name)) {
                        Long eId = (Long) args[0];
                        Long sId = (Long) args[1];
                        return repoAttendances.stream()
                                .filter(a -> a.getEvent().getId().equals(eId) && a.getStudent().getId().equals(sId))
                                .findFirst();
                    }
                    if ("save".equals(name)) {
                        Attendance a = (Attendance) args[0];
                        if (a.getId() == null) a.setId((long) (repoAttendances.size() + 200));
                        repoAttendances.removeIf(existing -> existing.getId().equals(a.getId()));
                        repoAttendances.add(a);
                        return a;
                    }
                    if ("countByEventIdAndStatus".equals(name)) {
                        Long eId = (Long) args[0];
                        AttendanceStatus st = (AttendanceStatus) args[1];
                        return repoAttendances.stream()
                                .filter(a -> a.getEvent().getId().equals(eId) && a.getStatus() == st)
                                .count();
                    }
                    return null;
                }
        );

        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByEmail".equals(name)) {
                        String email = (String) args[0];
                        if ("student@college.edu".equals(email)) return Optional.of(studentUser);
                        if ("organizer@college.edu".equals(email)) return Optional.of(organizerUser);
                        if ("admin@college.edu".equals(email)) return Optional.of(adminUser);
                        return Optional.empty();
                    }
                    return null;
                }
        );

        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByUserId".equals(name)) {
                        Long uId = (Long) args[0];
                        if (studentUser.getId().equals(uId)) return Optional.of(student);
                        return Optional.empty();
                    }
                    if ("findById".equals(name)) {
                        Long sId = (Long) args[0];
                        if (student.getId().equals(sId)) return Optional.of(student);
                        return Optional.empty();
                    }
                    return null;
                }
        );

        VenueRepository venueRepository = (VenueRepository) Proxy.newProxyInstance(
                VenueRepository.class.getClassLoader(),
                new Class<?>[]{VenueRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) {
                        Long vId = (Long) args[0];
                        if (venue1.getId().equals(vId)) return Optional.of(venue1);
                        if (venue2.getId().equals(vId)) return Optional.of(venue2);
                        return Optional.empty();
                    }
                    return null;
                }
        );

        OrganizerRepository organizerRepository = (OrganizerRepository) Proxy.newProxyInstance(
                OrganizerRepository.class.getClassLoader(),
                new Class<?>[]{OrganizerRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByUserId".equals(name)) {
                        Long uId = (Long) args[0];
                        if (organizerUser.getId().equals(uId)) return Optional.of(organizer);
                        return Optional.empty();
                    }
                    if ("findAll".equals(name)) {
                        return List.of(organizer);
                    }
                    return null;
                }
        );

        NotificationRepository notificationRepository = (NotificationRepository) Proxy.newProxyInstance(
                NotificationRepository.class.getClassLoader(),
                new Class<?>[]{NotificationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Notification n = (Notification) args[0];
                        repoNotifications.add(n);
                        return n;
                    }
                    return null;
                }
        );

        notificationService = new NotificationService(notificationRepository, userRepository);
        venueService = new VenueService(venueRepository);

        eventService = new EventService(
                eventRepository,
                venueRepository,
                organizerRepository,
                userRepository,
                studentRepository,
                registrationRepository,
                attendanceRepository,
                notificationService,
                venueService
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

        attendanceService = new AttendanceService(
                attendanceRepository,
                eventRepository,
                registrationRepository,
                studentRepository,
                userRepository,
                notificationService
        );
    }

    // =========================================================================
    // Category 1: Attendance Window & QR Check-in (Tests 1–9)
    // =========================================================================

    @Test
    @DisplayName("1. Student checks in before event start -> Rejected with HTTP 400")
    void test01_studentChecksInBeforeEventStart_Rejected() {
        // Event start is 2026-10-10 10:00:00
        LocalDateTime beforeStart = LocalDateTime.of(2026, 10, 10, 9, 59, 59);
        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", beforeStart)
        );

        assertTrue(ex.getMessage().contains("Check-in has not started yet"),
                "Expected 'Check-in has not started yet' message, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Check-in opens at"));
        assertTrue(ex.getMessage().toLowerCase().contains("10 oct 2026"));
    }

    @Test
    @DisplayName("2. Student checks in exactly at event start -> Allowed (PRESENT)")
    void test02_studentChecksInExactlyAtEventStart_Allowed() {
        LocalDateTime exactStart = LocalDateTime.of(2026, 10, 10, 10, 0, 0);
        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");

        AttendanceResponse res = attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", exactStart);

        assertNotNull(res);
        assertEquals(AttendanceStatus.PRESENT, res.getStatus());
        assertEquals(exactStart, res.getCheckInTime());
    }

    @Test
    @DisplayName("3. Student checks in during event -> Allowed (PRESENT)")
    void test03_studentChecksInDuringEvent_Allowed() {
        LocalDateTime duringEvent = LocalDateTime.of(2026, 10, 10, 11, 0, 0);
        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");

        AttendanceResponse res = attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", duringEvent);

        assertNotNull(res);
        assertEquals(AttendanceStatus.PRESENT, res.getStatus());
    }

    @Test
    @DisplayName("4. Student checks in exactly at event end -> Allowed (PRESENT)")
    void test04_studentChecksInExactlyAtEventEnd_Allowed() {
        LocalDateTime exactEnd = LocalDateTime.of(2026, 10, 10, 12, 0, 0);
        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");

        AttendanceResponse res = attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", exactEnd);

        assertNotNull(res);
        assertEquals(AttendanceStatus.PRESENT, res.getStatus());
    }

    @Test
    @DisplayName("5. Student checks in 1 hour after event end (within 24-hr grace) -> Allowed (PRESENT)")
    void test05_studentChecksInWithin24HourGracePeriod_Allowed() {
        LocalDateTime oneHourAfterEnd = LocalDateTime.of(2026, 10, 10, 13, 0, 0);
        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");

        AttendanceResponse res = attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", oneHourAfterEnd);

        assertNotNull(res);
        assertEquals(AttendanceStatus.PRESENT, res.getStatus());
    }

    @Test
    @DisplayName("6. Student checks in at exactly end + 24 hours -> Allowed (PRESENT)")
    void test06_studentChecksInExactlyAtGracePeriodBoundary_Allowed() {
        // End is 2026-10-10 12:00:00 -> grace period end is 2026-10-11 12:00:00
        LocalDateTime graceBoundary = LocalDateTime.of(2026, 10, 11, 12, 0, 0);
        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");

        AttendanceResponse res = attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", graceBoundary);

        assertNotNull(res);
        assertEquals(AttendanceStatus.PRESENT, res.getStatus());
    }

    @Test
    @DisplayName("7. Student checks in 1 second after grace period -> Rejected with HTTP 400")
    void test07_studentChecksInAfterGracePeriod_Rejected() {
        LocalDateTime afterGrace = LocalDateTime.of(2026, 10, 11, 12, 0, 1);
        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", afterGrace)
        );

        assertTrue(ex.getMessage().contains("Check-in is closed. The 24-hour check-in window has ended."));
    }

    @Test
    @DisplayName("8. Student attempts second check-in after already marked PRESENT -> Rejected with HTTP 409")
    void test08_studentAttemptsSecondCheckInAfterAlreadyPresent_Rejected() {
        Attendance existing = new Attendance(standardEvent, student, AttendanceMethod.QR_SCAN, null, AttendanceStatus.PRESENT);
        existing.setId(999L);
        existing.setCheckInTime(LocalDateTime.of(2026, 10, 10, 10, 15, 0));
        repoAttendances.add(existing);

        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");
        LocalDateTime during = LocalDateTime.of(2026, 10, 10, 11, 0, 0);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", during)
        );

        assertTrue(ex.getMessage().contains("Attendance has already been marked PRESENT."));
    }

    @Test
    @DisplayName("9. Unregistered student attempts check-in -> Rejected with HTTP 400")
    void test09_unregisteredStudentAttemptsCheckIn_Rejected() {
        repoRegistrations.clear(); // No registration for student
        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");
        LocalDateTime during = LocalDateTime.of(2026, 10, 10, 11, 0, 0);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", during)
        );

        assertTrue(ex.getMessage().contains("You are not registered for this event"));
    }

    // =========================================================================
    // Category 2: Organizer Manual Attendance (Tests 10–13)
    // =========================================================================

    @Test
    @DisplayName("10. Organizer attempts manual attendance before event start -> Rejected with HTTP 400")
    void test10_organizerMarksManualAttendanceBeforeEventStart_Rejected() {
        LocalDateTime beforeStart = LocalDateTime.of(2026, 10, 10, 9, 30, 0);
        ManualAttendanceRequest req = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markManualAttendance(standardEvent.getId(), req, "organizer@college.edu", beforeStart)
        );

        assertTrue(ex.getMessage().contains("Check-in has not started yet"));
    }

    @Test
    @DisplayName("11. Organizer marks manual attendance during event -> Allowed")
    void test11_organizerMarksManualAttendanceDuringEvent_Allowed() {
        LocalDateTime during = LocalDateTime.of(2026, 10, 10, 11, 0, 0);
        ManualAttendanceRequest req = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        assertDoesNotThrow(() ->
                attendanceService.markManualAttendance(standardEvent.getId(), req, "organizer@college.edu", during)
        );

        assertEquals(1, repoAttendances.size());
        assertEquals(AttendanceStatus.PRESENT, repoAttendances.get(0).getStatus());
    }

    @Test
    @DisplayName("12. Organizer marks manual attendance within 24-hour grace period -> Allowed")
    void test12_organizerMarksManualAttendanceWithinGracePeriod_Allowed() {
        LocalDateTime withinGrace = LocalDateTime.of(2026, 10, 10, 18, 0, 0);
        ManualAttendanceRequest req = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        assertDoesNotThrow(() ->
                attendanceService.markManualAttendance(standardEvent.getId(), req, "organizer@college.edu", withinGrace)
        );

        assertEquals(AttendanceStatus.PRESENT, repoAttendances.get(0).getStatus());
    }

    @Test
    @DisplayName("13. Organizer attempts manual attendance after 24-hour grace period -> Rejected with HTTP 400")
    void test13_organizerMarksManualAttendanceAfterGracePeriod_Rejected() {
        LocalDateTime afterGrace = LocalDateTime.of(2026, 10, 11, 12, 1, 0);
        ManualAttendanceRequest req = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markManualAttendance(standardEvent.getId(), req, "organizer@college.edu", afterGrace)
        );

        assertTrue(ex.getMessage().contains("Check-in is closed. The 24-hour check-in window has ended."));
    }

    // =========================================================================
    // Category 3: Admin Manual Attendance (Tests 14–17)
    // =========================================================================

    @Test
    @DisplayName("14. Admin attempts manual attendance before event start -> Rejected with HTTP 400")
    void test14_adminMarksManualAttendanceBeforeEventStart_Rejected() {
        LocalDateTime beforeStart = LocalDateTime.of(2026, 10, 10, 8, 0, 0);
        ManualAttendanceRequest req = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markManualAttendance(standardEvent.getId(), req, "admin@college.edu", beforeStart)
        );

        assertTrue(ex.getMessage().contains("Check-in has not started yet"));
    }

    @Test
    @DisplayName("15. Admin marks manual attendance during event -> Allowed")
    void test15_adminMarksManualAttendanceDuringEvent_Allowed() {
        LocalDateTime during = LocalDateTime.of(2026, 10, 10, 10, 30, 0);
        ManualAttendanceRequest req = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        assertDoesNotThrow(() ->
                attendanceService.markManualAttendance(standardEvent.getId(), req, "admin@college.edu", during)
        );

        assertEquals(AttendanceStatus.PRESENT, repoAttendances.get(0).getStatus());
    }

    @Test
    @DisplayName("16. Admin marks manual attendance within 24-hour grace period -> Allowed")
    void test16_adminMarksManualAttendanceWithinGracePeriod_Allowed() {
        LocalDateTime withinGrace = LocalDateTime.of(2026, 10, 11, 11, 0, 0);
        ManualAttendanceRequest req = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        assertDoesNotThrow(() ->
                attendanceService.markManualAttendance(standardEvent.getId(), req, "admin@college.edu", withinGrace)
        );

        assertEquals(AttendanceStatus.PRESENT, repoAttendances.get(0).getStatus());
    }

    @Test
    @DisplayName("17. Admin attempts manual attendance after 24-hour grace period -> Rejected with HTTP 400")
    void test17_adminMarksManualAttendanceAfterGracePeriod_Rejected() {
        LocalDateTime afterGrace = LocalDateTime.of(2026, 10, 11, 13, 0, 0);
        ManualAttendanceRequest req = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markManualAttendance(standardEvent.getId(), req, "admin@college.edu", afterGrace)
        );

        assertTrue(ex.getMessage().contains("Check-in is closed. The 24-hour check-in window has ended."));
    }

    // =========================================================================
    // Category 4: Registration Window (Tests 18–20)
    // =========================================================================

    @Test
    @DisplayName("18. Student attempts registration before registration start -> Rejected with HTTP 400")
    void test18_studentRegistersBeforeRegistrationStart_Rejected() {
        repoRegistrations.clear();
        LocalDate today = LocalDate.now();
        Event futureRegEvent = new Event();
        futureRegEvent.setId(601L);
        futureRegEvent.setTitle("Future Reg Event");
        futureRegEvent.setVenue(venue1);
        futureRegEvent.setOrganizer(organizer);
        futureRegEvent.setEventDate(today.plusDays(10));
        futureRegEvent.setStartTime(LocalTime.of(10, 0));
        futureRegEvent.setEndTime(LocalTime.of(12, 0));
        futureRegEvent.setMaxCapacity(50);
        futureRegEvent.setRegisteredCount(0);
        futureRegEvent.setRegistrationStartDate(today.plusDays(2)); // Registration in future
        futureRegEvent.setRegistrationEndDate(today.plusDays(8));
        futureRegEvent.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(futureRegEvent);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(futureRegEvent.getId(), "student@college.edu")
        );

        assertTrue(ex.getMessage().contains("Registration has not started yet"));
    }

    @Test
    @DisplayName("19. Student attempts registration during registration window -> Allowed")
    void test19_studentRegistersDuringRegistrationWindow_Allowed() {
        repoRegistrations.clear();
        LocalDate today = LocalDate.now();
        Event openRegEvent = new Event();
        openRegEvent.setId(602L);
        openRegEvent.setTitle("Open Reg Event");
        openRegEvent.setVenue(venue1);
        openRegEvent.setOrganizer(organizer);
        openRegEvent.setEventDate(today.plusDays(5));
        openRegEvent.setStartTime(LocalTime.of(10, 0));
        openRegEvent.setEndTime(LocalTime.of(12, 0));
        openRegEvent.setMaxCapacity(50);
        openRegEvent.setRegisteredCount(0);
        openRegEvent.setRegistrationStartDate(today.minusDays(1)); // Open
        openRegEvent.setRegistrationEndDate(today.plusDays(3));
        openRegEvent.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(openRegEvent);

        RegistrationResponse res = registrationService.registerForEvent(openRegEvent.getId(), "student@college.edu");

        assertNotNull(res);
        assertEquals(RegistrationStatus.REGISTERED, res.getStatus());
        assertEquals(1, openRegEvent.getRegisteredCount());
    }

    @Test
    @DisplayName("20. Student attempts registration after registration end -> Rejected with HTTP 400")
    void test20_studentRegistersAfterRegistrationEnd_Rejected() {
        repoRegistrations.clear();
        LocalDate today = LocalDate.now();
        Event closedRegEvent = new Event();
        closedRegEvent.setId(603L);
        closedRegEvent.setTitle("Closed Reg Event");
        closedRegEvent.setVenue(venue1);
        closedRegEvent.setOrganizer(organizer);
        closedRegEvent.setEventDate(today.plusDays(5));
        closedRegEvent.setStartTime(LocalTime.of(10, 0));
        closedRegEvent.setEndTime(LocalTime.of(12, 0));
        closedRegEvent.setMaxCapacity(50);
        closedRegEvent.setRegisteredCount(0);
        closedRegEvent.setRegistrationStartDate(today.minusDays(5));
        closedRegEvent.setRegistrationEndDate(today.minusDays(1)); // Ended
        closedRegEvent.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(closedRegEvent);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(closedRegEvent.getId(), "student@college.edu")
        );

        assertTrue(ex.getMessage().contains("Registration is closed"));
    }

    // =========================================================================
    // Category 5: Recommendations (Tests 21–24)
    // =========================================================================

    @Test
    @DisplayName("21. Recommendations exclude events student is already registered for")
    void test21_recommendationsExcludeRegisteredEvents() {
        // standardEvent is registered in repoRegistrations
        LocalDate today = LocalDate.now();
        standardEvent.setEventDate(today.plusDays(5));

        // Add a second event that is NOT registered
        Event unregisteredEvent = new Event();
        unregisteredEvent.setId(701L);
        unregisteredEvent.setTitle("Unregistered Hackathon");
        unregisteredEvent.setVenue(venue1);
        unregisteredEvent.setOrganizer(organizer);
        unregisteredEvent.setMaxCapacity(100);
        unregisteredEvent.setRegisteredCount(0);
        unregisteredEvent.setEventDate(today.plusDays(4));
        unregisteredEvent.setStartTime(LocalTime.of(10, 0));
        unregisteredEvent.setEndTime(LocalTime.of(17, 0));
        unregisteredEvent.setRegistrationStartDate(today.minusDays(2));
        unregisteredEvent.setRegistrationEndDate(today.plusDays(2));
        unregisteredEvent.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(unregisteredEvent);

        List<EventResponse> recommendations = eventService.getRecommendations("student@college.edu");

        boolean containsRegistered = recommendations.stream().anyMatch(e -> e.getId().equals(standardEvent.getId()));
        boolean containsUnregistered = recommendations.stream().anyMatch(e -> e.getId().equals(unregisteredEvent.getId()));

        assertFalse(containsRegistered, "Registered events must NOT appear in recommendations");
        assertTrue(containsUnregistered, "Unregistered upcoming events must appear in recommendations");
    }

    @Test
    @DisplayName("22. Recommendations include eligible upcoming events")
    void test22_recommendationsIncludeEligibleUpcomingEvents() {
        repoRegistrations.clear();
        LocalDate today = LocalDate.now();

        Event eligibleEvent = new Event();
        eligibleEvent.setId(702L);
        eligibleEvent.setTitle("CSE Tech Summit");
        eligibleEvent.setVenue(venue1);
        eligibleEvent.setOrganizer(organizer);
        eligibleEvent.setMaxCapacity(100);
        eligibleEvent.setRegisteredCount(0);
        eligibleEvent.setEligibleBranches("CSE");
        eligibleEvent.setEligibleAcademicYears("3rd Year");
        eligibleEvent.setRegistrationStartDate(today.minusDays(2));
        eligibleEvent.setRegistrationEndDate(today.plusDays(2));
        eligibleEvent.setEventDate(today.plusDays(3));
        eligibleEvent.setStartTime(LocalTime.of(10, 0));
        eligibleEvent.setEndTime(LocalTime.of(16, 0));
        eligibleEvent.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(eligibleEvent);

        List<EventResponse> recommendations = eventService.getRecommendations("student@college.edu");

        assertTrue(recommendations.stream().anyMatch(e -> e.getId().equals(eligibleEvent.getId())));
    }

    @Test
    @DisplayName("23. Recommendations exclude completed events")
    void test23_recommendationsExcludeCompletedEvents() {
        repoRegistrations.clear();
        LocalDate today = LocalDate.now();

        Event pastEvent = new Event();
        pastEvent.setId(703L);
        pastEvent.setTitle("Past Conference");
        pastEvent.setVenue(venue1);
        pastEvent.setOrganizer(organizer);
        pastEvent.setMaxCapacity(100);
        pastEvent.setRegisteredCount(0);
        pastEvent.setRegistrationStartDate(today.minusDays(10));
        pastEvent.setRegistrationEndDate(today.minusDays(3));
        pastEvent.setEventDate(today.minusDays(2)); // Ended
        pastEvent.setStartTime(LocalTime.of(10, 0));
        pastEvent.setEndTime(LocalTime.of(12, 0));
        pastEvent.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(pastEvent);

        List<EventResponse> recommendations = eventService.getRecommendations("student@college.edu");

        assertFalse(recommendations.stream().anyMatch(e -> e.getId().equals(pastEvent.getId())),
                "Completed events must be excluded from recommendations");
    }

    @Test
    @DisplayName("24. Recommendations exclude events student is not eligible for (branch/year)")
    void test24_recommendationsExcludeIneligibleEvents() {
        repoRegistrations.clear();
        LocalDate today = LocalDate.now();

        // Student is CSE 3rd Year
        Event meOnlyEvent = new Event();
        meOnlyEvent.setId(704L);
        meOnlyEvent.setTitle("Mechanical CAD Workshop");
        meOnlyEvent.setVenue(venue1);
        meOnlyEvent.setOrganizer(organizer);
        meOnlyEvent.setMaxCapacity(100);
        meOnlyEvent.setRegisteredCount(0);
        meOnlyEvent.setEligibleBranches("ME,Civil");
        meOnlyEvent.setEligibleAcademicYears("1st Year");
        meOnlyEvent.setRegistrationStartDate(today.minusDays(2));
        meOnlyEvent.setRegistrationEndDate(today.plusDays(2));
        meOnlyEvent.setEventDate(today.plusDays(4));
        meOnlyEvent.setStartTime(LocalTime.of(10, 0));
        meOnlyEvent.setEndTime(LocalTime.of(12, 0));
        meOnlyEvent.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(meOnlyEvent);

        List<EventResponse> recommendations = eventService.getRecommendations("student@college.edu");

        assertFalse(recommendations.stream().anyMatch(e -> e.getId().equals(meOnlyEvent.getId())),
                "Ineligible events must be excluded from recommendations");
    }

    // =========================================================================
    // Category 6: Event Visibility (Tests 25–29)
    // =========================================================================

    @Test
    @DisplayName("25. Student event discovery excludes PENDING_APPROVAL events")
    void test25_studentDiscoveryExcludesPendingApprovalEvents() {
        LocalDate today = LocalDate.now();
        Event pending = new Event();
        pending.setId(801L);
        pending.setTitle("Pending Review Fest");
        pending.setVenue(venue1);
        pending.setOrganizer(organizer);
        pending.setEventDate(today.plusDays(5));
        pending.setStartTime(LocalTime.of(10, 0));
        pending.setEndTime(LocalTime.of(12, 0));
        pending.setStatus(EventStatus.PENDING_APPROVAL);
        repoEvents.add(pending);

        List<EventResponse> discovery = eventService.getEventsForDiscovery("student@college.edu", null, null, null, null);

        assertFalse(discovery.stream().anyMatch(e -> e.getId().equals(pending.getId())),
                "PENDING_APPROVAL events must be hidden from students");
    }

    @Test
    @DisplayName("26. Student event discovery excludes REJECTED events")
    void test26_studentDiscoveryExcludesRejectedEvents() {
        LocalDate today = LocalDate.now();
        Event rejected = new Event();
        rejected.setId(802L);
        rejected.setTitle("Rejected Event");
        rejected.setVenue(venue1);
        rejected.setOrganizer(organizer);
        rejected.setEventDate(today.plusDays(5));
        rejected.setStartTime(LocalTime.of(10, 0));
        rejected.setEndTime(LocalTime.of(12, 0));
        rejected.setStatus(EventStatus.REJECTED);
        repoEvents.add(rejected);

        List<EventResponse> discovery = eventService.getEventsForDiscovery("student@college.edu", null, null, null, null);

        assertFalse(discovery.stream().anyMatch(e -> e.getId().equals(rejected.getId())),
                "REJECTED events must be hidden from students");
    }

    @Test
    @DisplayName("27. Student event discovery excludes CANCELLED events")
    void test27_studentDiscoveryExcludesCancelledEvents() {
        LocalDate today = LocalDate.now();
        Event cancelled = new Event();
        cancelled.setId(803L);
        cancelled.setTitle("Cancelled Fest");
        cancelled.setVenue(venue1);
        cancelled.setOrganizer(organizer);
        cancelled.setEventDate(today.plusDays(5));
        cancelled.setStartTime(LocalTime.of(10, 0));
        cancelled.setEndTime(LocalTime.of(12, 0));
        cancelled.setStatus(EventStatus.CANCELLED);
        repoEvents.add(cancelled);

        List<EventResponse> discovery = eventService.getEventsForDiscovery("student@college.edu", null, null, null, null);

        assertFalse(discovery.stream().anyMatch(e -> e.getId().equals(cancelled.getId())),
                "CANCELLED events must be hidden from students");
    }

    @Test
    @DisplayName("28. Student event discovery excludes COMPLETED events (based on date/time)")
    void test28_studentDiscoveryExcludesCompletedEvents() {
        LocalDate today = LocalDate.now();
        Event completed = new Event();
        completed.setId(804L);
        completed.setTitle("Past Summit");
        completed.setVenue(venue1);
        completed.setOrganizer(organizer);
        completed.setEventDate(today.minusDays(3));
        completed.setStartTime(LocalTime.of(10, 0));
        completed.setEndTime(LocalTime.of(12, 0));
        completed.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(completed);

        List<EventResponse> discovery = eventService.getEventsForDiscovery("student@college.edu", null, null, null, null);

        assertFalse(discovery.stream().anyMatch(e -> e.getId().equals(completed.getId())),
                "Events whose date/time has passed must not be visible to students in discovery");
    }

    @Test
    @DisplayName("29. Student event discovery includes APPROVED/PUBLISHED upcoming events")
    void test29_studentDiscoveryIncludesApprovedUpcomingEvents() {
        LocalDate today = LocalDate.now();
        standardEvent.setEventDate(today.plusDays(2));
        standardEvent.setStatus(EventStatus.PUBLISHED);

        List<EventResponse> discovery = eventService.getEventsForDiscovery("student@college.edu", null, null, null, null);

        assertTrue(discovery.stream().anyMatch(e -> e.getId().equals(standardEvent.getId())),
                "Approved upcoming published events must be visible in student discovery");
    }

    // =========================================================================
    // Category 7: Venue Conflict (Tests 30–36)
    // =========================================================================

    @Test
    @DisplayName("30. Venue conflict: Event B overlaps Event A at same venue -> Conflict detected")
    void test30_venueConflict_EventBOverlapsEventA_ConflictDetected() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        Event existing = new Event();
        existing.setId(901L);
        existing.setTitle("Existing Event 10-12");
        existing.setOrganizer(organizer);
        existing.setVenue(venue1);
        existing.setEventDate(date);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(existing);

        // Requested: 11:00 to 13:00 (overlaps 11:00-12:00)
        Map<String, Object> conflict = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(11, 0), LocalTime.of(13, 0), null);
        assertTrue((Boolean) conflict.get("hasConflict"));
        assertNotNull(conflict.get("conflictingEvent"));
    }

    @Test
    @DisplayName("31. Venue conflict: Event B starts at same time as Event A at same venue -> Conflict detected")
    void test31_venueConflict_EventBSameTimeAsEventA_ConflictDetected() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        Event existing = new Event();
        existing.setId(902L);
        existing.setTitle("Existing Event 10-12");
        existing.setOrganizer(organizer);
        existing.setVenue(venue1);
        existing.setEventDate(date);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(existing);

        // Requested: 10:00 to 11:30
        Map<String, Object> conflict = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(10, 0), LocalTime.of(11, 30), null);
        assertTrue((Boolean) conflict.get("hasConflict"));
    }

    @Test
    @DisplayName("32. Venue conflict: Event B ends during Event A at same venue -> Conflict detected")
    void test32_venueConflict_EventBEndsDuringEventA_ConflictDetected() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        Event existing = new Event();
        existing.setId(903L);
        existing.setTitle("Existing Event 10-12");
        existing.setOrganizer(organizer);
        existing.setVenue(venue1);
        existing.setEventDate(date);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(existing);

        // Requested: 09:00 to 10:30 (overlaps 10:00-10:30)
        Map<String, Object> conflict = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(9, 0), LocalTime.of(10, 30), null);
        assertTrue((Boolean) conflict.get("hasConflict"));
    }

    @Test
    @DisplayName("33. Venue conflict: Event B starts immediately when Event A ends (back-to-back) -> No conflict")
    void test33_venueConflict_EventBBackToBack_NoConflict() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        Event existing = new Event();
        existing.setId(904L);
        existing.setTitle("Existing Event 10-12");
        existing.setOrganizer(organizer);
        existing.setVenue(venue1);
        existing.setEventDate(date);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(existing);

        // Requested: 12:00 to 14:00 (starts right at 12:00)
        Map<String, Object> conflict = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(12, 0), LocalTime.of(14, 0), null);
        assertFalse((Boolean) conflict.get("hasConflict"), "Back-to-back booking must not conflict");
    }

    @Test
    @DisplayName("34. Venue conflict: Same time, different venues -> No conflict")
    void test34_venueConflict_SameTimeDifferentVenues_NoConflict() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        Event existing = new Event();
        existing.setId(905L);
        existing.setTitle("Event in Venue 1");
        existing.setOrganizer(organizer);
        existing.setVenue(venue1);
        existing.setEventDate(date);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(existing);

        // Requested in Venue 2 at exact same time
        Map<String, Object> conflict = eventService.checkVenueConflict(venue2.getId(), date, LocalTime.of(10, 0), LocalTime.of(12, 0), null);
        assertFalse((Boolean) conflict.get("hasConflict"), "Different venues must not conflict");
    }

    @Test
    @DisplayName("35. Venue conflict: Editing Event A with same time/venue as itself -> No conflict (self-exclusion)")
    void test35_venueConflict_EditingSelfSameTimeVenue_NoConflict() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        Event existing = new Event();
        existing.setId(906L);
        existing.setTitle("Event Self Edit");
        existing.setOrganizer(organizer);
        existing.setVenue(venue1);
        existing.setEventDate(date);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setStatus(EventStatus.PUBLISHED);
        repoEvents.add(existing);

        // Exclude ID 906
        Map<String, Object> conflict = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(10, 0), LocalTime.of(12, 0), 906L);
        assertFalse((Boolean) conflict.get("hasConflict"), "Self-exclusion must prevent self conflict");
    }

    @Test
    @DisplayName("36. Venue conflict: Event A is PENDING_APPROVAL, Event B cannot take the venue at the same time")
    void test36_venueConflict_PendingApprovalEventOccupiesVenue_ConflictDetected() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        Event pending = new Event();
        pending.setId(907L);
        pending.setTitle("Pending Approval Event");
        pending.setOrganizer(organizer);
        pending.setVenue(venue1);
        pending.setEventDate(date);
        pending.setStartTime(LocalTime.of(10, 0));
        pending.setEndTime(LocalTime.of(12, 0));
        pending.setStatus(EventStatus.PENDING_APPROVAL);
        repoEvents.add(pending);

        Map<String, Object> conflict = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(10, 30), LocalTime.of(11, 30), null);
        assertTrue((Boolean) conflict.get("hasConflict"), "PENDING_APPROVAL events must hold the venue slot");
    }

    // =========================================================================
    // Category 8: Duplicate Attendance (Tests 37–38)
    // =========================================================================

    @Test
    @DisplayName("37. Duplicate attendance: Student already marked PRESENT cannot change attendance status")
    void test37_duplicateAttendance_StudentAlreadyPresentCannotChangeStatus_Rejected() {
        Attendance existing = new Attendance(standardEvent, student, AttendanceMethod.QR_SCAN, null, AttendanceStatus.PRESENT);
        existing.setId(991L);
        existing.setCheckInTime(LocalDateTime.of(2026, 10, 10, 10, 5, 0));
        repoAttendances.add(existing);

        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");
        LocalDateTime laterTime = LocalDateTime.of(2026, 10, 10, 10, 30, 0);

        // Attempting to check in again
        ConflictException ex = assertThrows(ConflictException.class, () ->
                attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", laterTime)
        );

        assertEquals("Attendance has already been marked PRESENT.", ex.getMessage());
        assertEquals(AttendanceStatus.PRESENT, existing.getStatus());
    }

    @Test
    @DisplayName("38. Duplicate attendance: Repeated QR check-in attempt returns error without modifying attendance record")
    void test38_duplicateAttendance_RepeatedQrCheckInReturnsErrorWithoutModifyingRecord() {
        LocalDateTime originalCheckIn = LocalDateTime.of(2026, 10, 10, 10, 10, 0);
        Attendance existing = new Attendance(standardEvent, student, AttendanceMethod.QR_SCAN, null, AttendanceStatus.PRESENT);
        existing.setId(992L);
        existing.setCheckInTime(originalCheckIn);
        repoAttendances.add(existing);

        AttendanceMarkRequest req = new AttendanceMarkRequest("VALID-QR-TOKEN-123");
        LocalDateTime repeatedCheckIn = LocalDateTime.of(2026, 10, 10, 11, 45, 0);

        assertThrows(ConflictException.class, () ->
                attendanceService.markAttendanceViaQr(standardEvent.getId(), req, "student@college.edu", repeatedCheckIn)
        );

        // Verify existing record is not altered
        Attendance inDb = repoAttendances.stream().filter(a -> a.getId().equals(992L)).findFirst().orElseThrow();
        assertEquals(originalCheckIn, inDb.getCheckInTime(), "Original checkInTime must remain intact");
        assertEquals(1, repoAttendances.size(), "No new attendance records created");
    }
}
