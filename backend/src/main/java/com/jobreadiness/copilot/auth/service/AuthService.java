package com.jobreadiness.copilot.auth.service;

import com.jobreadiness.copilot.auth.dto.AuthResponse;
import com.jobreadiness.copilot.auth.dto.LoginRequest;
import com.jobreadiness.copilot.auth.dto.SignupRequest;
import com.jobreadiness.copilot.careerprofile.entity.CareerProfile;
import com.jobreadiness.copilot.careerprofile.repository.CareerProfileRepository;
import com.jobreadiness.copilot.common.exception.BadRequestException;
import com.jobreadiness.copilot.common.security.JwtTokenProvider;
import com.jobreadiness.copilot.user.entity.User;
import com.jobreadiness.copilot.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CareerProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            UserRepository userRepository,
            CareerProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .build();

        user = userRepository.save(user);

        CareerProfile profile = CareerProfile.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .educations(new ArrayList<>())
                .projects(new ArrayList<>())
                .experiences(new ArrayList<>())
                .certifications(new ArrayList<>())
                .skills(new ArrayList<>())
                .build();
        profileRepository.save(profile);

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }
}
