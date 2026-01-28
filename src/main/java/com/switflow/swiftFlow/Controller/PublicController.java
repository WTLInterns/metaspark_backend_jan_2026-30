package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Repo.UserRepository;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "http://localhost:3000")
public class PublicController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> getAvailableDepartments() {
        List<Map<String, Object>> departments = Arrays.stream(Department.values())
            .filter(dept -> dept != Department.ENQUIRY && dept != Department.COMPLETED) // Exclude non-login departments
            .map(dept -> {
                Map<String, Object> deptMap = Map.of(
                    "key", dept.name(),
                    "label", getDepartmentLabel(dept),
                    "description", getDepartmentDescription(dept),
                    "icon", getDepartmentIcon(dept)
                );
                return deptMap;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/login-roles")
    public ResponseEntity<List<Map<String, Object>>> getLoginRoles() {
        // Return predefined login roles that users can select from
        List<Map<String, Object>> loginRoles = Arrays.asList(
            Map.of(
                "id", 1,
                "fullName", "Administrator",
                "email", "admin@metaspark.com",
                "department", "ADMIN",
                "label", "Admin",
                "description", "System administrator with full access",
                "icon", "USER_COG"
            ),
            Map.of(
                "id", 2,
                "fullName", "Design Team",
                "email", "design@metaspark.com", 
                "department", "DESIGN",
                "label", "Designer",
                "description", "Design department user",
                "icon", "USER_CHECK"
            ),
            Map.of(
                "id", 3,
                "fullName", "Production Team",
                "email", "production@metaspark.com",
                "department", "PRODUCTION", 
                "label", "Production",
                "description", "Production department user",
                "icon", "USER"
            ),
            Map.of(
                "id", 4,
                "fullName", "Machining Team",
                "email", "machining@metaspark.com",
                "department", "MACHINING",
                "label", "Machinist", 
                "description", "Machining department user",
                "icon", "USER_COG"
            ),
            Map.of(
                "id", 5,
                "fullName", "Inspection Team",
                "email", "inspection@metaspark.com",
                "department", "INSPECTION",
                "label", "Inspector",
                "description", "Inspection department user", 
                "icon", "USER_CHECK"
            )
        );
        
        return ResponseEntity.ok(loginRoles);
    }

    @PostMapping("/fix-user-password/{email}")
    public ResponseEntity<?> fixUserPassword(@PathVariable String email) {
        try {
            User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Set password to phone number if phone number exists
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPhoneNumber()));
                userRepository.save(user);
                return ResponseEntity.ok(Map.of(
                    "message", "Password fixed successfully",
                    "email", email,
                    "password", user.getPhoneNumber()
                ));
            } else {
                return ResponseEntity.badRequest().body("User has no phone number to set as password");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    private String getDepartmentLabel(Department dept) {
        switch (dept) {
            case ADMIN: return "Admin";
            case DESIGN: return "Designer";
            case PRODUCTION: return "Production";
            case MACHINING: return "Machinist";
            case INSPECTION: return "Inspector";
            default: return dept.name();
        }
    }

    private String getDepartmentDescription(Department dept) {
        switch (dept) {
            case ADMIN: return "System administrator with full access";
            case DESIGN: return "Design department user";
            case PRODUCTION: return "Production department user";
            case MACHINING: return "Machining department user";
            case INSPECTION: return "Inspection department user";
            default: return dept.name() + " department";
        }
    }

    private String getDepartmentIcon(Department dept) {
        switch (dept) {
            case ADMIN: return "USER_COG";
            case DESIGN: return "USER_CHECK";
            case PRODUCTION: return "USER";
            case MACHINING: return "USER_COG";
            case INSPECTION: return "USER_CHECK";
            default: return "USER_CIRCLE";
        }
    }
}
