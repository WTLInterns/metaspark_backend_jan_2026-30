package com.switflow.swiftFlow.Request;

import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;

import java.util.List;

public class DesignerBaseSelectionRequest {

    private PdfType pdfType;
    private SelectionScope scope;
    private List<String> rowKeys;

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

    public List<String> getRowKeys() {
        return rowKeys;
    }

    public void setRowKeys(List<String> rowKeys) {
        this.rowKeys = rowKeys;
    }
}
