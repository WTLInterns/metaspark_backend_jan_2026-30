package com.switflow.swiftFlow.Response;

import java.util.List;

import com.switflow.swiftFlow.utility.Department;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long orderId;

    private String productDetails;

    private String customProductDetails;

    private String units;

    private String material;

    private MaterialDetails materialDetails;

    private ProcessDetails processDetails;

    private String status;

    private String dateAdded;

    private Department department; // Add department field

    private Integer designProgress;

    private Integer productionProgress;

    private Integer machiningProgress;

    private Integer inspectionProgress;

    // Lists of customers and products
    private List<CustomerInfo> customers;
    private List<ProductInfo> products;

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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerInfo {
        private int customerId;
        private String customerName;
        private String companyName;
        private String customerEmail;
        private String customerPhone;
        private String primaryAddress;

        private String billingAddress;

        private String shippingAddress;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductInfo {
        private int productId;
        private String productCode;
        private String productName;
    }

}