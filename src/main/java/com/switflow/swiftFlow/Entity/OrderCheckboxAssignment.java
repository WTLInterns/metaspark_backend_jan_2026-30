package com.switflow.swiftFlow.Entity;

import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_checkbox_assignment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_assign_order_pdf_scope_row", columnNames = {"order_id", "pdf_type", "scope", "row_key"})
        },
        indexes = {
                @Index(name = "idx_assign_assignee", columnList = "assigned_to_user_id"),
                @Index(name = "idx_assign_order_pdf_scope", columnList = "order_id,pdf_type,scope")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCheckboxAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pdf_type", nullable = false, length = 10)
    private PdfType pdfType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 50)
    private SelectionScope scope;

    @Column(name = "row_key", nullable = false, length = 255)
    private String rowKey;

    @Column(name = "assigned_to_user_id", nullable = false)
    private Long assignedToUserId;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
