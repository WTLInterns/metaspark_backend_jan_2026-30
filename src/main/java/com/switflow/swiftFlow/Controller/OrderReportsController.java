package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Service.OrderReportPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/{orderId}/reports")
public class OrderReportsController {

    @Autowired
    private OrderReportPdfService orderReportPdfService;

    @GetMapping(value = "/design", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN')")
    public ResponseEntity<byte[]> downloadDesignReport(@PathVariable long orderId) {
        return buildPdfResponse(orderId, OrderReportPdfService.ReportType.DESIGN, "Design_Report");
    }

    @GetMapping(value = "/production", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION')")
    public ResponseEntity<byte[]> downloadProductionReport(@PathVariable long orderId) {
        return buildPdfResponse(orderId, OrderReportPdfService.ReportType.PRODUCTION, "Production_Report");
    }

    @GetMapping(value = "/machinists", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MACHINING')")
    public ResponseEntity<byte[]> downloadMachinistsReport(@PathVariable long orderId) {
        return buildPdfResponse(orderId, OrderReportPdfService.ReportType.MACHINISTS, "Machinists_Report");
    }

    @GetMapping(value = "/inspection", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTION')")
    public ResponseEntity<byte[]> downloadInspectionReport(@PathVariable long orderId) {
        return buildPdfResponse(orderId, OrderReportPdfService.ReportType.INSPECTION, "Inspection_Report");
    }

    private ResponseEntity<byte[]> buildPdfResponse(long orderId, OrderReportPdfService.ReportType type, String suffix) {
        try {
            byte[] pdfBytes = orderReportPdfService.generateReportPdf(orderId, type);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename(orderId + "_" + suffix + ".pdf").build());
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
