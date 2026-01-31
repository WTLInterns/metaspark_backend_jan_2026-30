package com.switflow.swiftFlow.Request;

import lombok.Data;

@Data
public class InventoryInwardRequest {

    private String supplier;

    private String materialName;

    private String thickness;

    private String sheetSize;

    private Integer quantity;

    private String location;

    private String remarkUnique;
}
