package com.college.eventmanagement.repository;

import com.college.eventmanagement.entity.Attendance;
import com.college.eventmanagement.entity.AttendanceStatus;
import com.college.eventmanagement.entity.Event;
import com.college.eventmanagement.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEventAndStudent(Event event, Student student);
    Optional<Attendance> findByEventIdAndStudentId(Long eventId, Long studentId);
    boolean existsByEventAndStudent(Event event, Student student);
    boolean existsByEventIdAndStudentId(Long eventId, Long studentId);

    List<Attendance> findByEvent(Event event);
    List<Attendance> findByEventId(Long eventId);
    List<Attendance> findByStudentOrderByCheckInTimeDesc(Student student);

    long countByEventAndStatus(Event event, AttendanceStatus status);
    long countByEventIdAndStatus(Long eventId, AttendanceStatus status);
    long countByStudentAndStatus(Student student, AttendanceStatus status);
}
