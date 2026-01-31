package com.switflow.swiftFlow.Controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.switflow.swiftFlow.Request.InventoryInwardRequest;
import com.switflow.swiftFlow.Request.InventoryOutwardRequest;
import com.switflow.swiftFlow.Response.InventoryDashboardResponse;
import com.switflow.swiftFlow.Response.InventoryDashboardResponse.TotalInventoryEntry;
import com.switflow.swiftFlow.Service.InventoryService;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryDashboardResponse> getDashboard() {
        InventoryDashboardResponse response = inventoryService.getDashboard();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/inward/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateInward(@PathVariable Long id, @RequestBody InventoryInwardRequest request) {
        try {
            inventoryService.updateInward(id, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/inward/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteInward(@PathVariable Long id) {
        try {
            inventoryService.deleteInward(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
            }
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/inward")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createInward(@RequestBody InventoryInwardRequest request) {
        try {
            inventoryService.createInward(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/outward/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOutward(@PathVariable Long id, @RequestBody InventoryOutwardRequest request) {
        try {
            inventoryService.updateOutward(id, request);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Insufficient stock");
        } catch (IllegalArgumentException e) {
            if ("Material not found".equalsIgnoreCase(e.getMessage())
                    || e.getMessage().toLowerCase().contains("not found")) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
            }
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/outward/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteOutward(@PathVariable Long id) {
        try {
            inventoryService.deleteOutward(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
            }
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/outward")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createOutward(@RequestBody InventoryOutwardRequest request) {
        try {
            inventoryService.createOutward(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalStateException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Insufficient stock");
        } catch (IllegalArgumentException e) {
            if ("Material not found".equalsIgnoreCase(e.getMessage())) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
            }
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadReport(@RequestParam(name = "type", defaultValue = "csv") String type,
            HttpServletResponse response) throws IOException {

        InventoryDashboardResponse dashboard = inventoryService.getDashboard();

        if ("excel".equalsIgnoreCase(type)) {
            String filename = "inventory-report.xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");

            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Inventory Report");

                // Header row
                Row header = sheet.createRow(0);
                String[] headers = new String[] { "Material", "Thickness", "Sheet Size", "Quantity", "Location",
                        "Default Supplier" };
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(headers[i]);
                }

                int rowIdx = 1;
                for (TotalInventoryEntry entry : dashboard.getTotalInventory()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(safe(entry.getMaterialName()));
                    row.createCell(1).setCellValue(safe(entry.getThickness()));
                    row.createCell(2).setCellValue(safe(entry.getSheetSize()));
                    row.createCell(3).setCellValue(entry.getQuantity() == null ? 0 : entry.getQuantity());
                    row.createCell(4).setCellValue(safe(entry.getLocation()));
                    row.createCell(5).setCellValue(safe(entry.getDefaultSupplier()));
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(baos);
                baos.writeTo(response.getOutputStream());
                response.flushBuffer();
            }
        } else {
            String filename = "inventory-report.csv";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");

            try (PrintWriter writer = response.getWriter()) {
                writer.println("Material,Thickness,Sheet Size,Quantity,Location,Default Supplier");
                for (TotalInventoryEntry entry : dashboard.getTotalInventory()) {
                    writer.printf("%s,%s,%s,%d,%s,%s%n",
                            safe(entry.getMaterialName()),
                            safe(entry.getThickness()),
                            safe(entry.getSheetSize()),
                            entry.getQuantity() == null ? 0 : entry.getQuantity(),
                            safe(entry.getLocation()),
                            safe(entry.getDefaultSupplier()));
                }
                writer.flush();
            }
        }
    }

    @GetMapping("/remark")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getRemark(@RequestParam("type") String type) {
        String remark = inventoryService.generateNewRemark(type);
        Map<String, String> body = new HashMap<>();
        body.put("remarkUnique", remark);
        return ResponseEntity.ok(body);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
