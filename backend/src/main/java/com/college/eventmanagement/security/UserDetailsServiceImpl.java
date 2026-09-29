package com.college.eventmanagement.security;

import com.college.eventmanagement.entity.Student;
import com.college.eventmanagement.entity.User;
import com.college.eventmanagement.repository.StudentRepository;
import com.college.eventmanagement.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEnrollmentId) throws UsernameNotFoundException {
        // Try finding by email first
        Optional<User> userOpt = userRepository.findByEmail(usernameOrEnrollmentId);

        // If not found, try finding student by enrollment ID
        if (userOpt.isEmpty()) {
            Optional<Student> studentOpt = studentRepository.findByEnrollmentId(usernameOrEnrollmentId);
            if (studentOpt.isPresent()) {
                userOpt = Optional.of(studentOpt.get().getUser());
            }
        }

        User user = userOpt.orElseThrow(() ->
                new UsernameNotFoundException("User not found with email or enrollment ID: " + usernameOrEnrollmentId));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
