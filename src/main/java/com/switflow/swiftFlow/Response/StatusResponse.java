package com.switflow.swiftFlow.Response;

import com.switflow.swiftFlow.utility.Department;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusResponse {

    private int id;
    
    private Department oldStatus;

    private Department newStatus;

    private String comment;

    private String percentage;
    private String attachmentUrl;

    private long orderId;

    private String createdAt;
}