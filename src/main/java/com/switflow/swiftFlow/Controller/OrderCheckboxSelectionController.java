package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Request.DesignerBaseSelectionRequest;
import com.switflow.swiftFlow.Request.ProductionAssignmentsRequest;
import com.switflow.swiftFlow.Response.MyAssignmentsResponse;
import com.switflow.swiftFlow.Response.ProductionAssignmentsResponse;
import com.switflow.swiftFlow.Service.OrderCheckboxSelectionService;
import com.switflow.swiftFlow.Service.UserService;
import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderCheckboxSelectionController {

    @Autowired
    private OrderCheckboxSelectionService orderCheckboxSelectionService;

    @Autowired
    private UserService userService;

    @PostMapping("/{orderId}/designer-base-selections")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN')")
    public ResponseEntity<?> saveDesignerBaseSelections(
            @PathVariable Long orderId,
            @RequestBody DesignerBaseSelectionRequest request
    ) {
        try {
            if (request == null || request.getPdfType() == null || request.getScope() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "pdfType and scope are required"));
            }

            User currentUser = getCurrentUser();
            orderCheckboxSelectionService.saveDesignerBaseSelection(orderId, request.getPdfType(), request.getScope(), request.getRowKeys(), currentUser);
            return ResponseEntity.ok(Map.of("message", "Designer base selection saved"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to save designer base selection: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/designer-base-selections")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<?> getDesignerBaseSelections(
            @PathVariable Long orderId,
            @RequestParam PdfType pdfType,
            @RequestParam SelectionScope scope
    ) {
        try {
            List<String> rowKeys = orderCheckboxSelectionService.getDesignerBaseSelection(orderId, pdfType, scope);
            return ResponseEntity.ok(Map.of(
                    "pdfType", pdfType,
                    "scope", scope,
                    "rowKeys", rowKeys
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to fetch designer base selection: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/production-assignments")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION')")
    public ResponseEntity<?> assignProductionRows(
            @PathVariable Long orderId,
            @RequestBody ProductionAssignmentsRequest request
    ) {
        try {
            if (request == null || request.getPdfType() == null || request.getScope() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "pdfType and scope are required"));
            }

            User currentUser = getCurrentUser();
            orderCheckboxSelectionService.assignRowsToEmployees(orderId, request.getPdfType(), request.getScope(), request.getAssignments(), currentUser);
            return ResponseEntity.ok(Map.of("message", "Rows assigned successfully"));
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation -> conflict
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "One or more rowKeys are already assigned",
                    "status", 409
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to assign rows: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/production-assignments")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION')")
    public ResponseEntity<?> getProductionAssignments(
            @PathVariable Long orderId,
            @RequestParam PdfType pdfType,
            @RequestParam SelectionScope scope
    ) {
        try {
            ProductionAssignmentsResponse resp = orderCheckboxSelectionService.getProductionAssignments(orderId, pdfType, scope);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to fetch assignments: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/my-assignments")
    @PreAuthorize("hasAnyRole('ADMIN','MACHINING')")
    public ResponseEntity<?> getMyAssignments(
            @PathVariable Long orderId,
            @RequestParam PdfType pdfType,
            @RequestParam SelectionScope scope
    ) {
        try {
            User currentUser = getCurrentUser();
            MyAssignmentsResponse resp = orderCheckboxSelectionService.getMyAssignments(orderId, pdfType, scope, currentUser);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to fetch my assignments: " + e.getMessage()));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return null;
        }
        return userService.findByUsername(username);
    }
}
