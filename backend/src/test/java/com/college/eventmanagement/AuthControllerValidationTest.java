package com.college.eventmanagement;

import com.college.eventmanagement.controller.AuthController;
import com.college.eventmanagement.dto.StudentRegisterRequest;
import com.college.eventmanagement.exception.GlobalExceptionHandler;
import com.college.eventmanagement.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerValidationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // AuthService mock/stub is not even called for invalid @Valid payloads!
        AuthController controller = new AuthController(null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Direct API bypass with invalid password returns HTTP 400 with clean error payload")
    void testDirectApiInvalidPasswordReturns400() throws Exception {
        StudentRegisterRequest req = new StudentRegisterRequest();
        req.setFullName("Test Student");
        req.setEmail("test@college.edu");
        req.setPassword("12345"); // invalid: no letters, < 6 chars
        req.setEnrollmentId("EN2026TEST");
        req.setBranch("CSE");
        req.setAcademicYear("2026-2030");
        req.setCurrentYear("1st Year");
        req.setCurrentSemester("Semester 1");
        req.setSection("A");
        req.setPhoneNumber("9876543210");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Password must contain at least one letter and one number."))
                .andExpect(jsonPath("$.details.password").value("Password must contain at least one letter and one number."));
    }

    @Test
    @DisplayName("Direct API bypass with invalid phone number returns HTTP 400 with clean error payload")
    void testDirectApiInvalidPhoneReturns400() throws Exception {
        StudentRegisterRequest req = new StudentRegisterRequest();
        req.setFullName("Test Student");
        req.setEmail("test@college.edu");
        req.setPassword("ValidPass1");
        req.setEnrollmentId("EN2026TEST");
        req.setBranch("CSE");
        req.setAcademicYear("2026-2030");
        req.setCurrentYear("1st Year");
        req.setCurrentSemester("Semester 1");
        req.setSection("A");
        req.setPhoneNumber("123"); // invalid phone

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Phone number must contain exactly 10 digits."))
                .andExpect(jsonPath("$.details.phoneNumber").value("Phone number must contain exactly 10 digits."));
    }

    @Test
    @DisplayName("Direct API bypass with missing required fields returns HTTP 400 with field details")
    void testDirectApiMissingFieldsReturns400() throws Exception {
        StudentRegisterRequest req = new StudentRegisterRequest();
        // Empty body

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    @DisplayName("Direct API bypass with invalid academic year format returns HTTP 400")
    void testDirectApiInvalidAcademicYearFormatReturns400() throws Exception {
        StudentRegisterRequest req = new StudentRegisterRequest();
        req.setFullName("Test Student");
        req.setEmail("test@college.edu");
        req.setPassword("ValidPass1");
        req.setEnrollmentId("EN2026TEST");
        req.setBranch("CSE");
        req.setAcademicYear("2026"); // invalid: not YYYY-YYYY
        req.setCurrentYear("1st Year");
        req.setCurrentSemester("Semester 1");
        req.setSection("A");
        req.setPhoneNumber("9876543210");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.academicYear").value("Academic session must be in format YYYY-YYYY."));
    }
}
