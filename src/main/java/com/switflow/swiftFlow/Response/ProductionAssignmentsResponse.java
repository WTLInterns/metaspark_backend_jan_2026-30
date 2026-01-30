package com.switflow.swiftFlow.Response;

import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;

import java.util.List;

public class ProductionAssignmentsResponse {

    public static class EmployeeRows {
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

    private PdfType pdfType;
    private SelectionScope scope;
    private List<EmployeeRows> assignments;

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

    public List<EmployeeRows> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<EmployeeRows> assignments) {
        this.assignments = assignments;
    }
}
