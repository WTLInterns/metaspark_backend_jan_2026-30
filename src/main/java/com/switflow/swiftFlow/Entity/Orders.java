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

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "material", column = @Column(name = "material_details_material")),
            @AttributeOverride(name = "gas", column = @Column(name = "material_details_gas")),
            @AttributeOverride(name = "thickness", column = @Column(name = "material_details_thickness")),
            @AttributeOverride(name = "type", column = @Column(name = "material_details_type"))
    })
    private MaterialDetails materialDetails;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "laserCutting", column = @Column(name = "process_details_laser_cutting")),
            @AttributeOverride(name = "bending", column = @Column(name = "process_details_bending")),
            @AttributeOverride(name = "fabrication", column = @Column(name = "process_details_fabrication")),
            @AttributeOverride(name = "powderCoating", column = @Column(name = "process_details_powder_coating"))
    })
    private ProcessDetails processDetails;

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

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MaterialDetails {
        private String material;
        private String gas;
        private String thickness;
        private String type;
    }

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessDetails {
        private Boolean laserCutting;
        private Boolean bending;
        private Boolean fabrication;
        private Boolean powderCoating;
    }
}