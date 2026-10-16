package com.college.eventmanagement;

import com.college.eventmanagement.dto.*;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.repository.*;
import com.college.eventmanagement.service.*;
import com.college.eventmanagement.util.DateTimeUtil;
import com.college.eventmanagement.util.GeoUtil;
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
 * Authoritative Backend Test Suite for 300-Meter Geofenced Attendance.
 * Covers all specifications required in Section 12:
 * 1-10: Distance Calculation & Haversine Formula
 * 11-18: Attendance Geofencing & Business Rule Enforcement
 * 19-22: Role Isolation (Organizer & Admin unaffected)
 * 23-26: Privacy Guarantees (Zero participant location persistence)
 * 27-30: Venue Management & Coordinate Validation
 */
public class GeofenceAttendanceTest {

    private AttendanceService attendanceService;
    private VenueService venueService;

    // Test Data Stores
    private List<Event> repoEvents;
    private List<Registration> repoRegistrations;
    private List<Attendance> repoAttendances;
    private List<Venue> repoVenues;

    private User studentUser;
    private Student student;
    private User organizerUser;
    private Organizer organizer;
    private User adminUser;

    private Venue venueWithCoords;
    private Venue venueWithoutCoords;
    private Event geofencedEvent;
    private Registration registration;

    // Venue base coordinates (IIT Delhi campus: 28.5450, 77.1926)
    private static final double VENUE_LAT = 28.5450;
    private static final double VENUE_LON = 77.1926;

    @BeforeEach
    void setUp() {
        repoEvents = new ArrayList<>();
        repoRegistrations = new ArrayList<>();
        repoAttendances = new ArrayList<>();
        repoVenues = new ArrayList<>();

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

        venueWithCoords = new Venue("APJ Abdul Kalam Auditorium", "Central Block", "AUD-101", 250, "Main Auditorium", VENUE_LAT, VENUE_LON);
        venueWithCoords.setId(1L);
        repoVenues.add(venueWithCoords);

        venueWithoutCoords = new Venue("Old Seminar Room", "Block C", "SEM-02", 50, "Legacy Room");
        venueWithoutCoords.setId(2L);
        repoVenues.add(venueWithoutCoords);

        geofencedEvent = new Event();
        geofencedEvent.setId(500L);
        geofencedEvent.setTitle("AI & Cloud Symposium");
        geofencedEvent.setOrganizer(organizer);
        geofencedEvent.setVenue(venueWithCoords);
        geofencedEvent.setEventDate(LocalDate.of(2026, 10, 10));
        geofencedEvent.setStartTime(LocalTime.of(10, 0));
        geofencedEvent.setEndTime(LocalTime.of(12, 0));
        geofencedEvent.setMaxCapacity(100);
        geofencedEvent.setRegisteredCount(1);
        geofencedEvent.setStatus(EventStatus.PUBLISHED);
        geofencedEvent.setAttendanceActive(true);
        geofencedEvent.setAttendanceToken("GEO-VALID-TOKEN");
        repoEvents.add(geofencedEvent);

        registration = new Registration();
        registration.setId(1000L);
        registration.setEvent(geofencedEvent);
        registration.setStudent(student);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(LocalDateTime.of(2026, 10, 2, 10, 0));
        repoRegistrations.add(registration);

        // Repositories via Dynamic Proxy
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
                    if ("save".equals(name)) {
                        Registration r = (Registration) args[0];
                        if (r.getId() == null) r.setId((long) (repoRegistrations.size() + 500));
                        repoRegistrations.removeIf(existing -> existing.getId().equals(r.getId()));
                        repoRegistrations.add(r);
                        return r;
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
                        return repoVenues.stream().filter(v -> v.getId().equals(vId)).findFirst();
                    }
                    if ("save".equals(name)) {
                        Venue v = (Venue) args[0];
                        if (v.getId() == null) v.setId((long) (repoVenues.size() + 10));
                        repoVenues.removeIf(existing -> existing.getId().equals(v.getId()));
                        repoVenues.add(v);
                        return v;
                    }
                    if ("existsByNameAndBuilding".equals(name)) {
                        String vName = (String) args[0];
                        String bName = (String) args[1];
                        return repoVenues.stream().anyMatch(v -> v.getName().equalsIgnoreCase(vName) && v.getBuilding().equalsIgnoreCase(bName));
                    }
                    if ("findAll".equals(name)) {
                        return new ArrayList<>(repoVenues);
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

        attendanceService = new AttendanceService(
                attendanceRepository,
                eventRepository,
                registrationRepository,
                studentRepository,
                userRepository,
                notificationService
        );

        venueService = new VenueService(venueRepository);
    }

    // Helper: calculate latitude offset for a given distance in meters due north
    private static double metersToLatitudeOffset(double meters) {
        return (meters / GeoUtil.EARTH_RADIUS_METERS) * (180.0 / Math.PI);
    }

    // =========================================================================
    // SECTION 12: DISTANCE CALCULATION TESTS (1 - 10)
    // =========================================================================

    @Test
    @DisplayName("1. Distance Calculation: Participant at exact venue coordinates (0m) -> allowed")
    void test01_exactVenueCoordinates_Allowed() {
        double distance = GeoUtil.calculateDistanceMeters(VENUE_LAT, VENUE_LON, VENUE_LAT, VENUE_LON);
        assertEquals(0.0, distance, 0.001);
        assertTrue(GeoUtil.isWithinGeofence(VENUE_LAT, VENUE_LON, VENUE_LAT, VENUE_LON));
    }

    @Test
    @DisplayName("2. Distance Calculation: Participant 100 meters away -> allowed")
    void test02_participant100MetersAway_Allowed() {
        double offsetLat = VENUE_LAT + metersToLatitudeOffset(100.0);
        double distance = GeoUtil.calculateDistanceMeters(VENUE_LAT, VENUE_LON, offsetLat, VENUE_LON);
        assertEquals(100.0, distance, 0.5);
        assertTrue(GeoUtil.isWithinGeofence(VENUE_LAT, VENUE_LON, offsetLat, VENUE_LON));
    }

    @Test
    @DisplayName("3. Distance Calculation: Participant exactly 300 meters away -> allowed")
    void test03_participantExactly300MetersAway_Allowed() {
        double offsetLat = VENUE_LAT + metersToLatitudeOffset(300.0);
        double distance = GeoUtil.calculateDistanceMeters(VENUE_LAT, VENUE_LON, offsetLat, VENUE_LON);
        assertEquals(300.0, distance, 0.5);
        assertTrue(GeoUtil.isWithinGeofence(VENUE_LAT, VENUE_LON, offsetLat, VENUE_LON));
    }

    @Test
    @DisplayName("4. Distance Calculation: Participant 301 meters away -> rejected")
    void test04_participant301MetersAway_Rejected() {
        double offsetLat = VENUE_LAT + metersToLatitudeOffset(301.0);
        double distance = GeoUtil.calculateDistanceMeters(VENUE_LAT, VENUE_LON, offsetLat, VENUE_LON);
        assertTrue(distance > 300.0, "Distance must exceed 300m");
        assertFalse(GeoUtil.isWithinGeofence(VENUE_LAT, VENUE_LON, offsetLat, VENUE_LON));
    }

    @Test
    @DisplayName("5. Distance Calculation: Participant significantly outside radius (500m & 5km) -> rejected")
    void test05_participantSignificantlyOutside_Rejected() {
        double offset500m = VENUE_LAT + metersToLatitudeOffset(500.0);
        assertFalse(GeoUtil.isWithinGeofence(VENUE_LAT, VENUE_LON, offset500m, VENUE_LON));

        double offset5km = VENUE_LAT + metersToLatitudeOffset(5000.0);
        assertFalse(GeoUtil.isWithinGeofence(VENUE_LAT, VENUE_LON, offset5km, VENUE_LON));
    }

    @Test
    @DisplayName("6. Distance Calculation: Antimeridian / International Date Line crossing handled correctly")
    void test06_antimeridianCrossingHandledCorrectly() {
        // Venue at 0.0, 179.999; participant at 0.0, -179.999.
        // Difference is 0.002 degrees longitude ~222.4 meters, NOT 359.998 degrees (~40,000 km)!
        double venueLat = 0.0, venueLon = 179.999;
        double participantLat = 0.0, participantLon = -179.999;

        double distance = GeoUtil.calculateDistanceMeters(venueLat, venueLon, participantLat, participantLon);
        assertTrue(distance < 300.0, "Shortest path across antimeridian is ~222 meters, must be within 300m");
        assertTrue(GeoUtil.isWithinGeofence(venueLat, venueLon, participantLat, participantLon));
    }

    @Test
    @DisplayName("7. Coordinate Validation: Invalid latitude (< -90 or > 90) -> rejected")
    void test07_invalidLatitudeRejected() {
        assertFalse(GeoUtil.isValidCoordinate(90.1, 77.0));
        assertFalse(GeoUtil.isValidCoordinate(-90.1, 77.0));
        assertFalse(GeoUtil.isValidCoordinate(180.0, 77.0));
    }

    @Test
    @DisplayName("8. Coordinate Validation: Invalid longitude (< -180 or > 180) -> rejected")
    void test08_invalidLongitudeRejected() {
        assertFalse(GeoUtil.isValidCoordinate(28.0, 180.1));
        assertFalse(GeoUtil.isValidCoordinate(28.0, -180.1));
    }

    @Test
    @DisplayName("9. Coordinate Validation: Missing coordinates (null) -> rejected")
    void test09_missingCoordinatesRejected() {
        assertFalse(GeoUtil.isValidCoordinate(null, 77.0));
        assertFalse(GeoUtil.isValidCoordinate(28.0, null));
        assertFalse(GeoUtil.isValidCoordinate(null, null));
    }

    @Test
    @DisplayName("10. Coordinate Validation: NaN and infinite coordinate values -> rejected")
    void test10_nanAndInfiniteCoordinatesRejected() {
        assertFalse(GeoUtil.isValidCoordinate(Double.NaN, 77.0));
        assertFalse(GeoUtil.isValidCoordinate(28.0, Double.NaN));
        assertFalse(GeoUtil.isValidCoordinate(Double.POSITIVE_INFINITY, 77.0));
        assertFalse(GeoUtil.isValidCoordinate(28.0, Double.NEGATIVE_INFINITY));
    }

    // =========================================================================
    // SECTION 12: ATTENDANCE GEOFENCING TESTS (11 - 18)
    // =========================================================================

    @Test
    @DisplayName("11. Attendance: Eligible registered participant within 300 meters is marked PRESENT")
    void test11_participantWithin300Meters_MarkedPresent() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        // Participant 150m from venue
        double studentLat = VENUE_LAT + metersToLatitudeOffset(150.0);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", studentLat, VENUE_LON);

        AttendanceResponse res = attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", checkInTime);

        assertNotNull(res);
        assertEquals(AttendanceStatus.PRESENT, res.getStatus());
        assertEquals(checkInTime, res.getCheckInTime());
        assertEquals(1, repoAttendances.size());
    }

    @Test
    @DisplayName("12. Attendance: Participant outside 300 meters is rejected with HTTP 400")
    void test12_participantOutside300Meters_Rejected() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        // Participant 400m from venue
        double studentLat = VENUE_LAT + metersToLatitudeOffset(400.0);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", studentLat, VENUE_LON);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", checkInTime)
        );

        assertEquals("You are outside the permitted 300-meter attendance area. Please move closer to the event venue and try again.", ex.getMessage());
        assertEquals(0, repoAttendances.size(), "No attendance record must be created");
    }

    @Test
    @DisplayName("13. Attendance: Unregistered participant is rejected")
    void test13_unregisteredParticipant_Rejected() {
        repoRegistrations.clear();
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", VENUE_LAT, VENUE_LON);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", checkInTime)
        );

        assertTrue(ex.getMessage().contains("You are not registered for this event"));
    }

    @Test
    @DisplayName("14. Attendance: Participant before attendance window is rejected")
    void test14_participantBeforeWindow_Rejected() {
        LocalDateTime beforeWindow = LocalDateTime.of(2026, 10, 10, 9, 30);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", VENUE_LAT, VENUE_LON);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", beforeWindow)
        );

        assertTrue(ex.getMessage().contains("Check-in has not started yet"));
    }

    @Test
    @DisplayName("15. Attendance: Participant after attendance window (+24h) is rejected")
    void test15_participantAfterWindow_Rejected() {
        LocalDateTime afterWindow = LocalDateTime.of(2026, 10, 11, 13, 0);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", VENUE_LAT, VENUE_LON);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", afterWindow)
        );

        assertTrue(ex.getMessage().contains("Check-in is closed"));
    }

    @Test
    @DisplayName("16. Attendance: Duplicate attendance remains rejected")
    void test16_duplicateAttendance_Rejected() {
        Attendance existing = new Attendance(geofencedEvent, student, AttendanceMethod.QR_SCAN, null, AttendanceStatus.PRESENT);
        existing.setId(999L);
        existing.setCheckInTime(LocalDateTime.of(2026, 10, 10, 10, 15));
        repoAttendances.add(existing);

        LocalDateTime secondTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", VENUE_LAT, VENUE_LON);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", secondTime)
        );

        assertTrue(ex.getMessage().contains("Attendance has already been marked PRESENT."));
    }

    @Test
    @DisplayName("17. Attendance: Missing venue coordinates prevent participant GPS attendance safely")
    void test17_missingVenueCoordinates_PreventsGpsAttendanceSafely() {
        geofencedEvent.setVenue(venueWithoutCoords); // Venue has NULL lat/lon
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", VENUE_LAT, VENUE_LON);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", checkInTime)
        );

        assertEquals("Location verification is not configured for this venue. Please contact the event organizer.", ex.getMessage());
        assertEquals(0, repoAttendances.size());
    }

    @Test
    @DisplayName("18. Attendance: Attendance session validation still applies (invalid token / inactive session)")
    void test18_sessionValidationStillApplies() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);

        // Invalid token
        AttendanceMarkRequest invalidTokenReq = new AttendanceMarkRequest("WRONG-TOKEN", VENUE_LAT, VENUE_LON);
        BadRequestException tokenEx = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), invalidTokenReq, "student@college.edu", checkInTime)
        );
        assertTrue(tokenEx.getMessage().contains("Invalid or expired attendance QR code."));

        // Inactive session
        geofencedEvent.setAttendanceActive(false);
        AttendanceMarkRequest validReq = new AttendanceMarkRequest("GEO-VALID-TOKEN", VENUE_LAT, VENUE_LON);
        BadRequestException sessionEx = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), validReq, "student@college.edu", checkInTime)
        );
        assertTrue(sessionEx.getMessage().contains("Attendance session is not currently open"));
    }

    // =========================================================================
    // SECTION 12: ROLE ISOLATION TESTS (19 - 22)
    // =========================================================================

    @Test
    @DisplayName("19. Role Isolation: Organizer manual attendance works without GPS coordinates")
    void test19_organizerManualAttendanceWorksWithoutGps() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        ManualAttendanceRequest manualReq = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        attendanceService.markManualAttendance(geofencedEvent.getId(), manualReq, "organizer@college.edu", checkInTime);

        assertEquals(1, repoAttendances.size());
        Attendance att = repoAttendances.get(0);
        assertEquals(AttendanceStatus.PRESENT, att.getStatus());
        assertEquals(AttendanceMethod.MANUAL, att.getMethod());
    }

    @Test
    @DisplayName("20. Role Isolation: Admin manual attendance works without GPS coordinates")
    void test20_adminManualAttendanceWorksWithoutGps() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        ManualAttendanceRequest manualReq = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        attendanceService.markManualAttendance(geofencedEvent.getId(), manualReq, "admin@college.edu", checkInTime);

        assertEquals(1, repoAttendances.size());
        Attendance att = repoAttendances.get(0);
        assertEquals(AttendanceStatus.PRESENT, att.getStatus());
        assertEquals(AttendanceMethod.MANUAL, att.getMethod());
    }

    @Test
    @DisplayName("21. Role Isolation: Organizer manual attendance succeeds even if venue lacks GPS coordinates")
    void test21_organizerManualAttendanceSucceedsWithoutVenueCoordinates() {
        geofencedEvent.setVenue(venueWithoutCoords); // Venue has no coordinates
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        ManualAttendanceRequest manualReq = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        attendanceService.markManualAttendance(geofencedEvent.getId(), manualReq, "organizer@college.edu", checkInTime);

        assertEquals(1, repoAttendances.size());
    }

    @Test
    @DisplayName("22. Role Isolation: Admin manual attendance succeeds even if venue lacks GPS coordinates")
    void test22_adminManualAttendanceSucceedsWithoutVenueCoordinates() {
        geofencedEvent.setVenue(venueWithoutCoords); // Venue has no coordinates
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        ManualAttendanceRequest manualReq = new ManualAttendanceRequest(List.of(student.getId()), AttendanceStatus.PRESENT);

        attendanceService.markManualAttendance(geofencedEvent.getId(), manualReq, "admin@college.edu", checkInTime);

        assertEquals(1, repoAttendances.size());
    }

    // =========================================================================
    // SECTION 12: PRIVACY TESTS (23 - 26)
    // =========================================================================

    @Test
    @DisplayName("23. Privacy: Participant coordinates are not persisted in Attendance entity")
    void test23_participantCoordinatesNotPersistedInAttendance() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", VENUE_LAT, VENUE_LON);

        attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", checkInTime);

        Attendance saved = repoAttendances.get(0);
        // Inspect attendance fields reflectively to guarantee no latitude/longitude columns exist
        Arrays.stream(Attendance.class.getDeclaredFields()).forEach(field -> {
            assertFalse(field.getName().toLowerCase().contains("latitude"), "Attendance must NOT have latitude field");
            assertFalse(field.getName().toLowerCase().contains("longitude"), "Attendance must NOT have longitude field");
            assertFalse(field.getName().toLowerCase().contains("gps"), "Attendance must NOT have GPS field");
        });
    }

    @Test
    @DisplayName("24. Privacy: Participant coordinates are not included in AttendanceResponse DTO")
    void test24_participantCoordinatesNotIncludedInAttendanceResponse() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", VENUE_LAT, VENUE_LON);

        AttendanceResponse res = attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", checkInTime);

        Arrays.stream(AttendanceResponse.class.getDeclaredFields()).forEach(field -> {
            assertFalse(field.getName().toLowerCase().contains("latitude"), "AttendanceResponse must NOT have latitude field");
            assertFalse(field.getName().toLowerCase().contains("longitude"), "AttendanceResponse must NOT have longitude field");
            assertFalse(field.getName().toLowerCase().contains("gps"), "AttendanceResponse must NOT have GPS field");
        });
    }

    @Test
    @DisplayName("25. Privacy: Participant coordinates are not revealed in rejection error messages")
    void test25_participantCoordinatesNotRevealedInErrorMessage() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 10, 10, 10, 30);
        double farLat = VENUE_LAT + 1.0;
        double farLon = VENUE_LON + 1.0;
        AttendanceMarkRequest req = new AttendanceMarkRequest("GEO-VALID-TOKEN", farLat, farLon);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                attendanceService.markAttendanceViaQr(geofencedEvent.getId(), req, "student@college.edu", checkInTime)
        );

        assertFalse(ex.getMessage().contains(String.valueOf(farLat)));
        assertFalse(ex.getMessage().contains(String.valueOf(farLon)));
        assertEquals("You are outside the permitted 300-meter attendance area. Please move closer to the event venue and try again.", ex.getMessage());
    }

    @Test
    @DisplayName("26. Privacy: Centralized radius constant is strictly 300 meters")
    void test26_geofenceRadiusConstantIsStrictly300Meters() {
        assertEquals(300.0, GeoUtil.ATTENDANCE_GEOFENCE_RADIUS_METERS, 0.0001);
    }

    // =========================================================================
    // SECTION 12: VENUE MANAGEMENT TESTS (27 - 30)
    // =========================================================================

    @Test
    @DisplayName("27. Venue Management: Organizer can save valid venue coordinates")
    void test27_organizerCanSaveValidVenueCoordinates() {
        VenueRequest req = new VenueRequest();
        req.setName("Tech Conference Room");
        req.setBuilding("Academic Tower");
        req.setRoomNumber("T-401");
        req.setCapacity(60);
        req.setDescription("Smart seminar room");
        req.setLatitude(28.5458);
        req.setLongitude(77.1932);

        VenueResponse saved = venueService.createVenue(req);

        assertNotNull(saved);
        assertEquals(28.5458, saved.getLatitude());
        assertEquals(77.1932, saved.getLongitude());
    }

    @Test
    @DisplayName("28. Venue Management: Invalid venue coordinates are rejected")
    void test28_invalidVenueCoordinatesRejected() {
        VenueRequest req = new VenueRequest();
        req.setName("Invalid Venue");
        req.setBuilding("Academic Tower");
        req.setRoomNumber("T-402");
        req.setCapacity(60);
        req.setLatitude(99.0); // Invalid latitude > 90
        req.setLongitude(77.1932);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                venueService.createVenue(req)
        );

        assertTrue(ex.getMessage().contains("Invalid venue coordinates"));
    }

    @Test
    @DisplayName("29. Venue Management: Existing venues without coordinates remain usable by organizers/admins")
    void test29_existingVenuesWithoutCoordinatesRemainUsable() {
        VenueResponse response = venueService.getVenueById(venueWithoutCoords.getId());
        assertNotNull(response);
        assertNull(response.getLatitude());
        assertNull(response.getLongitude());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("30. Venue Management: Venue coordinates are available to attendance validation service")
    void test30_venueCoordinatesAvailableToAttendanceValidation() {
        Venue venue = geofencedEvent.getVenue();
        assertNotNull(venue);
        assertEquals(VENUE_LAT, venue.getLatitude());
        assertEquals(VENUE_LON, venue.getLongitude());
        assertTrue(GeoUtil.isValidCoordinate(venue.getLatitude(), venue.getLongitude()));
    }
}
