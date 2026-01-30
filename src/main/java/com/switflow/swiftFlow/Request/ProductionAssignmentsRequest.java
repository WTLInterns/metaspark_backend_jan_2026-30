package com.switflow.swiftFlow.Request;

import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;

import java.util.List;

public class ProductionAssignmentsRequest {

    private PdfType pdfType;
    private SelectionScope scope;
    private List<EmployeeAssignment> assignments;

    public static class EmployeeAssignment {
        private Long userId;
        private List<String> rowKeys;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public List<String> getRowKeys() {
            return rowKeys;
        }

        public void setRowKeys(List<String> rowKeys) {
            this.rowKeys = rowKeys;
        }
    }

    public PdfType getPdfType() {
        return pdfType;
    }

    public void setPdfType(PdfType pdfType) {
        this.pdfType = pdfType;
    }

    public SelectionScope getScope() {
        return scope;
    }

    public void setScope(SelectionScope scope) {
        this.scope = scope;
    }

    public List<EmployeeAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<EmployeeAssignment> assignments) {
        this.assignments = assignments;
    }
}
