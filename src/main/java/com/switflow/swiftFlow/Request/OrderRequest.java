package com.switflow.swiftFlow.Request;

import com.switflow.swiftFlow.utility.Department;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    

    private String productDetails;

    private String customProductDetails;

    private String units;

    private String material;

    private MaterialDetails materialDetails;

    private ProcessDetails processDetails;

    private Department department;

    private Integer customerId;

    private Integer productId;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MaterialDetails {
        private String material;
        private String gas;
        private String thickness;
        private String type;
    }

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
