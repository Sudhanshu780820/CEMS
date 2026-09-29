package com.college.eventmanagement.repository;

import com.college.eventmanagement.entity.ApprovalStatus;
import com.college.eventmanagement.entity.Student;
import com.college.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser(User user);
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByEnrollmentId(String enrollmentId);
    boolean existsByEnrollmentId(String enrollmentId);
    List<Student> findByApprovalStatus(ApprovalStatus approvalStatus);

    long countByApprovalStatus(ApprovalStatus status);

    @Query("SELECT s FROM Student s WHERE " +
           "(:query IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.enrollmentId) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:branch IS NULL OR s.branch = :branch) AND " +
           "(:academicYear IS NULL OR s.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR s.currentYear = :currentYear) AND " +
           "(:section IS NULL OR s.section = :section) AND " +
           "(:status IS NULL OR s.approvalStatus = :status)")
    List<Student> searchStudents(
        @Param("query") String query,
        @Param("branch") String branch,
        @Param("academicYear") String academicYear,
        @Param("currentYear") String currentYear,
        @Param("section") String section,
        @Param("status") ApprovalStatus status
    );
}
