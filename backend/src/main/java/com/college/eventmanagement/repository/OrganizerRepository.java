package com.college.eventmanagement.repository;

import com.college.eventmanagement.entity.Organizer;
import com.college.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    Optional<Organizer> findByUser(User user);
    Optional<Organizer> findByUserId(Long userId);
    boolean existsByContactEmail(String contactEmail);
}
