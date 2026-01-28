package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Entity.Machines;
import com.switflow.swiftFlow.Entity.OrderAssignment;
import com.switflow.swiftFlow.Request.UserRegistrationRequest;
import com.switflow.swiftFlow.Response.MessageResponse;
import com.switflow.swiftFlow.Response.MachinesResponse;
import com.switflow.swiftFlow.Response.AssignedOrderResponse;
import com.switflow.swiftFlow.Service.UserService;
import com.switflow.swiftFlow.Service.EmailService;
import com.switflow.swiftFlow.Service.MachinesService;
import com.switflow.swiftFlow.Service.OrderService;
import com.switflow.swiftFlow.Repo.OrderAssignmentRepository;
import com.switflow.swiftFlow.Repo.OrderRepository;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private MachinesService machinesService;
    
    @Autowired
    private OrderAssignmentRepository orderAssignmentRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            Optional<User> user = userService.getUserById(id);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/by-department/{department}")
    public ResponseEntity<List<User>> getUsersByDepartment(@PathVariable String department) {
        try {
            // Convert string to Department enum
            Department deptEnum = Department.valueOf(department.toUpperCase());
            List<User> users = userService.getUsersByDepartment(deptEnum);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/assigned-to-order/{orderId}/{department}")
    public ResponseEntity<User> getAssignedUserForOrderAndDepartment(@PathVariable Long orderId, @PathVariable String department) {
        try {
            // Convert string to Department enum
            Department deptEnum = Department.valueOf(department.toUpperCase());
            
            // Find the assignment for this order and department
            Optional<OrderAssignment> assignment = orderAssignmentRepository.findByOrderIdAndDepartment(orderId, deptEnum);
            
            if (assignment.isPresent()) {
                return ResponseEntity.ok(assignment.get().getUser());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserRegistrationRequest registrationRequest) {
        try {
            // Generate phone number if not provided
            if (registrationRequest.getPhoneNumber() == null || registrationRequest.getPhoneNumber().trim().isEmpty()) {
                String generatedPhone = generatePhoneNumber();
                registrationRequest.setPhoneNumber(generatedPhone);
            }

            // Set password to phone number if not provided
            if (registrationRequest.getPassword() == null || registrationRequest.getPassword().trim().isEmpty()) {
                registrationRequest.setPassword(registrationRequest.getPhoneNumber());
            }

            User createdUser = userService.createUser(registrationRequest);
            
            // Send login details email asynchronously
            try {
                emailService.sendLoginDetails(
                    createdUser.getEmail(),
                    createdUser.getFullName(),
                    registrationRequest.getPhoneNumber(),
                    createdUser.getDepartment().toString()
                );
                
                // Send notification to admin (you might want to get admin email from config or database)
                emailService.sendUserCreationNotification(
                    "admin@metaspark.com", // This should be configured properly
                    createdUser.getFullName(),
                    createdUser.getEmail(),
                    createdUser.getDepartment().toString()
                );
            } catch (Exception emailException) {
                // Log email error but don't fail the user creation
                System.err.println("Failed to send login details email: " + emailException.getMessage());
                emailException.printStackTrace();
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{userId}/send-login-details")
    public ResponseEntity<?> sendLoginDetails(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    emailService.sendLoginDetails(
                        user.getEmail(),
                        user.getFullName(),
                        user.getUsername(),
                        "Your current password (contact admin if forgotten)"
                    );
                    return ResponseEntity.ok(new MessageResponse("Login details sent successfully"));
                } else {
                    return ResponseEntity.badRequest().body(new MessageResponse("User email not found"));
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRegistrationRequest request) {
        try {
            Optional<User> updatedUser = userService.updateUser(id, request);
            return updatedUser.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/assign-to-order")
    @Transactional
    public ResponseEntity<?> assignUserToOrder(@RequestBody Map<String, Object> assignmentRequest) {
        try {
            Long userId = Long.valueOf(assignmentRequest.get("userId").toString());
            Long orderId = Long.valueOf(assignmentRequest.get("orderId").toString());
            String department = assignmentRequest.get("department").toString();
            
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(new MessageResponse("User not found"));
            }
            
            User user = userOpt.get();
            
            // Remove any existing assignment for this user and order
            orderAssignmentRepository.deleteByUserIdAndOrderId(userId, orderId);
            
            // Create new assignment
            OrderAssignment assignment = new OrderAssignment();
            assignment.setUser(user);
            assignment.setOrderId(orderId);
            assignment.setDepartment(Department.valueOf(department));
            assignment.setAssignedBy("Admin"); // You can get this from the authenticated user
            
            orderAssignmentRepository.save(assignment);
            
            // Update order department
            com.switflow.swiftFlow.Entity.Orders order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                order.setDepartment(Department.valueOf(department));
                orderRepository.save(order);
            }
            
            return ResponseEntity.ok(new MessageResponse(
                String.format("Assigned %s to %s department for order %d", 
                    user.getFullName(), department, orderId)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/assigned-orders/{userId}")
    public ResponseEntity<List<Long>> getAssignedOrders(@PathVariable Long userId) {
        try {
            System.out.println("🔍 [BACKEND] Fetching assigned orders for userId: " + userId);
            List<Long> orderIds = orderAssignmentRepository.findOrderIdsByUserId(userId);
            System.out.println("🔍 [BACKEND] Found order IDs for userId " + userId + ": " + orderIds);
            return ResponseEntity.ok(orderIds);
        } catch (Exception e) {
            System.err.println("❌ [BACKEND] Error fetching assigned orders for userId " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/assigned-orders-details")
    public ResponseEntity<List<AssignedOrderResponse>> getAssignedOrdersDetails(HttpServletRequest request) {
        try {
            // Get current user from token
            String username = request.getUserPrincipal().getName();
            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Long> orderIds = orderAssignmentRepository.findOrderIdsByUserId(currentUser.getId());
            List<AssignedOrderResponse> assignedOrders = new ArrayList<>();
            
            for (Long orderId : orderIds) {
                try {
                    com.switflow.swiftFlow.Entity.Orders order = orderRepository.findById(orderId).orElse(null);
                    if (order != null) {
                        // Create a simple DTO to avoid circular references
                        AssignedOrderResponse orderResponse = new AssignedOrderResponse();
                        orderResponse.setOrderId(order.getOrderId());
                        orderResponse.setProductDetails(order.getProductDetails());
                        orderResponse.setCustomProductDetails(order.getCustomProductDetails());
                        orderResponse.setUnits(order.getUnits());
                        orderResponse.setMaterial(order.getMaterial());
                        orderResponse.setStatus(order.getStatus());
                        orderResponse.setDateAdded(order.getDateAdded());
                        orderResponse.setDepartment(order.getDepartment());
                        
                        assignedOrders.add(orderResponse);
                    }
                } catch (Exception e) {
                    // Skip orders that can't be found
                    continue;
                }
            }
            
            return ResponseEntity.ok(assignedOrders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{userId}/assign-machine/{machineId}")
    public ResponseEntity<?> assignMachineToUser(@PathVariable Long userId, @PathVariable int machineId) {
        try {
            boolean assigned = userService.assignMachineToUser(userId, machineId);
            if (assigned) {
                return ResponseEntity.ok(new MessageResponse("Machine assigned successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/machines")
    public ResponseEntity<List<Machines>> getAllMachines() {
        try {
            List<Machines> machines = userService.getAllMachines();
            return ResponseEntity.ok(machines);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/assign-machine/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> assignMachineToUser(@PathVariable Long userId, @RequestBody Map<String, Integer> request) {
        try {
            Integer machineId = request.get("machineId");
            if (machineId == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Machine ID is required"));
            }
            
            User user = userService.getUserById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify machine exists
            MachinesResponse machineResponse = machinesService.getMachines(machineId);
            if (machineResponse == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Machine not found"));
            }
            
            user.setMachineId(machineId);
            userService.updateUser(user);
            
            return ResponseEntity.ok(new MessageResponse("Machine assigned successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/with-machine-details")
    public ResponseEntity<List<Map<String, Object>>> getUsersWithMachineDetails() {
        try {
            List<User> users = userService.getAllUsers();
            List<Map<String, Object>> usersWithMachines = users.stream().map(user -> {
                Map<String, Object> userMap = new java.util.HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("fullName", user.getFullName());
                userMap.put("email", user.getEmail());
                userMap.put("department", user.getDepartment());
                userMap.put("enabled", user.isEnabled());
                
                // Add machine details if user has a machine assigned
                if (user.getMachineId() != null) {
                    MachinesResponse machine = machinesService.getMachines(user.getMachineId());
                    if (machine != null) {
                        userMap.put("machineName", machine.getMachineName());
                        userMap.put("machineStatus", machine.getStatus());
                    }
                }
                
                return userMap;
            }).collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(usersWithMachines);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generatePhoneNumber() {
        // Generate a random 10-digit phone number
        StringBuilder phone = new StringBuilder();
        phone.append("9"); // Start with 9 for mobile numbers
        for (int i = 0; i < 9; i++) {
            phone.append((int) (Math.random() * 10));
        }
        return phone.toString();
    }
}
