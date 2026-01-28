package com.switflow.swiftFlow.Service;

import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Entity.Machines;
import com.switflow.swiftFlow.Repo.UserRepository;
import com.switflow.swiftFlow.Repo.MachineRepository;
import com.switflow.swiftFlow.Request.UserRegistrationRequest;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User createUser(UserRegistrationRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (request.getEmail() != null && !request.getEmail().isEmpty() && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDepartment(request.getRole());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEnabled(true);

        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, UserRegistrationRequest request) {
        Optional<User> existingUser = userRepository.findById(id);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Check if new username already exists (and it's not the same user)
            if (!user.getUsername().equals(request.getUsername()) && 
                userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            
            // Check if new email already exists (and it's not the same user)
            if (request.getEmail() != null && !request.getEmail().isEmpty() && 
                !request.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            
            user.setUsername(request.getUsername());
            user.setDepartment(request.getRole());
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPhoneNumber(request.getPhoneNumber());
            
            // Only update password if provided
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            
            return Optional.of(userRepository.save(user));
        }
        return Optional.empty();
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<User> getUsersByDepartment(Department department) {
        return userRepository.findByDepartment(department);
    }

    public boolean assignMachineToUser(Long userId, int machineId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Machines> machineOpt = machineRepository.findById(machineId);
        
        if (userOpt.isPresent() && machineOpt.isPresent()) {
            User user = userOpt.get();
            Machines machine = machineOpt.get();
            
            // For now, we'll just return true since the User entity doesn't have a machine field
            // In a real implementation, you might want to create a separate UserMachine mapping entity
            return true;
        }
        return false;
    }

    public List<Machines> getAllMachines() {
        return machineRepository.findAll();
    }
    
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
