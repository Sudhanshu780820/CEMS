package com.college.eventmanagement;

import com.college.eventmanagement.dto.EventResponse;
import com.college.eventmanagement.dto.RegistrationResponse;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.repository.*;
import com.college.eventmanagement.service.EventService;
import com.college.eventmanagement.service.NotificationService;
import com.college.eventmanagement.service.RegistrationService;
import com.college.eventmanagement.service.VenueService;
import com.college.eventmanagement.util.DateTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AdminEventSortingAndRegistrationRulesTest {

    private EventService eventService;
    private RegistrationService registrationService;

    private User studentUser;
    private Student approvedStudent;
    private Student pendingStudent;
    private Student ineligibleStudent;
    private User adminUser;
    private Venue testVenue;
    private Organizer testOrganizer;

    private List<Event> repositoryEvents;
    private List<Registration> repositoryRegistrations;

    @BeforeEach
    void setUp() {
        repositoryEvents = new ArrayList<>();
        repositoryRegistrations = new ArrayList<>();

        studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("student@college.edu");
        studentUser.setRole(Role.ROLE_STUDENT);

        approvedStudent = new Student();
        approvedStudent.setId(1L);
        approvedStudent.setUser(studentUser);
        approvedStudent.setFullName("Aarav Patel");
        approvedStudent.setBranch("CSE");
        approvedStudent.setCurrentYear("3rd Year");
        approvedStudent.setSection("A");
        approvedStudent.setApprovalStatus(ApprovalStatus.APPROVED);

        User pendingUser = new User();
        pendingUser.setId(11L);
        pendingUser.setEmail("pending@college.edu");
        pendingUser.setRole(Role.ROLE_STUDENT);

        pendingStudent = new Student();
        pendingStudent.setId(2L);
        pendingStudent.setUser(pendingUser);
        pendingStudent.setFullName("Pending Student");
        pendingStudent.setBranch("CSE");
        pendingStudent.setCurrentYear("3rd Year");
        pendingStudent.setSection("A");
        pendingStudent.setApprovalStatus(ApprovalStatus.PENDING);

        User ineligibleUser = new User();
        ineligibleUser.setId(12L);
        ineligibleUser.setEmail("civil@college.edu");
        ineligibleUser.setRole(Role.ROLE_STUDENT);

        ineligibleStudent = new Student();
        ineligibleStudent.setId(3L);
        ineligibleStudent.setUser(ineligibleUser);
        ineligibleStudent.setFullName("Civil Student");
        ineligibleStudent.setBranch("Civil");
        ineligibleStudent.setCurrentYear("1st Year");
        ineligibleStudent.setSection("B");
        ineligibleStudent.setApprovalStatus(ApprovalStatus.APPROVED);

        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setEmail("admin@college.edu");
        adminUser.setRole(Role.ROLE_ADMIN);

        testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Vikram Sarabhai Auditorium");
        testVenue.setBuilding("Academic Block 1");
        testVenue.setCapacity(250);
        testVenue.setLatitude(28.5450);
        testVenue.setLongitude(77.1926);
        testVenue.setActive(true);

        User orgUser = new User();
        orgUser.setId(20L);
        orgUser.setEmail("organizer@college.edu");
        orgUser.setRole(Role.ROLE_ORGANIZER);

        testOrganizer = new Organizer();
        testOrganizer.setId(1L);
        testOrganizer.setUser(orgUser);
        testOrganizer.setName("Prof. Sen");
        testOrganizer.setDepartment("Computer Science");
        testOrganizer.setContactEmail("organizer@college.edu");

        // Proxy UserRepository
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String email = (String) args[0];
                    if ("student@college.edu".equals(email)) return Optional.of(studentUser);
                    if ("pending@college.edu".equals(email)) return Optional.of(pendingUser);
                    if ("civil@college.edu".equals(email)) return Optional.of(ineligibleUser);
                    if ("admin@college.edu".equals(email)) return Optional.of(adminUser);
                    return Optional.empty();
                }
        );

        // Proxy StudentRepository
        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> {
                    Long userId = (Long) args[0];
                    if (Long.valueOf(10L).equals(userId)) return Optional.of(approvedStudent);
                    if (Long.valueOf(11L).equals(userId)) return Optional.of(pendingStudent);
                    if (Long.valueOf(12L).equals(userId)) return Optional.of(ineligibleStudent);
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
                        Long id = (Long) args[0];
                        return repositoryEvents.stream().filter(e -> e.getId().equals(id)).findFirst();
                    }
                    if ("incrementRegisteredCountIfAvailable".equals(name)) {
                        Long id = (Long) args[0];
                        Event ev = repositoryEvents.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
                        if (ev != null && ev.getRegisteredCount() < ev.getMaxCapacity()) {
                            ev.setRegisteredCount(ev.getRegisteredCount() + 1);
                            return 1;
                        }
                        return 0;
                    }
                    if ("searchEvents".equals(name)) {
                        EventStatus status = (EventStatus) args[0];
                        String category = (String) args[1];
                        LocalDate start = (LocalDate) args[2];
                        LocalDate end = (LocalDate) args[3];
                        String query = (String) args[4];

                        return repositoryEvents.stream()
                                .filter(e -> status == null || e.getStatus() == status)
                                .filter(e -> category == null || category.equalsIgnoreCase(e.getCategory()))
                                .filter(e -> start == null || !e.getEventDate().isBefore(start))
                                .filter(e -> end == null || !e.getEventDate().isAfter(end))
                                .filter(e -> query == null || e.getTitle().toLowerCase().contains(query.toLowerCase())
                                        || e.getDescription().toLowerCase().contains(query.toLowerCase()))
                                .sorted(Comparator.comparing(Event::getEventDate).thenComparing(Event::getStartTime))
                                .toList();
                    }
                    if ("findUpcomingPublishedEvents".equals(name)) {
                        LocalDate today = (LocalDate) args[0];
                        LocalTime now = (LocalTime) args[1];
                        return repositoryEvents.stream()
                                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                                .filter(e -> e.getEventDate().isAfter(today) || (e.getEventDate().isEqual(today) && e.getEndTime().isAfter(now)))
                                .sorted(Comparator.comparing(Event::getEventDate).thenComparing(Event::getStartTime))
                                .toList();
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
                        Long evId = (Long) args[0];
                        Long stId = (Long) args[1];
                        return repositoryRegistrations.stream()
                                .filter(r -> r.getEvent().getId().equals(evId) && r.getStudent().getId().equals(stId))
                                .findFirst();
                    }
                    if ("findByStudentOrderByRegisteredAtDesc".equals(name)) {
                        Student st = (Student) args[0];
                        return repositoryRegistrations.stream()
                                .filter(r -> r.getStudent().getId().equals(st.getId()))
                                .toList();
                    }
                    if ("save".equals(name)) {
                        Registration reg = (Registration) args[0];
                        reg.setId((long) (repositoryRegistrations.size() + 1));
                        repositoryRegistrations.add(reg);
                        return reg;
                    }
                    return null;
                }
        );

        NotificationRepository notificationRepository = (NotificationRepository) Proxy.newProxyInstance(
                NotificationRepository.class.getClassLoader(),
                new Class<?>[]{NotificationRepository.class},
                (proxy, method, args) -> null
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

    private Event buildTestEvent(Long id, String title, LocalDate eventDate, LocalTime startTime, LocalTime endTime,
                                  LocalDate regStart, LocalDate regEnd, EventStatus status) {
        Event e = new Event();
        e.setId(id);
        e.setTitle(title);
        e.setDescription("Description of " + title);
        e.setCategory("Technical");
        e.setVenue(testVenue);
        e.setOrganizer(testOrganizer);
        e.setEventDate(eventDate);
        e.setStartTime(startTime);
        e.setEndTime(endTime);
        e.setRegistrationStartDate(regStart);
        e.setRegistrationEndDate(regEnd);
        e.setMaxCapacity(100);
        e.setRegisteredCount(0);
        e.setEligibleBranches("CSE,IT");
        e.setEligibleAcademicYears("3rd Year");
        e.setStatus(status);
        return e;
    }

    // =========================================================================
    // REGISTRATION WINDOW & STATUS TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Before registration start → registration is rejected")
    void test1_beforeRegistrationStart_registrationRejected() {
        LocalDate today = DateTimeUtil.today();
        Event event = buildTestEvent(1L, "Future Hackathon", today.plusDays(10), LocalTime.of(10, 0), LocalTime.of(16, 0),
                today.plusDays(2), today.plusDays(8), EventStatus.PUBLISHED);
        repositoryEvents.add(event);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(1L, "student@college.edu")
        );
        assertTrue(ex.getMessage().contains("Registration has not started yet"));
        assertTrue(ex.getMessage().contains(event.getRegistrationStartDate().toString()));
    }

    @Test
    @DisplayName("2. During registration window → eligible registration succeeds")
    void test2_duringRegistrationWindow_registrationSucceeds() {
        LocalDate today = DateTimeUtil.today();
        Event event = buildTestEvent(2L, "Open AI Summit", today.plusDays(5), LocalTime.of(9, 0), LocalTime.of(17, 0),
                today.minusDays(2), today.plusDays(3), EventStatus.PUBLISHED);
        repositoryEvents.add(event);

        RegistrationResponse response = registrationService.registerForEvent(2L, "student@college.edu");
        assertNotNull(response);
        assertEquals(RegistrationStatus.REGISTERED, response.getStatus());
        assertEquals(1, repositoryRegistrations.size());
        assertEquals(1, event.getRegisteredCount());
    }

    @Test
    @DisplayName("3. After registration deadline → registration is rejected")
    void test3_afterRegistrationDeadline_registrationRejected() {
        LocalDate today = DateTimeUtil.today();
        Event event = buildTestEvent(3L, "Closed Coding Contest", today.plusDays(3), LocalTime.of(10, 0), LocalTime.of(13, 0),
                today.minusDays(7), today.minusDays(1), EventStatus.PUBLISHED);
        repositoryEvents.add(event);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(3L, "student@college.edu")
        );
        assertTrue(ex.getMessage().contains("Registration is closed"));
        assertTrue(ex.getMessage().contains(event.getRegistrationEndDate().toString()));
    }

    @Test
    @DisplayName("4. Already registered student → duplicate registration is prevented")
    void test4_alreadyRegistered_duplicatePrevented() {
        LocalDate today = DateTimeUtil.today();
        Event event = buildTestEvent(4L, "Duplicate Test Workshop", today.plusDays(5), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(2), today.plusDays(3), EventStatus.PUBLISHED);
        repositoryEvents.add(event);

        // First registration succeeds
        registrationService.registerForEvent(4L, "student@college.edu");

        // Second registration attempt is rejected with 409 Conflict
        ConflictException ex = assertThrows(ConflictException.class, () ->
                registrationService.registerForEvent(4L, "student@college.edu")
        );
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("5. Closed registration status is represented correctly on DTO")
    void test5_closedRegistrationStatusRepresentedCorrectly() {
        LocalDate today = DateTimeUtil.today();
        Event event = buildTestEvent(5L, "Upcoming Event with Closed Reg", today.plusDays(4), LocalTime.of(11, 0), LocalTime.of(14, 0),
                today.minusDays(5), today.minusDays(1), EventStatus.PUBLISHED);

        EventResponse response = eventService.mapToEventResponse(event, approvedStudent);
        assertEquals("CLOSED", response.getRegistrationState());
        assertEquals("REGISTRATION CLOSED", response.getActionStatus());
        assertFalse(response.isRegistrationOpen());
        assertFalse(response.isRegistered());
    }

    @Test
    @DisplayName("6. Registration status uses authoritative backend date/time")
    void test6_registrationStatusUsesAuthoritativeDateTime() {
        LocalDate today = DateTimeUtil.today();

        // Not started
        Event notStarted = buildTestEvent(61L, "Future Event", today.plusDays(5), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.plusDays(1), today.plusDays(4), EventStatus.PUBLISHED);
        EventResponse resNotStarted = eventService.mapToEventResponse(notStarted, approvedStudent);
        assertEquals("NOT_STARTED", resNotStarted.getRegistrationState());
        assertEquals("REGISTRATION NOT STARTED", resNotStarted.getActionStatus());

        // Open
        Event open = buildTestEvent(62L, "Open Event", today.plusDays(5), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(1), today.plusDays(4), EventStatus.PUBLISHED);
        EventResponse resOpen = eventService.mapToEventResponse(open, approvedStudent);
        assertEquals("OPEN", resOpen.getRegistrationState());
        assertEquals("REGISTER NOW", resOpen.getActionStatus());

        // Closed
        Event closed = buildTestEvent(63L, "Closed Event", today.plusDays(5), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(5), today.minusDays(1), EventStatus.PUBLISHED);
        EventResponse resClosed = eventService.mapToEventResponse(closed, approvedStudent);
        assertEquals("CLOSED", resClosed.getRegistrationState());
        assertEquals("REGISTRATION CLOSED", resClosed.getActionStatus());
    }

    @Test
    @DisplayName("7. Completed events are excluded from recommendations")
    void test7_completedEventsExcludedFromRecommendations() {
        LocalDate today = DateTimeUtil.today();
        LocalTime now = DateTimeUtil.currentTime();

        // Past event yesterday
        Event pastYesterday = buildTestEvent(71L, "Yesterday Fest", today.minusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(10), today.minusDays(2), EventStatus.PUBLISHED);
        // Explicitly COMPLETED status event
        Event completedStatus = buildTestEvent(72L, "Completed Symposium", today.plusDays(2), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(5), today.minusDays(1), EventStatus.COMPLETED);
        // Future upcoming event
        Event futureEvent = buildTestEvent(73L, "Upcoming Workshop", today.plusDays(3), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(2), today.plusDays(2), EventStatus.PUBLISHED);

        repositoryEvents.addAll(List.of(pastYesterday, completedStatus, futureEvent));

        List<EventResponse> recommendations = eventService.getRecommendations("student@college.edu");
        assertEquals(1, recommendations.size());
        assertEquals("Upcoming Workshop", recommendations.get(0).getTitle());
    }

    @Test
    @DisplayName("8. Existing eligibility and approval checks remain enforced")
    void test8_eligibilityAndApprovalChecksEnforced() {
        LocalDate today = DateTimeUtil.today();
        Event event = buildTestEvent(8L, "CSE Only Hackathon", today.plusDays(5), LocalTime.of(10, 0), LocalTime.of(15, 0),
                today.minusDays(2), today.plusDays(3), EventStatus.PUBLISHED);
        repositoryEvents.add(event);

        // 1. Unapproved student rejected
        BadRequestException unapprovedEx = assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(8L, "pending@college.edu")
        );
        assertTrue(unapprovedEx.getMessage().contains("not approved yet"));

        // 2. Ineligible branch student rejected
        BadRequestException ineligibleEx = assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(8L, "civil@college.edu")
        );
        assertTrue(ineligibleEx.getMessage().contains("not eligible"));
    }

    // =========================================================================
    // ADMIN EVENT SORTING TESTS (eventDate ASC, startTime ASC)
    // =========================================================================

    @Test
    @DisplayName("9. Admin sorting: Events on different dates are ordered chronologically")
    void test9_adminEventsOrderedChronologicallyAcrossDates() {
        LocalDate today = DateTimeUtil.today();

        Event laterDate = buildTestEvent(1L, "Later Date Event", today.plusDays(5), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(2), today.plusDays(4), EventStatus.PUBLISHED);
        Event earlierDate = buildTestEvent(2L, "Earlier Date Event", today.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(2), today, EventStatus.PUBLISHED);

        repositoryEvents.add(laterDate);
        repositoryEvents.add(earlierDate);

        List<EventResponse> adminEvents = eventService.getAllEventsForAdmin(null, null, null, null, null);
        assertEquals(2, adminEvents.size());
        assertEquals("Earlier Date Event", adminEvents.get(0).getTitle());
        assertEquals("Later Date Event", adminEvents.get(1).getTitle());
    }

    @Test
    @DisplayName("10. Admin sorting: Events on the same date are ordered by start time")
    void test10_adminEventsOrderedByStartTimeOnSameDate() {
        LocalDate sameDate = DateTimeUtil.today().plusDays(2);

        Event afternoonEvent = buildTestEvent(1L, "Afternoon Lecture", sameDate, LocalTime.of(14, 0), LocalTime.of(16, 0),
                sameDate.minusDays(5), sameDate.minusDays(1), EventStatus.PUBLISHED);
        Event morningEvent = buildTestEvent(2L, "Morning Keynote", sameDate, LocalTime.of(9, 30), LocalTime.of(11, 0),
                sameDate.minusDays(5), sameDate.minusDays(1), EventStatus.PUBLISHED);

        repositoryEvents.add(afternoonEvent);
        repositoryEvents.add(morningEvent);

        List<EventResponse> adminEvents = eventService.getAllEventsForAdmin(null, null, null, null, null);
        assertEquals(2, adminEvents.size());
        assertEquals("Morning Keynote", adminEvents.get(0).getTitle());
        assertEquals("Afternoon Lecture", adminEvents.get(1).getTitle());
    }

    @Test
    @DisplayName("11. Admin sorting: Registration dates do NOT affect event ordering (prompt example)")
    void test11_registrationDatesDoNotAffectEventOrdering() {
        // Event A: 20 October 2026, 10:00 AM, reg deadline 10 October
        Event eventA = buildTestEvent(101L, "Event A", LocalDate.of(2026, 10, 20), LocalTime.of(10, 0), LocalTime.of(12, 0),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), EventStatus.PUBLISHED);

        // Event B: 18 October 2026, 2:00 PM (14:00), reg deadline 15 October
        Event eventB = buildTestEvent(102L, "Event B", LocalDate.of(2026, 10, 18), LocalTime.of(14, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), EventStatus.PUBLISHED);

        // Event C: 18 October 2026, 9:00 AM, reg deadline 16 October
        Event eventC = buildTestEvent(103L, "Event C", LocalDate.of(2026, 10, 18), LocalTime.of(9, 0), LocalTime.of(11, 0),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 16), EventStatus.PUBLISHED);

        // Add in random insertion order
        repositoryEvents.add(eventA);
        repositoryEvents.add(eventB);
        repositoryEvents.add(eventC);

        List<EventResponse> adminEvents = eventService.getAllEventsForAdmin(null, null, null, null, null);

        assertEquals(3, adminEvents.size());
        // Expected order:
        // 1. Event C — 18 October, 9:00 AM
        // 2. Event B — 18 October, 2:00 PM
        // 3. Event A — 20 October, 10:00 AM
        assertEquals("Event C", adminEvents.get(0).getTitle(), "Soonest event on Oct 18 morning should be first");
        assertEquals("Event B", adminEvents.get(1).getTitle(), "Oct 18 afternoon event should be second");
        assertEquals("Event A", adminEvents.get(2).getTitle(), "Oct 20 event should be third despite earliest reg deadline");
    }

    @Test
    @DisplayName("12. Admin sorting: Past events remain visible in All Events when no filter excludes them")
    void test12_pastEventsRemainVisibleInAdminAllEvents() {
        LocalDate today = DateTimeUtil.today();

        Event pastEvent = buildTestEvent(1L, "Past Orientation", today.minusDays(30), LocalTime.of(10, 0), LocalTime.of(12, 0),
                today.minusDays(40), today.minusDays(32), EventStatus.COMPLETED);
        Event futureEvent = buildTestEvent(2L, "Future Robotics Expo", today.plusDays(15), LocalTime.of(10, 0), LocalTime.of(16, 0),
                today.minusDays(5), today.plusDays(10), EventStatus.PUBLISHED);

        repositoryEvents.add(futureEvent);
        repositoryEvents.add(pastEvent);

        List<EventResponse> adminEvents = eventService.getAllEventsForAdmin(null, null, null, null, null);
        assertEquals(2, adminEvents.size(), "Both past and future events must be visible to admin");
        assertEquals("Past Orientation", adminEvents.get(0).getTitle(), "Past event should appear first chronologically");
        assertEquals("Future Robotics Expo", adminEvents.get(1).getTitle());
    }

    @Test
    @DisplayName("13. Admin sorting remains correct with category and search filters")
    void test13_sortingRemainsCorrectWithCategoryAndSearchFilters() {
        LocalDate today = DateTimeUtil.today();

        Event cultural1 = buildTestEvent(1L, "Cultural Music Night", today.plusDays(2), LocalTime.of(18, 0), LocalTime.of(21, 0),
                today.minusDays(5), today, EventStatus.PUBLISHED);
        cultural1.setCategory("Cultural");

        Event cultural2 = buildTestEvent(2L, "Cultural Dance Fest", today.plusDays(1), LocalTime.of(11, 0), LocalTime.of(14, 0),
                today.minusDays(5), today, EventStatus.PUBLISHED);
        cultural2.setCategory("Cultural");

        Event technical = buildTestEvent(3L, "Technical Coding Jam", today.plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0),
                today.minusDays(5), today, EventStatus.PUBLISHED);
        technical.setCategory("Technical");

        repositoryEvents.addAll(List.of(cultural1, cultural2, technical));

        // Filter by category "Cultural"
        List<EventResponse> filtered = eventService.getAllEventsForAdmin(null, "Cultural", null, null, null);
        assertEquals(2, filtered.size());
        assertEquals("Cultural Dance Fest", filtered.get(0).getTitle(), "Day 1 event should come before Day 2 event");
        assertEquals("Cultural Music Night", filtered.get(1).getTitle());
    }

    @Test
    @DisplayName("14. After event end time → registration is rejected even if registrationEndDate was not set earlier")
    void test14_afterEventEndTime_registrationRejected() {
        LocalDate today = DateTimeUtil.today();
        LocalTime now = DateTimeUtil.currentTime();

        // Event from earlier today whose end time has already elapsed
        Event endedToday = buildTestEvent(14L, "Ended Today Morning", today, LocalTime.of(0, 1), LocalTime.of(0, 2),
                today.minusDays(5), today, EventStatus.PUBLISHED);
        repositoryEvents.add(endedToday);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(14L, "student@college.edu")
        );
        assertTrue(ex.getMessage().contains("already ended"));
    }
}
