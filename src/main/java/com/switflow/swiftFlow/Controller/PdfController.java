package com.switflow.swiftFlow.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switflow.swiftFlow.Entity.Status;
import com.switflow.swiftFlow.Response.MachinesResponse;
import com.switflow.swiftFlow.Service.MachinesService;
import com.switflow.swiftFlow.Service.PdfService;
import com.switflow.swiftFlow.Service.StatusService;
import com.switflow.swiftFlow.Repo.StatusRepository;
import com.switflow.swiftFlow.Request.StatusRequest;
import com.switflow.swiftFlow.pdf.PdfRow;
import com.switflow.swiftFlow.utility.Department;
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
    public ResponseEntity<Map<String, Object>> getThreeCheckboxSelection(@PathVariable long orderId) {
        Map<String, Object> result = new HashMap<>();
        List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);
        if (statuses == null || statuses.isEmpty()) {
            result.put("designerSelectedRowIds", List.of());
            result.put("productionSelectedRowIds", List.of());
            result.put("machineSelectedRowIds", List.of());
            result.put("inspectionSelectedRowIds", List.of());
            return ResponseEntity.ok(result);
        }

        List<String> designer = extractSelectedRowIdsFromLatestDepartmentStatus(statuses, Department.DESIGN);
        List<String> production = extractSelectedRowIdsFromLatestDepartmentStatus(statuses, Department.PRODUCTION);
        List<String> inspection = extractSelectedRowIdsFromLatestDepartmentStatus(statuses, Department.INSPECTION);

        List<String> machine = extractThreeCheckboxSelectedRowIdsFromLatestMachiningStatus(statuses);

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
