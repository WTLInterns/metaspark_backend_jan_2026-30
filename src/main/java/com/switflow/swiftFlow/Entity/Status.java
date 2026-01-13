package com.switflow.swiftFlow.Entity;

import com.switflow.swiftFlow.utility.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Status {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private Department oldStatus;

    @Enumerated(EnumType.STRING)
    private Department newStatus;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String comment;

    private String attachmentUrl;

    private String percentage;

    @ManyToOne
    private Orders orders;

    private String createdAt;
    

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }
    }
}