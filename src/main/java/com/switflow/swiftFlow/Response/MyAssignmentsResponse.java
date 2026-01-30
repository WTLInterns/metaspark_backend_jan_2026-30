package com.switflow.swiftFlow.Response;

import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;

import java.util.List;

public class MyAssignmentsResponse {

    private PdfType pdfType;
    private SelectionScope scope;
    private List<String> designerBaseRowKeys;
    private List<String> myAssignedRowKeys;

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

    public List<String> getDesignerBaseRowKeys() {
        return designerBaseRowKeys;
    }

    public void setDesignerBaseRowKeys(List<String> designerBaseRowKeys) {
        this.designerBaseRowKeys = designerBaseRowKeys;
    }

    public List<String> getMyAssignedRowKeys() {
        return myAssignedRowKeys;
    }

    public void setMyAssignedRowKeys(List<String> myAssignedRowKeys) {
        this.myAssignedRowKeys = myAssignedRowKeys;
    }
}
