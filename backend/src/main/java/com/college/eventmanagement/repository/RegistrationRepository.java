package com.college.eventmanagement.repository;

import com.college.eventmanagement.entity.Event;
import com.college.eventmanagement.entity.Registration;
import com.college.eventmanagement.entity.RegistrationStatus;
import com.college.eventmanagement.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByEventAndStudent(Event event, Student student);
    Optional<Registration> findByEventIdAndStudentId(Long eventId, Long studentId);
    boolean existsByEventAndStudent(Event event, Student student);
    boolean existsByEventIdAndStudentId(Long eventId, Long studentId);

    List<Registration> findByStudentOrderByRegisteredAtDesc(Student student);
    List<Registration> findByEvent(Event event);
    List<Registration> findByEventOrderByRegisteredAtDesc(Event event);
    List<Registration> findByEventIdOrderByRegisteredAtDesc(Long eventId);

    long countByEvent(Event event);
    long countByEventId(Long eventId);
    long countByStudentAndStatus(Student student, RegistrationStatus status);

    @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId AND " +
           "(:query IS NULL OR LOWER(r.student.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.student.enrollmentId) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.student.user.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:branch IS NULL OR r.student.branch = :branch) AND " +
           "(:status IS NULL OR r.status = :status) " +
           "ORDER BY r.registeredAt DESC")
    List<Registration> filterParticipants(
        @Param("eventId") Long eventId,
        @Param("query") String query,
        @Param("branch") String branch,
        @Param("status") RegistrationStatus status
    );
}
