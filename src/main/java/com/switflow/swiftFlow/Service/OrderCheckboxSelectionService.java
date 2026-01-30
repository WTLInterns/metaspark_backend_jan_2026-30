package com.switflow.swiftFlow.Service;

import com.switflow.swiftFlow.Entity.OrderCheckboxAssignment;
import com.switflow.swiftFlow.Entity.OrderCheckboxBaseSelection;
import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Repo.OrderCheckboxAssignmentRepository;
import com.switflow.swiftFlow.Repo.OrderCheckboxBaseSelectionRepository;
import com.switflow.swiftFlow.Request.ProductionAssignmentsRequest;
import com.switflow.swiftFlow.Response.MyAssignmentsResponse;
import com.switflow.swiftFlow.Response.ProductionAssignmentsResponse;
import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderCheckboxSelectionService {

    @Autowired
    private OrderCheckboxBaseSelectionRepository baseSelectionRepository;

    @Autowired
    private OrderCheckboxAssignmentRepository assignmentRepository;

    @Transactional
    public void saveDesignerBaseSelection(Long orderId, PdfType pdfType, SelectionScope scope, List<String> rowKeys, User currentUser) {
        if (orderId == null || pdfType == null || scope == null) {
            throw new IllegalArgumentException("orderId, pdfType and scope are required");
        }
        if (rowKeys == null || rowKeys.isEmpty()) {
            throw new IllegalArgumentException("rowKeys is required");
        }

        Long createdBy = (currentUser != null ? currentUser.getId() : null);

        // Immutable base selection behavior: do NOT delete existing; only add missing keys.
        for (String rawKey : rowKeys) {
            String key = normalizeKey(rawKey);
            if (key == null) continue;

            boolean exists = baseSelectionRepository.existsByOrderIdAndPdfTypeAndScopeAndRowKey(orderId, pdfType, scope, key);
            if (exists) continue;

            OrderCheckboxBaseSelection entity = new OrderCheckboxBaseSelection();
            entity.setOrderId(orderId);
            entity.setPdfType(pdfType);
            entity.setScope(scope);
            entity.setRowKey(key);
            entity.setCreatedByUserId(createdBy);
            baseSelectionRepository.save(entity);
        }
    }

    @Transactional
    public void assignRowsToEmployees(Long orderId, PdfType pdfType, SelectionScope scope, List<ProductionAssignmentsRequest.EmployeeAssignment> assignments, User currentUser) {
        if (orderId == null || pdfType == null || scope == null) {
            throw new IllegalArgumentException("orderId, pdfType and scope are required");
        }
        if (assignments == null || assignments.isEmpty()) {
            throw new IllegalArgumentException("assignments is required");
        }

        // Build allowed set from designer base selection
        Set<String> baseKeys = baseSelectionRepository.findByOrderIdAndPdfTypeAndScope(orderId, pdfType, scope)
                .stream()
                .map(OrderCheckboxBaseSelection::getRowKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (baseKeys.isEmpty()) {
            throw new IllegalArgumentException("No designer base selection exists for this order/pdfType/scope");
        }

        Long assignedByUserId = (currentUser != null ? currentUser.getId() : null);

        for (ProductionAssignmentsRequest.EmployeeAssignment ea : assignments) {
            if (ea == null || ea.getUserId() == null) {
                throw new IllegalArgumentException("Each assignment must include userId");
            }
            List<String> rowKeys = ea.getRowKeys();
            if (rowKeys == null || rowKeys.isEmpty()) {
                continue;
            }

            for (String rawKey : rowKeys) {
                String key = normalizeKey(rawKey);
                if (key == null) continue;

                if (!baseKeys.contains(key)) {
                    throw new IllegalArgumentException("Assigned rowKey not in designer base selection: " + key);
                }

                OrderCheckboxAssignment entity = new OrderCheckboxAssignment();
                entity.setOrderId(orderId);
                entity.setPdfType(pdfType);
                entity.setScope(scope);
                entity.setRowKey(key);
                entity.setAssignedToUserId(ea.getUserId());
                entity.setAssignedByUserId(assignedByUserId);

                try {
                    assignmentRepository.save(entity);
                } catch (DataIntegrityViolationException e) {
                    // Unique constraint triggered => row already assigned to another employee
                    throw e;
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public MyAssignmentsResponse getMyAssignments(Long orderId, PdfType pdfType, SelectionScope scope, User currentUser) {
        if (orderId == null || pdfType == null || scope == null) {
            throw new IllegalArgumentException("orderId, pdfType and scope are required");
        }
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Unable to determine current user");
        }

        List<String> base = baseSelectionRepository.findByOrderIdAndPdfTypeAndScope(orderId, pdfType, scope)
                .stream()
                .map(OrderCheckboxBaseSelection::getRowKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> mine = assignmentRepository.findByOrderIdAndPdfTypeAndScopeAndAssignedToUserId(orderId, pdfType, scope, currentUser.getId())
                .stream()
                .map(OrderCheckboxAssignment::getRowKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        MyAssignmentsResponse response = new MyAssignmentsResponse();
        response.setPdfType(pdfType);
        response.setScope(scope);
        response.setDesignerBaseRowKeys(base);
        response.setMyAssignedRowKeys(mine);
        return response;
    }

    @Transactional(readOnly = true)
    public ProductionAssignmentsResponse getProductionAssignments(Long orderId, PdfType pdfType, SelectionScope scope) {
        if (orderId == null || pdfType == null || scope == null) {
            throw new IllegalArgumentException("orderId, pdfType and scope are required");
        }

        List<OrderCheckboxAssignment> all = assignmentRepository.findByOrderIdAndPdfTypeAndScope(orderId, pdfType, scope);

        Map<Long, List<String>> grouped = new LinkedHashMap<>();
        for (OrderCheckboxAssignment a : all) {
            if (a == null) continue;
            if (a.getAssignedToUserId() == null || a.getRowKey() == null) continue;
            grouped.computeIfAbsent(a.getAssignedToUserId(), k -> new ArrayList<>()).add(a.getRowKey());
        }

        ProductionAssignmentsResponse resp = new ProductionAssignmentsResponse();
        resp.setPdfType(pdfType);
        resp.setScope(scope);

        List<ProductionAssignmentsResponse.EmployeeRows> assignments = new ArrayList<>();
        for (Map.Entry<Long, List<String>> e : grouped.entrySet()) {
            ProductionAssignmentsResponse.EmployeeRows er = new ProductionAssignmentsResponse.EmployeeRows();
            er.setUserId(e.getKey());
            er.setRowKeys(e.getValue());
            assignments.add(er);
        }
        resp.setAssignments(assignments);
        return resp;
    }

    @Transactional(readOnly = true)
    public List<String> getDesignerBaseSelection(Long orderId, PdfType pdfType, SelectionScope scope) {
        if (orderId == null || pdfType == null || scope == null) {
            throw new IllegalArgumentException("orderId, pdfType and scope are required");
        }
        return baseSelectionRepository.findByOrderIdAndPdfTypeAndScope(orderId, pdfType, scope)
                .stream()
                .map(OrderCheckboxBaseSelection::getRowKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeKey(String raw) {
        if (raw == null) return null;
        String key = raw.trim();
        if (key.isEmpty()) return null;
        return key;
    }
}
