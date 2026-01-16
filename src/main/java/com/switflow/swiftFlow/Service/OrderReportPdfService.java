package com.switflow.swiftFlow.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switflow.swiftFlow.Entity.Customer;
import com.switflow.swiftFlow.Entity.Orders;
import com.switflow.swiftFlow.Entity.Product;
import com.switflow.swiftFlow.Entity.Status;
import com.switflow.swiftFlow.Exception.OrderNotFoundException;
import com.switflow.swiftFlow.Repo.OrderRepository;
import com.switflow.swiftFlow.Repo.StatusRepository;
import com.switflow.swiftFlow.Response.SubNestRowDto;
import com.switflow.swiftFlow.utility.Department;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderReportPdfService {

    public enum ReportType {
        DESIGN,
        PRODUCTION,
        MACHINISTS,
        INSPECTION
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PdfSubnestService pdfSubnestService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] generateReportPdf(long orderId, ReportType reportType) throws IOException {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDImageXObject logoImage = loadLogoImage(doc);
            float headerHeight = 60f;
            PdfTextCursor cursor = new PdfTextCursor(doc, logoImage, headerHeight);
            try {
                cursor.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cursor.writeLine(buildReportTitle(order, reportType));
                cursor.newLine();

                cursor.setFont(PDType1Font.HELVETICA, 10);
                writeSharedHeader(cursor, order, statuses, reportType);
                cursor.newLine();

                writeRoleSpecificSection(cursor, order, statuses, reportType);
            } finally {
                cursor.close();
            }

            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Loads the company logo from the classpath and creates a PDFBox image for embedding.
     *
     * The expected location is {@code src/main/resources/static/images/logo.png}, which
     * will be available on the classpath at {@code static/images/logo.png} in production.
     */
    private PDImageXObject loadLogoImage(PDDocument document) throws IOException {
        ClassPathResource resource = new ClassPathResource("static/images/logo.png");
        if (!resource.exists()) {
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return PDImageXObject.createFromByteArray(document, bytes, "company-logo");
        }
    }

    private String buildReportTitle(Orders order, ReportType reportType) {
        return "Order #" + order.getOrderId() + " - " + switch (reportType) {
            case DESIGN -> "Design Report";
            case PRODUCTION -> "Production Report";
            case MACHINISTS -> "Machinists Report";
            case INSPECTION -> "Inspection Report";
        };
    }

    private Department mapReportTypeToDepartment(ReportType reportType) {
        return switch (reportType) {
            case DESIGN -> Department.DESIGN;
            case PRODUCTION -> Department.PRODUCTION;
            case MACHINISTS -> Department.MACHINING;
            case INSPECTION -> Department.INSPECTION;
        };
    }

    private String getSelectionHeaderForReportType(ReportType reportType) {
        return switch (reportType) {
            case DESIGN -> "Design-selected checkbox data";
            case PRODUCTION -> "Production-selected checkbox data";
            case MACHINISTS -> "Machinists-selected checkbox data";
            case INSPECTION -> "Inspection-selected checkbox data";
        };
    }

    private void writeSharedHeader(PdfTextCursor cursor, Orders order, List<Status> statuses, ReportType reportType) throws IOException {
        cursor.writeSectionHeader("Production stage / order progress");

        Department currentStage = order.getDepartment();
        String progressPercentage = findLatestProgressPercentage(statuses).orElse("—");
        cursor.writeKeyValueAligned("Current stage", currentStage != null ? currentStage.name() : "—");
        cursor.writeKeyValueAligned("Progress percentage", progressPercentage);
        cursor.newLine();

        cursor.writeSectionHeader("Project Details");
        cursor.writeKeyValueAligned("Units", safe(order.getUnits()));
        cursor.writeKeyValueAligned("Material", safe(order.getMaterial()));

        Customer customer = (order.getCustomers() != null && !order.getCustomers().isEmpty())
                ? order.getCustomers().get(0)
                : null;
        cursor.writeKeyValueAligned("Customer", customer != null ? safe(customer.getCustomerName()) : "—");
        cursor.writeKeyValueAligned("Billing Address", customer != null ? safe(customer.getBillingAddress()) : "—");
        cursor.writeKeyValueAligned("Shipping Address", customer != null ? safe(customer.getShippingAddress()) : "—");
        cursor.newLine();

        cursor.writeSectionHeader(getSelectionHeaderForReportType(reportType));

        Department selectionDepartment = mapReportTypeToDepartment(reportType);

        List<String> selectedRowIds = extractSelectedRowIdsForDepartment(statuses, selectionDepartment);
        List<JsonNode> allSubNestItems = buildAllSubNestItems(statuses);
        List<JsonNode> mappedItems = mapSelectedRowIdsToItems(allSubNestItems, selectedRowIds);

        List<JsonNode> selectedItems = !mappedItems.isEmpty()
                ? mappedItems
                : extractSelectedItemsForDepartment(statuses, selectionDepartment);

        if (selectedItems.isEmpty()) {
            cursor.writeLine("No design selection details available");
            return;
        }

        drawDesignSelectedItemsTable(cursor, selectedItems);
    }

    private String findLatestAttachmentUrl(List<Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return statuses.stream()
                .filter(s -> s != null && s.getAttachmentUrl() != null && !s.getAttachmentUrl().isBlank())
                .max(Comparator.comparing(Status::getId))
                .map(Status::getAttachmentUrl)
                .orElse(null);
    }

    private List<JsonNode> buildAllSubNestItems(List<Status> statuses) {
        try {
            String attachmentUrl = findLatestAttachmentUrl(statuses);
            if (attachmentUrl == null || attachmentUrl.isBlank()) {
                return List.of();
            }
            List<SubNestRowDto> rows = pdfSubnestService.parseSubnestFromUrl(attachmentUrl);
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            List<JsonNode> result = new ArrayList<>();
            for (SubNestRowDto r : rows) {
                if (r == null) {
                    continue;
                }
                com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();
                node.put("rowNo", r.getRowNo());
                node.put("sizeX", r.getSizeX());
                node.put("sizeY", r.getSizeY());
                node.put("material", r.getMaterial());
                node.put("thickness", r.getThickness());
                node.put("time", r.getTimePerInstance());
                node.put("totalTime", r.getTotalTime());
                node.put("ncFile", r.getNcFile());
                node.put("quantity", r.getQty());
                node.put("area", r.getAreaM2());
                node.put("efficiencyPercent", r.getEfficiencyPercent());
                result.add(node);
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<JsonNode> mapSelectedRowIdsToItems(List<JsonNode> allItems, List<String> selectedRowIds) {
        if (allItems == null || allItems.isEmpty() || selectedRowIds == null || selectedRowIds.isEmpty()) {
            return List.of();
        }
        List<JsonNode> result = new ArrayList<>();
        for (String rowId : selectedRowIds) {
            if (rowId == null || rowId.isBlank()) {
                continue;
            }
            try {
                int index = Integer.parseInt(rowId.trim()) - 1;
                if (index < 0 || index >= allItems.size()) {
                    continue;
                }
                JsonNode item = allItems.get(index);
                if (item != null && !item.isNull()) {
                    result.add(item);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private void drawDesignSelectedItemsTable(PdfTextCursor cursor, List<JsonNode> selectedItems) throws IOException {
        cursor.drawDesignSelectedTableHeader();

        int index = 1;
        for (JsonNode item : selectedItems) {
            String ncFile = safeInline(textOrEmpty(item.get("ncFile")));
            String material = safeInline(textOrEmpty(item.get("material")));
            String thickness = safeInline(textOrEmpty(item.get("thickness")));
            String sizeX = safeInline(textOrEmpty(item.get("sizeX")));
            String sizeY = safeInline(textOrEmpty(item.get("sizeY")));
            String qty = safeInline(textOrEmpty(item.get("quantity")));
            String area = safeInline(textOrEmpty(item.get("area")));
            String timePerInst = safeInline(textOrEmpty(item.get("time")));

            // Optional fields depending on upstream payload shape; kept blank if absent.
            String totalTime = safeInline(textOrEmpty(item.get("totalTime")));
            String eff = safeInline(textOrEmpty(item.get("efficiencyPercent")));

            cursor.drawDesignSelectedTableRow(
                    index,
                    sizeX,
                    sizeY,
                    material,
                    thickness,
                    timePerInst,
                    totalTime,
                    ncFile,
                    qty,
                    area,
                    eff
            );
            index++;
        }

        cursor.newLine();
    }

    private List<JsonNode> extractSelectedItemsPreferDepartments(List<Status> statuses, Department... preferredDepartments) {
        if (preferredDepartments == null || preferredDepartments.length == 0) {
            return List.of();
        }

        for (Department department : preferredDepartments) {
            if (department == null) {
                continue;
            }
            List<JsonNode> items = extractSelectedItemsForDepartment(statuses, department);
            if (!items.isEmpty()) {
                return items;
            }
        }
        return List.of();
    }

    private List<JsonNode> extractSelectedItemsForDepartment(List<Status> statuses, Department department) {
        Status latest = statuses.stream()
                .filter(s -> s.getNewStatus() == department
                        && s.getComment() != null
                        && (s.getComment().contains("selectedItems") || s.getComment().contains("selectedRowIds")))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(latest.getComment());

            // Preferred: row detail objects for richer table rendering.
            JsonNode arr = root.get("selectedItems");
            if (arr != null && arr.isArray() && !arr.isEmpty()) {
                List<JsonNode> items = new ArrayList<>();
                for (JsonNode n : arr) {
                    if (n != null && n.isObject()) {
                        items.add(n);
                    }
                }
                if (!items.isEmpty()) {
                    return items;
                }
            }

            // Backward-compatible: if only selectedRowIds is stored for the role, still show the
            // role's selections in the same table layout by deriving minimal row objects.
            JsonNode selectedRowIds = root.get("selectedRowIds");
            if (selectedRowIds != null && selectedRowIds.isArray() && !selectedRowIds.isEmpty()) {
                List<JsonNode> derived = new ArrayList<>();
                for (JsonNode idNode : selectedRowIds) {
                    if (idNode == null || idNode.isNull()) {
                        continue;
                    }
                    String id = idNode.asText();
                    if (id == null || id.isBlank()) {
                        continue;
                    }
                    com.fasterxml.jackson.databind.node.ObjectNode row = objectMapper.createObjectNode();
                    // Keep all other columns blank so layout remains unchanged; show the id under NC file.
                    row.put("sizeX", "");
                    row.put("sizeY", "");
                    row.put("material", "");
                    row.put("thickness", "");
                    row.put("time", "");
                    row.put("totalTime", "");
                    row.put("ncFile", id);
                    row.put("quantity", "");
                    row.put("area", "");
                    row.put("efficiencyPercent", "");
                    derived.add(row);
                }
                return derived;
            }

            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        return node.toString();
    }

    private String safeInline(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value;
    }

    private void writeRoleSpecificSection(PdfTextCursor cursor, Orders order, List<Status> statuses, ReportType reportType) throws IOException {
        cursor.writeSectionHeader("Report Details");

        switch (reportType) {
            case DESIGN -> {
                cursor.writeSectionHeader("Design-specific data");
                cursor.writeKeyValueAligned("Custom product details", safe(order.getCustomProductDetails()));
                cursor.writeKeyValueAligned("Product details", safe(order.getProductDetails()));
                List<Product> products = order.getProducts();
                if (products != null && !products.isEmpty()) {
                    String productCodes = products.stream()
                            .map(Product::getProductCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "));
                    cursor.writeKeyValueAligned("Product codes", productCodes.isBlank() ? "—" : productCodes);
                }
                writeLatestStatusSummary(cursor, statuses, Department.DESIGN);
            }
            case PRODUCTION -> {
                cursor.writeSectionHeader("Production-specific data");
                List<String> productionSelection = extractSelectedRowIdsForDepartment(statuses, Department.PRODUCTION);
                cursor.writeKeyValueAligned("Selected items count", String.valueOf(productionSelection.size()));
                if (!productionSelection.isEmpty()) {
                    cursor.writeLine("Selected items:");
                    for (String id : productionSelection) {
                        cursor.writeLine("- " + id);
                    }
                }
                writeLatestStatusSummary(cursor, statuses, Department.PRODUCTION);
            }
            case MACHINISTS -> {
                cursor.writeSectionHeader("Machinists-specific data");
                List<String> machineSelection = extractSelectedRowIdsForDepartment(statuses, Department.MACHINING);
                cursor.writeKeyValueAligned("Selected items count", String.valueOf(machineSelection.size()));
                if (!machineSelection.isEmpty()) {
                    cursor.writeLine("Selected items:");
                    for (String id : machineSelection) {
                        cursor.writeLine("- " + id);
                    }
                }
                writeLatestStatusSummary(cursor, statuses, Department.MACHINING);
            }
            case INSPECTION -> {
                cursor.writeSectionHeader("Inspection-specific data");
                List<String> inspectionSelection = extractSelectedRowIdsForDepartment(statuses, Department.INSPECTION);
                cursor.writeKeyValueAligned("Selected items count", String.valueOf(inspectionSelection.size()));
                if (!inspectionSelection.isEmpty()) {
                    cursor.writeLine("Selected items:");
                    for (String id : inspectionSelection) {
                        cursor.writeLine("- " + id);
                    }
                }
                writeLatestStatusSummary(cursor, statuses, Department.INSPECTION);
            }
        }
    }

    private void writeLatestStatusSummary(PdfTextCursor cursor, List<Status> statuses, Department department) throws IOException {
        Status latest = statuses.stream()
                .filter(s -> s.getNewStatus() == department)
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null) {
            cursor.writeLine("No status updates found for " + department.name());
            return;
        }

        if (latest.getComment() != null && !latest.getComment().isBlank()) {
            cursor.writeKeyValueAligned("Latest comment", latest.getComment());
        }
        if (latest.getAttachmentUrl() != null && !latest.getAttachmentUrl().isBlank()) {
            cursor.writeKeyValueAligned("Attachment", latest.getAttachmentUrl());
        }
        if (latest.getCreatedAt() != null && !latest.getCreatedAt().isBlank()) {
            cursor.writeKeyValueAligned("Updated at", latest.getCreatedAt());
        }
        if (latest.getPercentage() != null && !latest.getPercentage().isBlank()) {
            cursor.writeKeyValueAligned("Progress percentage", latest.getPercentage());
        }
    }

    private Optional<String> findLatestProgressPercentage(List<Status> statuses) {
        return statuses.stream()
                .filter(s -> s.getPercentage() != null && !s.getPercentage().isBlank())
                .max(Comparator.comparing(Status::getId))
                .map(Status::getPercentage);
    }

    private List<String> extractSelectedRowIdsForDepartment(List<Status> statuses, Department department) throws IOException {
        Status latest = statuses.stream()
                .filter(s -> s.getNewStatus() == department && s.getComment() != null && s.getComment().contains("selectedRowIds"))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(latest.getComment());
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
            return fallbackParseSelectedRowIds(latest.getComment());
        }
    }

    private List<String> fallbackParseSelectedRowIds(String comment) {
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
        } catch (Exception e) {
            return List.of();
        }
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value;
    }

    private static class PdfTextCursor {
        private final PDDocument doc;
        private PDPage page;
        private PDPageContentStream content;

        private float margin = 40f;
        private float y;
        private float leading = 14f;
        private float bottomMargin = 40f;

        private PDType1Font font = PDType1Font.HELVETICA;
        private float fontSize = 10f;
        private final PDImageXObject logoImage;
        private final float headerHeight;

        private PdfTextCursor(PDDocument doc, PDImageXObject logoImage, float headerHeight) throws IOException {
            this.doc = doc;
            this.logoImage = logoImage;
            this.headerHeight = headerHeight;
            newPage();
        }

        private void newPage() throws IOException {
            if (content != null) {
                content.close();
            }
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            content = new PDPageContentStream(doc, page);

            PDRectangle mediaBox = page.getMediaBox();

            // Draw logo in the top-left corner (inside the page margin) on every page,
            // and then reserve a header band so that text starts below the logo area.
            if (logoImage != null && headerHeight > 0) {
                float availableHeaderHeight = Math.max(headerHeight - 10f, 20f);

                // Scale logo to fit within a reasonable width/height while preserving aspect ratio.
                float maxLogoWidth = 120f;
                float maxLogoHeight = availableHeaderHeight;
                float scaleX = maxLogoWidth / logoImage.getWidth();
                float scaleY = maxLogoHeight / logoImage.getHeight();
                float scale = Math.min(scaleX, scaleY);

                float logoWidth = logoImage.getWidth() * scale;
                float logoHeight = logoImage.getHeight() * scale;

                float logoX = margin;
                float logoY = mediaBox.getUpperRightY() - margin - logoHeight;

                content.drawImage(logoImage, logoX, logoY, logoWidth, logoHeight);

                // Start text below the reserved header area, not directly under the image
                this.y = mediaBox.getUpperRightY() - margin - headerHeight;
            } else {
                // Fallback: no logo, behave like before
                this.y = mediaBox.getUpperRightY() - margin;
            }
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight <= bottomMargin) {
                newPage();
            }
        }

        public void setFont(PDType1Font font, float fontSize) {
            this.font = font;
            this.fontSize = fontSize;
            this.leading = Math.max(12f, fontSize + 4f);
        }

        // Rendering helper: section header with consistent spacing.
        public void writeSectionHeader(String title) throws IOException {
            if (title == null) {
                title = "";
            }
            newLine();
            setFont(PDType1Font.HELVETICA_BOLD, 12);
            writeLine(title);
            setFont(PDType1Font.HELVETICA, 10);
            newLine();
        }

        // Rendering helper: aligned key-value pairs using fixed X offsets.
        public void writeKeyValueAligned(String key, String value) throws IOException {
            String safeKey = key == null ? "" : key;
            String safeValue = value == null ? "" : value;

            ensureSpace(leading);

            float labelWidth = 140f;
            drawTextAt(margin, y, safeKey + ":", font, fontSize);
            drawTextAt(margin + labelWidth, y, safeValue, font, fontSize);
            y -= leading;
        }

        // Rendering helper: design-selected items table.
        public void drawDesignSelectedTableHeader() throws IOException {
            ensureSpace(leading * 2);

            float x = margin;
            float[] cols = designTableColumns(x);

            setFont(PDType1Font.HELVETICA_BOLD, 8);
            drawTextAt(cols[0], y, "No.", font, fontSize);
            drawTextAt(cols[1], y, "Size X", font, fontSize);
            drawTextAt(cols[2], y, "Size Y", font, fontSize);
            drawTextAt(cols[3], y, "Material", font, fontSize);
            drawTextAt(cols[4], y, "Thk", font, fontSize);
            drawTextAt(cols[5], y, "Time/inst.", font, fontSize);
            drawTextAt(cols[6], y, "Total time", font, fontSize);
            drawTextAt(cols[7], y, "NC file", font, fontSize);
            drawTextAt(cols[8], y, "Qty", font, fontSize);
            drawTextAt(cols[9], y, "Area (m²)", font, fontSize);
            drawTextAt(cols[10], y, "Eff.%", font, fontSize);

            float lineY = y - 2f;
            content.moveTo(margin, lineY);
            content.lineTo(margin + 530f, lineY);
            content.stroke();

            y -= leading;
            setFont(PDType1Font.HELVETICA, 8);
        }

        public void drawDesignSelectedTableRow(
                int index,
                String sizeX,
                String sizeY,
                String material,
                String thickness,
                String timePerInst,
                String totalTime,
                String ncFile,
                String qty,
                String area,
                String eff
        ) throws IOException {
            float rowHeight = leading;
            ensureSpaceForDesignTableRow(rowHeight);

            float[] cols = designTableColumns(margin);
            drawTextAt(cols[0], y, String.valueOf(index), font, fontSize);
            drawTextAt(cols[1], y, safeCell(sizeX), font, fontSize);
            drawTextAt(cols[2], y, safeCell(sizeY), font, fontSize);
            drawTextAt(cols[3], y, safeCell(material), font, fontSize);
            drawTextAt(cols[4], y, safeCell(thickness), font, fontSize);
            drawTextAt(cols[5], y, safeCell(timePerInst), font, fontSize);
            drawTextAt(cols[6], y, safeCell(totalTime), font, fontSize);
            drawTextAt(cols[7], y, safeCell(ncFile), font, fontSize);
            drawTextAt(cols[8], y, safeCell(qty), font, fontSize);
            drawTextAt(cols[9], y, safeCell(area), font, fontSize);
            drawTextAt(cols[10], y, safeCell(eff), font, fontSize);

            y -= rowHeight;
        }

        private void ensureSpaceForDesignTableRow(float rowHeight) throws IOException {
            if (y - rowHeight <= bottomMargin) {
                newPage();
                drawDesignSelectedTableHeader();
            }
        }

        private float[] designTableColumns(float startX) {
            // Fixed X offsets relative to margin.
            return new float[]{
                    startX + 0f,   // No.
                    startX + 24f,  // Size X
                    startX + 62f,  // Size Y
                    startX + 100f, // Material
                    startX + 160f, // Thk
                    startX + 188f, // Time / inst.
                    startX + 245f, // Total time
                    startX + 310f, // NC file
                    startX + 410f, // Qty
                    startX + 440f, // Area
                    startX + 500f  // Eff.
            };
        }

        private void drawTextAt(float x, float y, String text, PDType1Font font, float size) throws IOException {
            String safeText = text == null ? "" : text;
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(trimToFit(safeText, 28));
            content.endText();
        }

        private String safeCell(String value) {
            if (value == null || value.isBlank()) {
                return "—";
            }
            return value;
        }

        private String trimToFit(String text, int maxChars) {
            if (text == null) {
                return "";
            }
            String t = text.replace("\n", " ").replace("\r", " ");
            if (t.length() <= maxChars) {
                return t;
            }
            return t.substring(0, Math.max(0, maxChars - 1)) + "…";
        }

        public void writeLine(String text) throws IOException {
            if (text == null) {
                text = "";
            }
            List<String> lines = wrapText(text, 90);
            for (String line : lines) {
                ensureSpace(leading);
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(margin, y);
                content.showText(line);
                content.endText();
                y -= leading;
            }
        }

        public void writeKeyValue(String key, String value) throws IOException {
            writeLine(key + ": " + (value == null ? "" : value));
        }

        public void newLine() throws IOException {
            ensureSpace(leading);
            y -= leading;
        }

        public void close() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }

        private List<String> wrapText(String text, int maxChars) {
            if (text.length() <= maxChars) {
                return List.of(text);
            }
            List<String> result = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String w : words) {
                if (current.length() == 0) {
                    current.append(w);
                } else if (current.length() + 1 + w.length() <= maxChars) {
                    current.append(' ').append(w);
                } else {
                    result.add(current.toString());
                    current = new StringBuilder(w);
                }
            }
            if (current.length() > 0) {
                result.add(current.toString());
            }
            return result;
        }
    }
}
