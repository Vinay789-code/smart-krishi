package com.smartkrishi.service;

import com.smartkrishi.config.JwtUtil;
import com.smartkrishi.dto.AuthRequest;
import com.smartkrishi.dto.AuthResponse;
import com.smartkrishi.dto.RegisterRequest;
import com.smartkrishi.entity.FarmerProfile;
import com.smartkrishi.entity.Role;
import com.smartkrishi.entity.User;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.exception.ResourceNotFoundException;
import com.smartkrishi.repository.FarmerProfileRepository;
import com.smartkrishi.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final FarmerProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       FarmerProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with email " + request.getEmail() + " already exists");
        }

        Role role = Role.ROLE_FARMER;
        if (request.getRole() != null && request.getRole().equalsIgnoreCase("ROLE_ADMIN")) {
            role = Role.ROLE_ADMIN;
        }

        User user = new User(
                request.getName(),
                request.getEmail().toLowerCase().trim(),
                request.getPhone(),
                passwordEncoder.encode(request.getPassword()),
                role
        );
        User savedUser = userRepository.save(user);

        FarmerProfile profile = new FarmerProfile();
        profile.setUser(savedUser);
        profile.setVillage(request.getVillage());
        profile.setDistrict(request.getDistrict());
        profile.setState(request.getState());
        profile.setLandArea(request.getLandArea() != null ? request.getLandArea() : 0.0);
        profile.setSoilType(request.getSoilType() != null ? request.getSoilType() : "Alluvial");
        profile.setIrrigationType(request.getIrrigationType() != null ? request.getIrrigationType() : "Tube Well");
        profile.setPrimaryCrop(request.getPrimaryCrop());
        profileRepository.save(profile);

        String token = jwtUtil.generateToken(savedUser);

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole().name(),
                profile.getVillage(),
                profile.getDistrict(),
                profile.getState(),
                profile.getLandArea(),
                profile.getSoilType()
        );
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user);

        FarmerProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                profile != null ? profile.getVillage() : null,
                profile != null ? profile.getDistrict() : null,
                profile != null ? profile.getState() : null,
                profile != null ? profile.getLandArea() : 0.0,
                profile != null ? profile.getSoilType() : null
        );
    }
}
