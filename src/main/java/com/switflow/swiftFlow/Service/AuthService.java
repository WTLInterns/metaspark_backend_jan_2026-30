package com.switflow.swiftFlow.Service;

import com.switflow.swiftFlow.Entity.*;
import com.switflow.swiftFlow.Exception.UserAlreadyExistsException;
import com.switflow.swiftFlow.Jwt.JwtUtil;
import com.switflow.swiftFlow.Repo.*;
import com.switflow.swiftFlow.Request.AuthRequest;
import com.switflow.swiftFlow.Request.UserRegistrationRequest;
import com.switflow.swiftFlow.Response.AuthResponse;
import com.switflow.swiftFlow.utility.Department;
import com.switflow.swiftFlow.utility.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRoleRepository adminRoleRepository;
    
    @Autowired
    private DesignRoleRepository designRoleRepository;
    
    @Autowired
    private ProductionRoleRepository productionRoleRepository;
    
    @Autowired
    private MechanicRoleRepository mechanicRoleRepository;
    
    @Autowired
    private InspectionRoleRepository inspectionRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse authenticate(AuthRequest authRequest) {
        try {
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }
            
            // Get user role
            Role role = getUserRole(user.getId());
            
            // Load user details with authorities from CustomUserDetailsService
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            
            final String token = jwtUtil.generateToken(userDetails);

            return new AuthResponse(token, user, role);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    public User registerUser(UserRegistrationRequest registrationRequest) {
        if (userRepository.existsByUsername(registrationRequest.getUsername())) {
            throw new UserAlreadyExistsException("User with username " + registrationRequest.getUsername() + " already exists");
        }

        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setFullName(registrationRequest.getFullName());
        user.setEmail(registrationRequest.getEmail());
        user.setPhoneNumber(registrationRequest.getPhoneNumber());
        user.setEnabled(true);
        user.setDepartment(registrationRequest.getRole());
        User savedUser = userRepository.save(user);

        if (registrationRequest.getRole() != null) {
            createRoleEntity(savedUser, registrationRequest.getRole().name());
        } else {
            DesignRole designRole = new DesignRole();
            designRole.setUser(savedUser);
            designRole.setSpecialization("General Design");
            designRole.setYearsOfExperience(0);
            designRoleRepository.save(designRole);
        }

        return savedUser;
    }

    public Role getUserRole(Long userId) {
        // First check if user has a department set directly
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getDepartment() != null) {
            try {
                return Role.valueOf(user.getDepartment().name());
            } catch (IllegalArgumentException e) {
                // If the department doesn't map to a Role enum value, fall back to checking role tables
            }
        }
        
        // Check for each role type in role-specific tables (fallback)
        if (adminRoleRepository.findByUserId(userId).isPresent()) {
            return Role.ADMIN;
        }
        if (designRoleRepository.findByUserId(userId).isPresent()) {
            return Role.DESIGN;
        }
        if (productionRoleRepository.findByUserId(userId).isPresent()) {
            return Role.PRODUCTION;
        }
        if (mechanicRoleRepository.findByUserId(userId).isPresent()) {
            return Role.MECHANIC;
        }
        if (inspectionRoleRepository.findByUserId(userId).isPresent()) {
            return Role.INSPECTION;
        }
        
        // Default to DESIGN if no role found
        return Role.DESIGN;
    }

    private void createRoleEntity(User user, String role) {
        switch (role.toUpperCase()) {
            case "ADMIN":
                AdminRole adminRole = new AdminRole();
                adminRole.setUser(user);
                adminRole.setDepartment("General");
                adminRole.setAccessLevel("Standard");
                adminRoleRepository.save(adminRole);
                break;
            case "DESIGN":
                DesignRole designRole = new DesignRole();
                designRole.setUser(user);
                designRole.setSpecialization("General Design");
                designRole.setYearsOfExperience(0);
                designRoleRepository.save(designRole);
                break;
            case "PRODUCTION":
                ProductionRole productionRole = new ProductionRole();
                productionRole.setUser(user);
                productionRole.setProductionLine("General");
                productionRole.setShift("Day");
                productionRoleRepository.save(productionRole);
                break;
            case "MECHANIC":
                MechanicRole mechanicRole = new MechanicRole();
                mechanicRole.setUser(user);
                mechanicRole.setSpecialization("General");
                mechanicRole.setCertificationLevel("Entry");
                mechanicRoleRepository.save(mechanicRole);
                break;
            case "INSPECTION":
                InspectionRole inspectionRole = new InspectionRole();
                inspectionRole.setUser(user);
                inspectionRole.setInspectionArea("General");
                inspectionRole.setCertification("Basic");
                inspectionRoleRepository.save(inspectionRole);
                break;
            default:
                DesignRole defaultDesignRole = new DesignRole();
                defaultDesignRole.setUser(user);
                defaultDesignRole.setSpecialization("General Design");
                defaultDesignRole.setYearsOfExperience(0);
                designRoleRepository.save(defaultDesignRole);
                break;
        }
    }
    
    public String getUsernameFromToken(String token) {
        return jwtUtil.extractUsername(token);
    }
    
    public boolean validateToken(String token, String username) {
        return jwtUtil.validateToken(token, userDetailsService.loadUserByUsername(username));
    }
    
    public String generateNewToken(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return jwtUtil.generateToken(userDetails);
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}