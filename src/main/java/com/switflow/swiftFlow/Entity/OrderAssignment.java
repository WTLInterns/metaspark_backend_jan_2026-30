package com.switflow.swiftFlow.Entity;

import com.switflow.swiftFlow.utility.Department;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "order_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private Department department;
    
    @Column(name = "assigned_at")
    private java.time.LocalDateTime assignedAt;
    
    @Column(name = "assigned_by")
    private String assignedBy;
    
    @PrePersist
    protected void onCreate() {
        assignedAt = java.time.LocalDateTime.now();
    }
}
