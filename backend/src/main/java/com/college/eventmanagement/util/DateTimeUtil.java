package com.college.eventmanagement.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Authoritative Campus Date & Time Utility for CEMS.
 * Ensures consistent timezone (Asia/Kolkata, UTC+05:30) evaluation across
 * cloud servers, local development, and Docker/PaaS deployments.
 */
public final class DateTimeUtil {

    public static final ZoneId CAMPUS_ZONE_ID = ZoneId.of("Asia/Kolkata");

    private DateTimeUtil() {}

    public static ZoneId getZoneId() {
        return CAMPUS_ZONE_ID;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(CAMPUS_ZONE_ID);
    }

    public static LocalDate today() {
        return LocalDate.now(CAMPUS_ZONE_ID);
    }

    public static LocalTime currentTime() {
        return LocalTime.now(CAMPUS_ZONE_ID);
    }
}
