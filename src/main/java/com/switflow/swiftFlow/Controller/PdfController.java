package com.switflow.swiftFlow.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switflow.swiftFlow.Entity.Status;
import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Response.MachinesResponse;
import com.switflow.swiftFlow.Response.ProductionAssignmentsResponse;
import com.switflow.swiftFlow.Service.MachinesService;
import com.switflow.swiftFlow.Service.OrderCheckboxSelectionService;
import com.switflow.swiftFlow.Service.PdfService;
import com.switflow.swiftFlow.Service.StatusService;
import com.switflow.swiftFlow.Service.UserService;
import com.switflow.swiftFlow.Repo.StatusRepository;
import com.switflow.swiftFlow.Request.StatusRequest;
import com.switflow.swiftFlow.Repo.OrderAssignmentRepository;
import com.switflow.swiftFlow.pdf.PdfRow;
import com.switflow.swiftFlow.utility.Department;
import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private MachinesService machinesService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderAssignmentRepository orderAssignmentRepository;

    @Autowired
    private OrderCheckboxSelectionService orderCheckboxSelectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class RowSelectionRequest {
        private List<String> selectedRowIds;
        private Long machineId;
        private String attachmentUrl;

        public List<String> getSelectedRowIds() {
            return selectedRowIds;
        }
        public void setSelectedRowIds(List<String> selectedRowIds) {
            this.selectedRowIds = selectedRowIds;
        }

        public Long getMachineId() {
            return machineId;
        }

        public void setMachineId(Long machineId) {
            this.machineId = machineId;
        }

        public String getAttachmentUrl() {
            return attachmentUrl;
        }

        public void setAttachmentUrl(String attachmentUrl) {
            this.attachmentUrl = attachmentUrl;
        }
    }

    public static class ThreeCheckboxRequest {
        private List<String> designerSelectedRowIds;
        private List<String> productionSelectedRowIds;
        private List<String> machineSelectedRowIds;
        private List<String> inspectionSelectedRowIds;
        private Long machineId;
        private List<Map<String, Object>> selectedItems;
        private Long assignedUserId; // NEW: For specifying which employee this selection belongs to
        private String pdfType;      // NEW: Explicit PDF flow, e.g. PDF1 or PDF2
        private String scope;        // NEW: Explicit scope, e.g. SUBNEST, PARTS, MATERIAL, NESTING_RESULTS

        public List<String> getDesignerSelectedRowIds() {
            return designerSelectedRowIds;
        }

        public void setDesignerSelectedRowIds(List<String> designerSelectedRowIds) {
            this.designerSelectedRowIds = designerSelectedRowIds;
        }

        public List<String> getProductionSelectedRowIds() {
            return productionSelectedRowIds;
        }

        public void setProductionSelectedRowIds(List<String> productionSelectedRowIds) {
            this.productionSelectedRowIds = productionSelectedRowIds;
        }

        public List<String> getMachineSelectedRowIds() {
            return machineSelectedRowIds;
        }

        public void setMachineSelectedRowIds(List<String> machineSelectedRowIds) {
            this.machineSelectedRowIds = machineSelectedRowIds;
        }

        public List<String> getInspectionSelectedRowIds() {
            return inspectionSelectedRowIds;
        }

        public void setInspectionSelectedRowIds(List<String> inspectionSelectedRowIds) {
            this.inspectionSelectedRowIds = inspectionSelectedRowIds;
        }

        public Long getMachineId() {
            return machineId;
        }

        public void setMachineId(Long machineId) {
            this.machineId = machineId;
        }

        public List<Map<String, Object>> getSelectedItems() {
            return selectedItems;
        }

        public void setSelectedItems(List<Map<String, Object>> selectedItems) {
            this.selectedItems = selectedItems;
        }

        public Long getAssignedUserId() {
            return assignedUserId;
        }

        public void setAssignedUserId(Long assignedUserId) {
            this.assignedUserId = assignedUserId;
        }

        public String getPdfType() {
            return pdfType;
        }

        public void setPdfType(String pdfType) {
            this.pdfType = pdfType;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    @PostMapping("/order/{orderId}/machine-send-to-inspection")
    @PreAuthorize("hasAnyRole('ADMIN','MACHINING')")
    @Transactional
    public ResponseEntity<?> machineSendToInspection(@PathVariable long orderId) {
        try {
            List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);
            if (statuses == null || statuses.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No status history found for this order",
                        "status", 400
                ));
            }

            // Scope-wise merge of all MACHINING threeCheckbox entries
            Map<String, java.util.LinkedHashSet<String>> scopeMap = new LinkedHashMap<>();

            for (Status s : statuses) {
                if (s == null || s.getNewStatus() != Department.MACHINING) continue;
                String comment = s.getComment();
                if (comment == null || comment.isBlank()) continue;
                if (!comment.contains("threeCheckbox") || !comment.contains("selectedRowIds")) continue;
                try {
                    JsonNode node = objectMapper.readTree(comment.trim());

                    // Prefer explicit pdfType/scope from the comment when available
                    String explicitScopeKey = null;
                    JsonNode pdfTypeNode = node.get("pdfType");
                    JsonNode scopeNode = node.get("scope");
                    if (pdfTypeNode != null && !pdfTypeNode.isNull() && scopeNode != null && !scopeNode.isNull()) {
                        String pdfType = pdfTypeNode.asText("");
                        String scope = scopeNode.asText("");
                        if (!pdfType.isBlank() && !scope.isBlank()) {
                            explicitScopeKey = mapPdfTypeAndScopeToKey(pdfType, scope);
                        }
                    }

                    List<String> ids = extractStringList(node.get("selectedRowIds"));
                    for (String id : ids) {
                        if (id == null || id.isBlank()) continue;

                        String scopeKey;
                        if (explicitScopeKey != null) {
                            // When we have pdfType/scope, all row IDs from this status belong to that logical scope
                            scopeKey = explicitScopeKey;
                        } else {
                            // Legacy fallback: infer from the row ID prefix (nesting) or treat as PDF1_SUBNEST
                            scopeKey = classifyScopeKey(id);
                        }

                        if (scopeKey == null || scopeKey.isBlank()) continue;
                        scopeMap.computeIfAbsent(scopeKey, k -> new java.util.LinkedHashSet<>()).add(id);
                    }
                } catch (Exception ignored) {
                    // Skip malformed JSON comments
                }
            }

            boolean hasAny = scopeMap.values().stream().anyMatch(set -> set != null && !set.isEmpty());
            if (!hasAny) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No machine selections found to merge for this order",
                        "status", 400
                ));
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("threeCheckbox", true);
            payload.put("source", "MACHINING_MERGE");

            Map<String, Object> scopes = new LinkedHashMap<>();
            for (Map.Entry<String, java.util.LinkedHashSet<String>> e : scopeMap.entrySet()) {
                scopes.put(e.getKey(), new java.util.ArrayList<>(e.getValue()));
            }
            payload.put("scopes", scopes);

            String mergedComment;
            try {
                mergedComment = objectMapper.writeValueAsString(payload);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of(
                        "message", "Failed to serialize merged selection: " + e.getMessage(),
                        "status", 500
                ));
            }

            StatusRequest statusRequest = new StatusRequest();
            statusRequest.setNewStatus(Department.INSPECTION);
            statusRequest.setComment(mergedComment);
            statusRequest.setPercentage(null);
            statusRequest.setAttachmentUrl(null);

            statusService.createStatus(statusRequest, orderId);

            // Remove all MACHINING assignments for this order so mechanists no longer see it
            orderAssignmentRepository.deleteByOrderIdAndDepartment(orderId, Department.MACHINING);

            int mergedCount = scopeMap.values().stream().mapToInt(set -> set != null ? set.size() : 0).sum();

            return ResponseEntity.ok(Map.of(
                    "message", "Sent to Inspection with merged machine selections",
                    "mergedCount", mergedCount
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", 400
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to send to Inspection with merged selections: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @PostMapping("/order/{orderId}/inspection-selection")
    @PreAuthorize("hasAnyRole('ADMIN','MACHINING')")
    public ResponseEntity<?> saveInspectionSelection(
            @PathVariable long orderId,
            @RequestBody RowSelectionRequest request
    ) {
        try {
            StatusRequest statusRequest = new StatusRequest();
            statusRequest.setNewStatus(Department.INSPECTION);
            statusRequest.setComment("Sent to Inspection");
            statusRequest.setPercentage(null);
            statusRequest.setAttachmentUrl(null);
            return ResponseEntity.ok(statusService.createStatus(statusRequest, orderId));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", 400
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to send to Inspection: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @PostMapping("/order/{orderId}/machining-selection")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION','MACHINING')")
    public ResponseEntity<?> saveMachiningSelection(
            @PathVariable long orderId,
            @RequestBody RowSelectionRequest request
    ) {
        try {
            if (request == null || request.getSelectedRowIds() == null || request.getSelectedRowIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No rows selected",
                        "status", 400
                ));
            }

            Long machineId = request.getMachineId();
            String machineName = null;
            if (machineId != null) {
                try {
                    MachinesResponse machine = machinesService.getMachines(machineId.intValue());
                    if (machine != null) {
                        machineName = machine.getMachineName();
                    }
                } catch (Exception ignored) {
                }
            }

            return ResponseEntity.ok(pdfService.saveRowSelection(
                    orderId,
                    request.getSelectedRowIds(),
                    Department.MACHINING,
                    request.getAttachmentUrl(),
                    machineId,
                    machineName
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", 400
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save machining selection: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @GetMapping("/order/{orderId}/machining-selection")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION','MACHINING')")
    public ResponseEntity<Map<String, Object>> getMachiningSelection(@PathVariable long orderId) {
        List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);
        if (statuses == null || statuses.isEmpty()) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }

        Status latest = statuses.stream()
                .filter(s -> s != null
                        && s.getNewStatus() == Department.MACHINING
                        && s.getComment() != null
                        && s.getComment().contains("selectedRowIds"))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }

        String comment = latest.getComment().trim();
        try {
            JsonNode node = objectMapper.readTree(comment);
            Map<String, Object> result = new HashMap<>();
            result.put("selectedRowIds", extractStringList(node.get("selectedRowIds")));

            JsonNode machineIdNode = node.get("machineId");
            if (machineIdNode != null && !machineIdNode.isNull()) {
                result.put("machineId", machineIdNode.asLong());
            }
            JsonNode machineNameNode = node.get("machineName");
            if (machineNameNode != null && !machineNameNode.isNull()) {
                result.put("machineName", machineNameNode.asText());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("selectedRowIds", extractRowIdsFromComment(comment)));
        }
    }

    @PostMapping("/order/{orderId}/three-checkbox-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    @Transactional
    public ResponseEntity<?> saveThreeCheckboxSelection(
            @PathVariable long orderId,
            @RequestBody ThreeCheckboxRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Invalid request",
                        "status", 400
                ));
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String authorityRole = null;
            if (authentication != null && authentication.getAuthorities() != null) {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String auth = authority.getAuthority();
                    if (auth != null && auth.startsWith("ROLE_")) {
                        authorityRole = auth;
                        break;
                    }
                }
            }

            Department currentRole;
            try {
                currentRole = resolveEffectiveRole(httpRequest, request, authorityRole);
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", ex.getMessage(),
                        "status", 400
                ));
            }

            if (currentRole == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Unable to determine role",
                        "status", 400
                ));
            }

            List<String> selected;
            if (currentRole == Department.DESIGN) {
                selected = request.getDesignerSelectedRowIds();
            } else if (currentRole == Department.PRODUCTION) {
                selected = request.getProductionSelectedRowIds();
            } else if (currentRole == Department.MACHINING) {
                selected = request.getMachineSelectedRowIds();
            } else {
                selected = request.getInspectionSelectedRowIds();
            }

            if (currentRole != Department.INSPECTION && (selected == null || selected.isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No rows selected",
                        "status", 400
                ));
            }
            if (currentRole == Department.INSPECTION && selected == null) {
                selected = List.of();
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("selectedRowIds", selected);
            payload.put("threeCheckbox", true);
            if (request.getPdfType() != null && !request.getPdfType().isBlank()) {
                payload.put("pdfType", request.getPdfType());
            }
            if (request.getScope() != null && !request.getScope().isBlank()) {
                payload.put("scope", request.getScope());
            }
            
            // CRITICAL FIX: Use the assigned employee's ID from the request, not current user
            Long assignedUserId = request.getAssignedUserId();
            String assignedUsername = null;
            
            if (assignedUserId != null) {
                payload.put("userId", assignedUserId);
                try {
                    User assignedUser = userService.getUserById(assignedUserId).orElse(null);
                    if (assignedUser != null) {
                        assignedUsername = assignedUser.getUsername();
                        payload.put("username", assignedUsername);
                    }
                } catch (Exception e) {
                    // Continue without username
                }
            } else {
                // Fallback to current user if no assigned user ID provided (for direct user selections)
                String currentUsername = authentication.getName();
                Long currentUserId = null;
                try {
                    User currentUser = userService.findByUsername(currentUsername);
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                    }
                } catch (Exception e) {
                    // Continue without user info
                }
                if (currentUserId != null) {
                    payload.put("userId", currentUserId);
                    payload.put("username", currentUsername);
                }
            }

            // Persist row detail objects for Design so Order Report PDFs can display row data.
            if (currentRole == Department.DESIGN && request.getSelectedItems() != null && !request.getSelectedItems().isEmpty()) {
                payload.put("selectedItems", request.getSelectedItems());
            }

            // Persist machine selection context if available.
            Long machineId = request.getMachineId();
            if (currentRole == Department.MACHINING && machineId != null) {
                payload.put("machineId", machineId);
                try {
                    MachinesResponse machine = machinesService.getMachines(machineId.intValue());
                    if (machine != null && machine.getMachineName() != null && !machine.getMachineName().isBlank()) {
                        payload.put("machineName", machine.getMachineName());
                    }
                } catch (Exception ignored) {
                }
            }

            String jsonComment = objectMapper.writeValueAsString(payload);
            pdfService.saveRowSelectionWithoutTransitionRawComment(orderId, currentRole, jsonComment, null);
            return ResponseEntity.ok(Map.of("message", "Three-checkbox selection saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save three-checkbox selection: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @GetMapping("/order/{orderId}/three-checkbox-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<Map<String, Object>> getThreeCheckboxSelection(
            @PathVariable long orderId,
            @RequestParam(value = "pdfType", required = false) String pdfType,
            @RequestParam(value = "scope", required = false) String scope,
            HttpServletRequest httpRequest
    ) {
        Map<String, Object> result = new HashMap<>();
        List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);
        
        if (statuses == null || statuses.isEmpty()) {
            result.put("designerSelectedRowIds", List.of());
            result.put("productionSelectedRowIds", List.of());
            result.put("machineSelectedRowIds", List.of());
            result.put("inspectionSelectedRowIds", List.of());
            return ResponseEntity.ok(result);
        }

        // Get current user for user-specific retrieval
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        Long currentUserId = null;
        User currentUser = null;
        try {
            currentUser = userService.findByUsername(currentUsername);
            if (currentUser != null) {
                currentUserId = currentUser.getId();
            }
        } catch (Exception e) {
            // Continue without user info
        }

        // For mechanist users, filter ALL selections by current user
        // For other users, return department-wide selections
        List<String> designer, production, machine, inspection;

        // Check if user is mechanist by either department or authority
        boolean isMechanist = false;
        if (currentUser != null && currentUser.getDepartment() != null) {
            isMechanist = Department.MACHINING.equals(currentUser.getDepartment());
        }

        // Also check by authority as fallback
        if (!isMechanist && authentication != null && authentication.getAuthorities() != null) {
            isMechanist = authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_MACHINING".equals(auth.getAuthority()));
        }

        // Check if user is an inspection user (for merged machine view)
        boolean isInspectionUser = false;
        if (currentUser != null && currentUser.getDepartment() != null) {
            isInspectionUser = Department.INSPECTION.equals(currentUser.getDepartment());
        }
        if (!isInspectionUser && authentication != null && authentication.getAuthorities() != null) {
            isInspectionUser = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_INSPECTION".equals(auth.getAuthority()));
        }

        if (currentUserId != null && currentUser != null && isMechanist) {
            // Mechanist user: only show their own selections
            designer = extractThreeCheckboxSelectedRowIdsForUser(statuses, currentUserId);
            production = extractThreeCheckboxSelectedRowIdsForUser(statuses, currentUserId);
            machine = extractThreeCheckboxSelectedRowIdsForUser(statuses, currentUserId);
            inspection = extractThreeCheckboxSelectedRowIdsForUser(statuses, currentUserId);
        } else {
            // Non-mechanist user: show department-wide selections.
            // For Inspection users with explicit pdfType/scope, use the unified checkbox tables
            // for Designer base and Production assignments for consistency.
            boolean hasScopeParams = pdfType != null && !pdfType.isBlank() && scope != null && !scope.isBlank();

            if (isInspectionUser && hasScopeParams) {
                try {
                    PdfType pdfEnum = PdfType.valueOf(pdfType.toUpperCase());
                    SelectionScope scopeEnum = SelectionScope.valueOf(scope.toUpperCase());

                    // Designer column from immutable base selection
                    designer = orderCheckboxSelectionService.getDesignerBaseSelection(orderId, pdfEnum, scopeEnum);

                    // Production column as union of all employee assignments for this scope
                    ProductionAssignmentsResponse prodResp = orderCheckboxSelectionService.getProductionAssignments(orderId, pdfEnum, scopeEnum);
                    List<String> prodUnion = new ArrayList<>();
                    if (prodResp != null && prodResp.getAssignments() != null) {
                        for (ProductionAssignmentsResponse.EmployeeRows er : prodResp.getAssignments()) {
                            if ( er == null || er.getRowKeys() == null) continue;
                            for (String rk : er.getRowKeys()) {
                                if (rk == null) continue;
                                prodUnion.add(rk);
                            }
                        }
                    }
                    production = prodUnion;
                } catch (IllegalArgumentException ex) {
                    // Fallback to legacy behavior if enums cannot be resolved
                    designer = extractSelectedRowIdsFromLatestDepartmentStatus(statuses, Department.DESIGN);
                    production = extractSelectedRowIdsFromLatestDepartmentStatus(statuses, Department.PRODUCTION);
                }
            } else {
                designer = extractSelectedRowIdsFromLatestDepartmentStatus(statuses, Department.DESIGN);
                production = extractSelectedRowIdsFromLatestDepartmentStatus(statuses, Department.PRODUCTION);
            }
            if (isInspectionUser) {
                // Inspection users should see merged machine selection from INSPECTION status, scope-wise
                String scopeKey = null;
                if (pdfType != null && scope != null) {
                    scopeKey = mapPdfTypeAndScopeToKey(pdfType, scope);
                }
                machine = extractMergedMachineSelectionFromInspectionStatus(statuses, scopeKey);
            } else {
                // Other users (e.g. Production) continue to see latest MACHINING selection
                machine = extractThreeCheckboxSelectedRowIdsFromLatestMachiningStatus(statuses);
            }
            inspection = extractSelectedRowIdsFromLatestDepartmentStatus(statuses, Department.INSPECTION);
        }

        result.put("designerSelectedRowIds", designer);
        result.put("productionSelectedRowIds", production);
        result.put("machineSelectedRowIds", machine);
        result.put("inspectionSelectedRowIds", inspection);

        Optional<Map<String, Object>> machineCtx = extractMachineContextFromLatestMachiningStatus(statuses);
        machineCtx.ifPresent(result::putAll);
        
        return ResponseEntity.ok(result);
    }

    private List<String> extractSelectedRowIdsFromLatestDepartmentStatus(List<Status> statuses, Department department) {
        if (statuses == null || statuses.isEmpty() || department == null) {
            return List.of();
        }

        Status latest = statuses.stream()
                .filter(s -> s != null
                        && s.getNewStatus() == department
                        && s.getComment() != null
                        && s.getComment().contains("selectedRowIds"))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(latest.getComment().trim());
            return extractStringList(node.get("selectedRowIds"));
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> extractMergedMachineSelectionFromInspectionStatus(List<Status> statuses, String scopeKey) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        // Use the latest INSPECTION status that contains a MACHINING_MERGE payload
        Status latest = statuses.stream()
                .filter(s -> s != null
                        && s.getNewStatus() == Department.INSPECTION
                        && s.getComment() != null
                        && s.getComment().contains("MACHINING_MERGE"))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(latest.getComment().trim());
            JsonNode sourceNode = node.get("source");
            if (sourceNode != null && !sourceNode.isNull()) {
                String source = sourceNode.asText("");
                if (!"MACHINING_MERGE".equals(source)) {
                    return List.of();
                }
            }

            JsonNode scopesNode = node.get("scopes");
            if (scopesNode == null || !scopesNode.isObject()) {
                return List.of();
            }

            List<String> result = new ArrayList<>();
            if (scopeKey != null && !scopeKey.isBlank()) {
                JsonNode arr = scopesNode.get(scopeKey);
                result.addAll(extractStringList(arr));
            } else {
                // No specific scope requested: union all scopes
                scopesNode.fieldNames().forEachRemaining(k -> {
                    JsonNode arr = scopesNode.get(k);
                    result.addAll(extractStringList(arr));
                });
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String classifyScopeKey(String id) {
        // PDF2 (nesting) scopes based on ID prefixes
        String v = id;
        if (v.startsWith("RESULT-")) {
            return "PDF2_RESULTS";
        }
        if (v.startsWith("PLATE-")) {
            return "PDF2_PLATE_INFO";
        }
        if (v.startsWith("PART-")) {
            return "PDF2_PART_INFO";
        }
        // Fallback: treat as standard PDF1 SUBNEST row number
        return "PDF1_SUBNEST";
    }

    private String mapPdfTypeAndScopeToKey(String pdfType, String scope) {
        String t = pdfType != null ? pdfType.toUpperCase() : "";
        String s = scope != null ? scope.toUpperCase() : "";

        if ("PDF1".equals(t)) {
            if ("SUBNEST".equals(s)) return "PDF1_SUBNEST";
            if ("PARTS".equals(s)) return "PDF1_PARTS";
            if ("MATERIAL".equals(s)) return "PDF1_MATERIAL";
        }

        if ("PDF2".equals(t)) {
            if ("NESTING_RESULTS".equals(s)) return "PDF2_RESULTS";
            if ("NESTING_PLATE_INFO".equals(s)) return "PDF2_PLATE_INFO";
            if ("NESTING_PART_INFO".equals(s)) return "PDF2_PART_INFO";
        }

        return null;
    }

    private Optional<Map<String, Object>> extractMachineContextFromLatestMachiningStatus(List<Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Optional.empty();
        }

        Status latest = statuses.stream()
                .filter(s -> s != null
                        && s.getNewStatus() == Department.MACHINING
                        && s.getComment() != null
                        && (s.getComment().contains("machineId") || s.getComment().contains("machineName")))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode node = objectMapper.readTree(latest.getComment().trim());
            Map<String, Object> ctx = new HashMap<>();
            JsonNode machineIdNode = node.get("machineId");
            if (machineIdNode != null && !machineIdNode.isNull()) {
                ctx.put("machineId", machineIdNode.asLong());
            }
            JsonNode machineNameNode = node.get("machineName");
            if (machineNameNode != null && !machineNameNode.isNull()) {
                ctx.put("machineName", machineNameNode.asText());
            }
            return ctx.isEmpty() ? Optional.empty() : Optional.of(ctx);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private List<String> extractThreeCheckboxSelectedRowIdsFromLatestMachiningStatus(List<Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        Status latest = statuses.stream()
                .filter(s -> s != null
                        && s.getNewStatus() == Department.MACHINING
                        && s.getComment() != null
                        && s.getComment().contains("threeCheckbox")
                        && s.getComment().contains("selectedRowIds"))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(latest.getComment().trim());
            return extractStringList(node.get("selectedRowIds"));
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> extractThreeCheckboxSelectedRowIdsForUser(List<Status> statuses, Long userId) {
        if (statuses == null || statuses.isEmpty() || userId == null) {
            return List.of();
        }

        // Find all status entries (PRODUCTION or MACHINING) that contain threeCheckbox data for this specific user
        List<Status> userStatuses = statuses.stream()
                .filter(s -> s != null
                        && (s.getNewStatus() == Department.MACHINING || s.getNewStatus() == Department.PRODUCTION)
                        && s.getComment() != null
                        && s.getComment().contains("threeCheckbox")
                        && s.getComment().contains("selectedRowIds")
                        && s.getComment().contains("\"userId\":" + userId))
                .collect(java.util.stream.Collectors.toList());

        if (userStatuses.isEmpty()) {
            return List.of();
        }

        // Get the latest status for this user
        Status latestUserStatus = userStatuses.stream()
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latestUserStatus == null || latestUserStatus.getComment() == null || latestUserStatus.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(latestUserStatus.getComment().trim());
            return extractStringList(node.get("selectedRowIds"));
        } catch (Exception e) {
            return List.of();
        }
    }

    private Department resolveEffectiveRole(HttpServletRequest req, ThreeCheckboxRequest body, String authorityRole) {
        boolean isAdmin = "ROLE_ADMIN".equals(authorityRole);

        int bodyNonEmptyCount = 0;
        Department bodyInferred = null;
        if (body != null) {
            if (body.getDesignerSelectedRowIds() != null && !body.getDesignerSelectedRowIds().isEmpty()) {
                bodyNonEmptyCount++;
                bodyInferred = Department.DESIGN;
            }
            if (body.getProductionSelectedRowIds() != null && !body.getProductionSelectedRowIds().isEmpty()) {
                bodyNonEmptyCount++;
                bodyInferred = Department.PRODUCTION;
            }
            // NOTE: machineSelectedRowIds is intentionally ignored for role inference.
        }

        if (isAdmin) {
            if (bodyNonEmptyCount != 1) {
                throw new IllegalArgumentException("ADMIN must provide exactly one non-empty role selection array");
            }
            return bodyInferred;
        }

        // Body-driven inference is highest priority for DESIGN / PRODUCTION only.
        if (bodyNonEmptyCount == 1) {
            return bodyInferred;
        }

        Department fromContext = resolveRoleFromRequestContext(req);
        if (fromContext != null) {
            return fromContext;
        }

        if ("ROLE_DESIGN".equals(authorityRole)) {
            return Department.DESIGN;
        }
        if ("ROLE_PRODUCTION".equals(authorityRole)) {
            return Department.PRODUCTION;
        }
        if ("ROLE_MACHINING".equals(authorityRole)) {
            return Department.MACHINING;
        }
        if ("ROLE_INSPECTION".equals(authorityRole)) {
            return Department.INSPECTION;
        }
        return null;
    }

    private Department resolveRoleFromRequestContext(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String referer = request.getHeader("Referer");
        String forwardedUri = request.getHeader("X-Forwarded-Uri");
        String originalUri = request.getHeader("X-Original-URI");
        String originalUrl = request.getHeader("X-Original-URL");

        String context = (referer != null ? referer : "")
                + " " + (forwardedUri != null ? forwardedUri : "")
                + " " + (originalUri != null ? originalUri : "")
                + " " + (originalUrl != null ? originalUrl : "");
        String lower = context.toLowerCase();

        if (lower.contains("/designuser/")) {
            return Department.DESIGN;
        }
        if (lower.contains("/productionuser/")) {
            return Department.PRODUCTION;
        }
        if (lower.contains("/mechanistuser/") || lower.contains("/mechanicuser/") || lower.contains("/machinistuser/")) {
            return Department.MACHINING;
        }
        if (lower.contains("/inspectionuser/")) {
            return Department.INSPECTION;
        }
        return null;
    }

    private Status findLatestStatus(List<Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return statuses.stream()
                .max(
                        Comparator
                                .comparing((Status s) -> {
                                    String createdAt = s.getCreatedAt();
                                    if (createdAt == null || createdAt.isBlank()) {
                                        return LocalDate.MIN;
                                    }
                                    try {
                                        return LocalDate.parse(createdAt, formatter);
                                    } catch (DateTimeParseException e) {
                                        return LocalDate.MIN;
                                    }
                                })
                                .thenComparing(Status::getId)
                )
                .orElse(null);
    }

    private List<String> extractStringList(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (JsonNode n : node) {
            if (n != null && !n.isNull()) {
                String v = n.asText();
                if (v != null && !v.isBlank()) {
                    ids.add(v);
                }
            }
        }
        return ids;
    }

    private List<String> extractRowIdsFromComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(comment);
            JsonNode selectedArray = node.get("selectedRowIds");
            List<String> ids = new ArrayList<>();
            if (selectedArray != null && selectedArray.isArray()) {
                for (JsonNode idNode : selectedArray) {
                    String id = idNode.asText();
                    if (id != null && !id.isBlank()) {
                        ids.add(id);
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            // Fallback parsing
            try {
                int start = comment.indexOf('[');
                int end = comment.indexOf(']');
                if (start == -1 || end == -1 || end <= start) {
                    return List.of();
                }
                String inside = comment.substring(start + 1, end);
                String[] parts = inside.split(",");
                List<String> ids = new ArrayList<>();
                for (String p : parts) {
                    String id = p.trim();
                    if (id.startsWith("\"")) {
                        id = id.substring(1);
                    }
                    if (id.endsWith("\"")) {
                        id = id.substring(0, id.length() - 1);
                    }
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }
                return ids;
            } catch (Exception fallback) {
                return List.of();
            }
        }
    }
}
