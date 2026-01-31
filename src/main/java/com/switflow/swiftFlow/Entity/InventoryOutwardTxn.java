package com.switflow.swiftFlow.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_outward_txn", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "remarkUnique" })
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryOutwardTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateTime;

    private String customer;

    @ManyToOne
    @JoinColumn(name = "materialId", nullable = false)
    private InventoryMaterialMaster material;

    private Integer quantity;

    private String remarkUnique;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.dateTime == null) {
            this.dateTime = this.createdAt;
        }
    }
}
