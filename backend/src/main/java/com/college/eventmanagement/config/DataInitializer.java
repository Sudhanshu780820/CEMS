package com.college.eventmanagement.config;

import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final OrganizerRepository organizerRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           StudentRepository studentRepository,
                           OrganizerRepository organizerRepository,
                           VenueRepository venueRepository,
                           EventRepository eventRepository,
                           RegistrationRepository registrationRepository,
                           AttendanceRepository attendanceRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.organizerRepository = organizerRepository;
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Seed data already exists
        }

        System.out.println(">>> Initializing Seed Data for College Event Management System...");

        // 1. Admin Account
        User adminUser = new User("admin@college.edu", passwordEncoder.encode("Admin@123"), Role.ROLE_ADMIN);
        userRepository.save(adminUser);

        // 2. Organizer Accounts
        User orgUser1 = new User("cs.dept@college.edu", passwordEncoder.encode("Organizer@123"), Role.ROLE_ORGANIZER);
        userRepository.save(orgUser1);
        Organizer organizer1 = new Organizer();
        organizer1.setUser(orgUser1);
        organizer1.setName("Department of Computer Science");
        organizer1.setDepartment("Computer Science & Engineering");
        organizer1.setContactEmail("cs.dept@college.edu");
        organizer1.setPhoneNumber("+91 98765 43210");
        organizer1.setApproved(true);
        organizerRepository.save(organizer1);

        User orgUser2 = new User("robotics.club@college.edu", passwordEncoder.encode("Organizer@123"), Role.ROLE_ORGANIZER);
        userRepository.save(orgUser2);
        Organizer organizer2 = new Organizer();
        organizer2.setUser(orgUser2);
        organizer2.setName("Robotics & AI Society");
        organizer2.setDepartment("Student Activity Center");
        organizer2.setContactEmail("robotics.club@college.edu");
        organizer2.setPhoneNumber("+91 98765 43211");
        organizer2.setApproved(true);
        organizerRepository.save(organizer2);

        // 3. Venues
        Venue auditorium = new Venue("APJ Abdul Kalam Auditorium", "Central Academic Block", "AUD-101", 250, "Air-conditioned grand hall with 4K projector and sound system");
        venueRepository.save(auditorium);

        Venue seminarHall = new Venue("Sir CV Raman Seminar Hall", "Tech Block B", "ST-204", 80, "Tiered seminar hall equipped for hybrid conferences and workshops");
        venueRepository.save(seminarHall);

        Venue compLab = new Venue("Advanced Computing Lab 3", "IT Innovation Center", "LAB-302", 40, "High-performance workstation lab with dual displays");
        venueRepository.save(compLab);

        Venue amphitheatre = new Venue("Open Air Amphitheatre", "Campus Greens", "OAA-01", 500, "Spacious outdoor stage for cultural festivals and ceremonies");
        venueRepository.save(amphitheatre);

        // 4. Students
        // Approved Students
        Student student1 = createStudent(
                "rahul.cse@college.edu", "Student@123",
                "Rahul Sharma", "EN2023CSE001", "CSE", "2023-2027", "3rd Year", "Sem 5", "A", "+91 91234 56780",
                ApprovalStatus.APPROVED, null
        );

        Student student2 = createStudent(
                "priya.it@college.edu", "Student@123",
                "Priya Patel", "EN2023IT042", "IT", "2023-2027", "3rd Year", "Sem 5", "B", "+91 91234 56781",
                ApprovalStatus.APPROVED, null
        );

        Student student3 = createStudent(
                "arjun.aiml@college.edu", "Student@123",
                "Arjun Verma", "EN2024AIML015", "AI/ML", "2024-2028", "2nd Year", "Sem 3", "A", "+91 91234 56782",
                ApprovalStatus.APPROVED, null
        );

        // Pending Students (For testing Admin approval workflow!)
        Student student4 = createStudent(
                "sneha.ece@college.edu", "Student@123",
                "Sneha Reddy", "EN2024ECE089", "ECE", "2024-2028", "2nd Year", "Sem 3", "A", "+91 91234 56783",
                ApprovalStatus.PENDING, null
        );

        Student student5 = createStudent(
                "ananya.civil@college.edu", "Student@123",
                "Ananya Gupta", "EN2025CIV012", "Civil", "2025-2029", "1st Year", "Sem 1", "C", "+91 91234 56784",
                ApprovalStatus.PENDING, null
        );

        // Suspended Student
        Student student6 = createStudent(
                "vikram.me@college.edu", "Student@123",
                "Vikram Rao", "EN2022ME033", "ME", "2022-2026", "4th Year", "Sem 7", "A", "+91 91234 56785",
                ApprovalStatus.SUSPENDED, "Violation of campus conduct rules"
        );

        // 5. Events
        LocalDate today = LocalDate.now();

        // Event 1: AI & Deep Learning Masterclass (upcoming, CSE + AI/ML)
        Event event1 = new Event();
        event1.setTitle("AI & Deep Learning Masterclass");
        event1.setDescription("Deep dive into deep neural networks, transformer architectures, and hands-on PyTorch model training led by industry experts.");
        event1.setCategory("Workshop");
        event1.setEventDate(today.plusDays(3));
        event1.setStartTime(LocalTime.of(10, 0));
        event1.setEndTime(LocalTime.of(13, 0));
        event1.setVenue(seminarHall);
        event1.setOrganizer(organizer1);
        event1.setMaxCapacity(40);
        event1.setRegisteredCount(2);
        event1.setRegistrationStartDate(today.minusDays(5));
        event1.setRegistrationEndDate(today.plusDays(2));
        event1.setEligibleBranches("CSE,AI/ML");
        event1.setEligibleAcademicYears("2nd Year,3rd Year");
        event1.setEligibleSections(""); // all sections
        event1.setEventImageUrl("https://images.unsplash.com/photo-1677442136019-21780efad99a?auto=format&fit=crop&w=1200&q=80");
        event1.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event1);

        // Event 2: Annual College Hackathon 2026 (upcoming, All branches)
        Event event2 = new Event();
        event2.setTitle("CodeCraft: 24-Hour Annual College Hackathon");
        event2.setDescription("Build real-world solutions for smart campus and sustainability. Cash prizes of over Rs 1,00,000 and direct internship opportunities.");
        event2.setCategory("Technical");
        event2.setEventDate(today.plusDays(7));
        event2.setStartTime(LocalTime.of(9, 0));
        event2.setEndTime(LocalTime.of(18, 0));
        event2.setVenue(auditorium);
        event2.setOrganizer(organizer1);
        event2.setMaxCapacity(150);
        event2.setRegisteredCount(3);
        event2.setRegistrationStartDate(today.minusDays(7));
        event2.setRegistrationEndDate(today.plusDays(5));
        event2.setEligibleBranches(""); // open to all
        event2.setEligibleAcademicYears("");
        event2.setEligibleSections("");
        event2.setEventImageUrl("https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80");
        event2.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event2);

        // Event 3: Autonomous Robotics Showcase (Today's event, Active QR session!)
        Event event3 = new Event();
        event3.setTitle("Autonomous Robotics Showcase & Live Demo");
        event3.setDescription("Live demonstrations of line followers, maze solvers, and ROS-powered quadruped robots designed by robotics society scholars.");
        event3.setCategory("Technical");
        event3.setEventDate(today);
        event3.setStartTime(LocalTime.of(14, 0));
        event3.setEndTime(LocalTime.of(17, 0));
        event3.setVenue(compLab);
        event3.setOrganizer(organizer2);
        event3.setMaxCapacity(35);
        event3.setRegisteredCount(2);
        event3.setRegistrationStartDate(today.minusDays(3));
        event3.setRegistrationEndDate(today);
        event3.setEligibleBranches("CSE,IT,ECE,ME,AI/ML");
        event3.setEligibleAcademicYears("");
        event3.setEligibleSections("");
        event3.setEventImageUrl("https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=1200&q=80");
        event3.setStatus(EventStatus.PUBLISHED);
        event3.setAttendanceToken("QR-SAMPLE-ROBOTICS-SESSION-2026");
        event3.setAttendanceActive(true); // Active QR code session!
        eventRepository.save(event3);

        // Event 4: Campus Spring Cultural Fest (Cultural)
        Event event4 = new Event();
        event4.setTitle("Vibrance 2026: Campus Cultural Fiesta");
        event4.setDescription("Musical performances, street play competitions, dance battles, and food stalls across the central amphitheatre.");
        event4.setCategory("Cultural");
        event4.setEventDate(today.plusDays(14));
        event4.setStartTime(LocalTime.of(16, 0));
        event4.setEndTime(LocalTime.of(21, 0));
        event4.setVenue(amphitheatre);
        event4.setOrganizer(organizer2);
        event4.setMaxCapacity(400);
        event4.setRegisteredCount(1);
        event4.setRegistrationStartDate(today.minusDays(2));
        event4.setRegistrationEndDate(today.plusDays(10));
        event4.setEligibleBranches("");
        event4.setEligibleAcademicYears("");
        event4.setEligibleSections("");
        event4.setEventImageUrl("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80");
        event4.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event4);

        // Event 5: Completed Symposium (Past event with attendance)
        Event event5 = new Event();
        event5.setTitle("Cloud Architecture & DevOps Symposium");
        event5.setDescription("Keynote sessions on Kubernetes orchestration, AWS cloud design patterns, and CI/CD pipelines.");
        event5.setCategory("Seminar");
        event5.setEventDate(today.minusDays(4));
        event5.setStartTime(LocalTime.of(11, 0));
        event5.setEndTime(LocalTime.of(14, 0));
        event5.setVenue(seminarHall);
        event5.setOrganizer(organizer1);
        event5.setMaxCapacity(80);
        event5.setRegisteredCount(3);
        event5.setRegistrationStartDate(today.minusDays(15));
        event5.setRegistrationEndDate(today.minusDays(5));
        event5.setEligibleBranches("CSE,IT");
        event5.setEligibleAcademicYears("");
        event5.setEligibleSections("");
        event5.setEventImageUrl("https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80");
        event5.setStatus(EventStatus.COMPLETED);
        eventRepository.save(event5);

        // 6. Registrations & Attendance Seeds
        // Registrations for Event 1 (Rahul & Arjun)
        Registration reg1 = new Registration(event1, student1);
        registrationRepository.save(reg1);
        Registration reg2 = new Registration(event1, student3);
        registrationRepository.save(reg2);

        // Registrations for Event 2 (Rahul, Priya, Arjun)
        Registration reg3 = new Registration(event2, student1);
        registrationRepository.save(reg3);
        Registration reg4 = new Registration(event2, student2);
        registrationRepository.save(reg4);
        Registration reg5 = new Registration(event2, student3);
        registrationRepository.save(reg5);

        // Registrations for Event 3 (Rahul & Priya)
        Registration reg6 = new Registration(event3, student1);
        registrationRepository.save(reg6);
        Registration reg7 = new Registration(event3, student2);
        registrationRepository.save(reg7);

        // Registrations for Event 4 (Priya)
        Registration reg8 = new Registration(event4, student2);
        registrationRepository.save(reg8);

        // Registrations and Attendance for Completed Event 5
        Registration reg9 = new Registration(event5, student1);
        reg9.setStatus(RegistrationStatus.ATTENDED);
        registrationRepository.save(reg9);
        Attendance att1 = new Attendance(event5, student1, AttendanceMethod.QR_SCAN, null, AttendanceStatus.PRESENT);
        att1.setCheckInTime(LocalDateTime.now().minusDays(4));
        attendanceRepository.save(att1);

        Registration reg10 = new Registration(event5, student2);
        reg10.setStatus(RegistrationStatus.ATTENDED);
        registrationRepository.save(reg10);
        Attendance att2 = new Attendance(event5, student2, AttendanceMethod.MANUAL, adminUser, AttendanceStatus.PRESENT);
        att2.setCheckInTime(LocalDateTime.now().minusDays(4));
        attendanceRepository.save(att2);

        Registration reg11 = new Registration(event5, student3);
        reg11.setStatus(RegistrationStatus.ABSENT);
        registrationRepository.save(reg11);
        Attendance att3 = new Attendance(event5, student3, AttendanceMethod.MANUAL, adminUser, AttendanceStatus.ABSENT);
        attendanceRepository.save(att3);

        System.out.println(">>> Seed Data Successfully Initialized!");
    }

    private Student createStudent(String email, String password, String fullName, String enrollmentId,
                                  String branch, String academicYear, String currentYear, String currentSemester,
                                  String section, String phone, ApprovalStatus status, String rejectionReason) {
        User user = new User(email, passwordEncoder.encode(password), Role.ROLE_STUDENT);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setFullName(fullName);
        student.setEnrollmentId(enrollmentId);
        student.setBranch(branch);
        student.setAcademicYear(academicYear);
        student.setCurrentYear(currentYear);
        student.setCurrentSemester(currentSemester);
        student.setSection(section);
        student.setPhoneNumber(phone);
        student.setApprovalStatus(status);
        student.setRejectionReason(rejectionReason);
        return studentRepository.save(student);
    }
}
