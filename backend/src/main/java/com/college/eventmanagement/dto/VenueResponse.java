package com.college.eventmanagement.dto;

public class VenueResponse {
    private Long id;
    private String name;
    private String building;
    private String roomNumber;
    private Integer capacity;
    private String description;
    private boolean isActive;

    public VenueResponse() {}

    public VenueResponse(Long id, String name, String building, String roomNumber, Integer capacity, String description, boolean isActive) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.description = description;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
