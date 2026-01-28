package com.switflow.swiftFlow.Response;

import java.util.List;
import com.switflow.swiftFlow.utility.Department;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignedOrderResponse {

    private Long orderId;
    private String productDetails;
    private String customProductDetails;
    private String units;
    private String material;
    private String status;
    private String dateAdded;
    private Department department;
    private String customerName;
    private String productName;

    public AssignedOrderResponse(Long orderId, String productDetails, String customProductDetails, 
                               String units, String material, String status, String dateAdded, 
                               Department department) {
        this.orderId = orderId;
        this.productDetails = productDetails;
        this.customProductDetails = customProductDetails;
        this.units = units;
        this.material = material;
        this.status = status;
        this.dateAdded = dateAdded;
        this.department = department;
    }
}
