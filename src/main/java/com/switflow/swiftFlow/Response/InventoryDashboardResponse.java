package com.switflow.swiftFlow.Response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDashboardResponse {

    private List<InwardEntry> latestInward;
    private List<OutwardEntry> latestOutward;
    private List<TotalInventoryEntry> totalInventory;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InwardEntry {
        private Long id;
        private LocalDateTime dateTime;
        private String supplier;
        private String materialName;
        private String sheetSize;
        private String thickness;
        private Integer quantity;
        private String remarkUnique;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OutwardEntry {
        private Long id;
        private LocalDateTime dateTime;
        private String customer;
        private String materialName;
        private String sheetSize;
        private String thickness;
        private Integer quantity;
        private String remarkUnique;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TotalInventoryEntry {
        private Long id;
        private String materialName;
        private String thickness;
        private String sheetSize;
        private Integer quantity;
        private String location;
        private String defaultSupplier;
    }
}
