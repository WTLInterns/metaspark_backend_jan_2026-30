package com.switflow.swiftFlow.Entity;

import com.switflow.swiftFlow.utility.Department;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    
    private String password;
    
    private String fullName;
    
    @Column(unique = true)
    private String email;
    
    private String phoneNumber;
    
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    private Department department;
    
    private Integer machineId; // Foreign key to Machines table
}