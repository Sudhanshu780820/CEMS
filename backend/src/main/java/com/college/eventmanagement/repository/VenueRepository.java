package com.college.eventmanagement.repository;

import com.college.eventmanagement.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByIsActiveTrue();
    boolean existsByNameAndBuilding(String name, String building);
}
