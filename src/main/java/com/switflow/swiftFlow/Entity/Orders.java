package com.switflow.swiftFlow.Entity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.switflow.swiftFlow.utility.Department;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private String productDetails;

    private String customProductDetails;

    private String units;

    private String material;

    @ManyToMany(mappedBy = "orders")
    private List<Customer> customers;

    @Enumerated(EnumType.STRING)
    private Department department;

    @ManyToMany(mappedBy = "orders")
    private List<Product> products;
    
    private String status = "Active";

    private String dateAdded;

    @OneToMany(mappedBy = "orders")
    private List<Status> statuses;

    private Integer designProgress = 0;

    private Integer productionProgress = 0;

    private Integer machiningProgress = 0;

    private Integer inspectionProgress = 0;

    @PrePersist
    protected void onCreate() {
        if (dateAdded == null) {
            dateAdded = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }
    }
}