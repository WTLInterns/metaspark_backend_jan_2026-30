package com.switflow.swiftFlow.Request;

import com.switflow.swiftFlow.utility.Department;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusRequest {
    
    private Department oldStatus;

    private Department newStatus;

    private String comment;

    private String attachmentUrl;

    private String createdAt;

    private String percentage;
}