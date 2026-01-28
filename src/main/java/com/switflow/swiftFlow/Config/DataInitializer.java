package com.switflow.swiftFlow.Config;

import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Repo.UserRepository;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
    }

    private void initializeDefaultUsers() {
        // Check if users already exist
        if (userRepository.count() > 0) {
            return; // Users already initialized
        }

        // Create default users for each department
        User admin = createDefaultUser(
            "admin@metaspark.com",
            "admin123",
            Department.ADMIN,
            "System Administrator",
            "admin@metaspark.com",
            "9876543210"
        );

        User designer = createDefaultUser(
            "design@metaspark.com",
            "design123",
            Department.DESIGN,
            "Design Team Lead",
            "design@metaspark.com",
            "9876543211"
        );

        User production = createDefaultUser(
            "production@metaspark.com",
            "production123",
            Department.PRODUCTION,
            "Production Team Lead",
            "production@metaspark.com",
            "9876543212"
        );

        User machining = createDefaultUser(
            "machining@metaspark.com",
            "machining123",
            Department.MACHINING,
            "Machining Team Lead",
            "machining@metaspark.com",
            "9876543213"
        );

        User inspection = createDefaultUser(
            "inspection@metaspark.com",
            "inspection123",
            Department.INSPECTION,
            "Inspection Team Lead",
            "inspection@metaspark.com",
            "9876543214"
        );

        // Save all users
        userRepository.save(admin);
        userRepository.save(designer);
        userRepository.save(production);
        userRepository.save(machining);
        userRepository.save(inspection);

        System.out.println("Default users initialized successfully!");
        System.out.println("Admin Login: admin@metaspark.com / admin123");
        System.out.println("Designer Login: design@metaspark.com / design123");
        System.out.println("Production Login: production@metaspark.com / production123");
        System.out.println("Machining Login: machining@metaspark.com / machining123");
        System.out.println("Inspection Login: inspection@metaspark.com / inspection123");
    }

    private User createDefaultUser(String username, String password, Department department, 
                                 String fullName, String email, String phoneNumber) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setDepartment(department);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setEnabled(true);
        return user;
    }
}
