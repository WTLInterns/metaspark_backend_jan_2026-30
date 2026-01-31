package com.switflow.swiftFlow.Request;

import lombok.Data;

@Data
public class InventoryOutwardRequest {

    private String customer;

    private Long materialId;

    private Integer quantity;

    private String remarkUnique;
}
