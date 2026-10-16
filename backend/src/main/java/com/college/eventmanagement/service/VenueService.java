package com.college.eventmanagement.service;

import com.college.eventmanagement.dto.VenueRequest;
import com.college.eventmanagement.dto.VenueResponse;
import com.college.eventmanagement.entity.Venue;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.repository.VenueRepository;
import com.college.eventmanagement.util.GeoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> getActiveVenues() {
        return venueRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));
        return mapToResponse(venue);
    }

    @Transactional
    public VenueResponse createVenue(VenueRequest request) {
        if (venueRepository.existsByNameAndBuilding(request.getName().trim(), request.getBuilding().trim())) {
            throw new ConflictException("A venue named '" + request.getName() + "' already exists in building '" + request.getBuilding() + "'");
        }

        Venue venue = new Venue(
                request.getName().trim(),
                request.getBuilding().trim(),
                request.getRoomNumber().trim(),
                request.getCapacity(),
                request.getDescription()
        );

        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (!GeoUtil.isValidCoordinate(request.getLatitude(), request.getLongitude())) {
                throw new BadRequestException("Invalid venue coordinates. Latitude must be between -90 and 90, and longitude between -180 and 180.");
            }
            venue.setLatitude(request.getLatitude());
            venue.setLongitude(request.getLongitude());
        }

        return mapToResponse(venueRepository.save(venue));
    }

    @Transactional
    public VenueResponse updateVenue(Long id, VenueRequest request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));

        venue.setName(request.getName().trim());
        venue.setBuilding(request.getBuilding().trim());
        venue.setRoomNumber(request.getRoomNumber().trim());
        venue.setCapacity(request.getCapacity());
        venue.setDescription(request.getDescription());

        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (!GeoUtil.isValidCoordinate(request.getLatitude(), request.getLongitude())) {
                throw new BadRequestException("Invalid venue coordinates. Latitude must be between -90 and 90, and longitude between -180 and 180.");
            }
            venue.setLatitude(request.getLatitude());
            venue.setLongitude(request.getLongitude());
        }

        return mapToResponse(venueRepository.save(venue));
    }

    @Transactional
    public void deleteVenue(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));
        // Soft delete / deactivate
        venue.setActive(false);
        venueRepository.save(venue);
    }

    public VenueResponse mapToResponse(Venue venue) {
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getBuilding(),
                venue.getRoomNumber(),
                venue.getCapacity(),
                venue.getDescription(),
                venue.isActive(),
                venue.getLatitude(),
                venue.getLongitude()
        );
    }
}
