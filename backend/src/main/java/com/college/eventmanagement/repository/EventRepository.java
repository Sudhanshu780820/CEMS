package com.college.eventmanagement.repository;

import com.college.eventmanagement.entity.Event;
import com.college.eventmanagement.entity.EventStatus;
import com.college.eventmanagement.entity.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganizerOrderByEventDateDescStartTimeDesc(Organizer organizer);

    List<Event> findByStatusOrderByEventDateAscStartTimeAsc(EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.eventDate >= :today ORDER BY e.eventDate ASC, e.startTime ASC")
    List<Event> findUpcomingPublishedEvents(@Param("today") LocalDate today);

    @Query("SELECT e FROM Event e WHERE e.organizer = :organizer AND e.eventDate = :today")
    List<Event> findOrganizerTodayEvents(@Param("organizer") Organizer organizer, @Param("today") LocalDate today);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.venue.id = :venueId " +
           "AND e.eventDate = :eventDate " +
           "AND e.status NOT IN ('CANCELLED', 'DRAFT') " +
           "AND (:eventId IS NULL OR e.id != :eventId) " +
           "AND (e.startTime < :endTime AND e.endTime > :startTime)")
    long countVenueOverlaps(
        @Param("venueId") Long venueId,
        @Param("eventDate") LocalDate eventDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("eventId") Long eventId
    );

    @Modifying
    @Query("UPDATE Event e SET e.registeredCount = e.registeredCount + 1 WHERE e.id = :eventId AND e.registeredCount < e.maxCapacity")
    int incrementRegisteredCountIfAvailable(@Param("eventId") Long eventId);

    @Modifying
    @Query("UPDATE Event e SET e.registeredCount = e.registeredCount - 1 WHERE e.id = :eventId AND e.registeredCount > 0")
    int decrementRegisteredCount(@Param("eventId") Long eventId);

    @Query("SELECT e FROM Event e WHERE " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:category IS NULL OR e.category = :category) AND " +
           "(:startDate IS NULL OR e.eventDate >= :startDate) AND " +
           "(:endDate IS NULL OR e.eventDate <= :endDate) AND " +
           "(:query IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY e.eventDate ASC, e.startTime ASC")
    List<Event> searchEvents(
        @Param("status") EventStatus status,
        @Param("category") String category,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("query") String query
    );
}
