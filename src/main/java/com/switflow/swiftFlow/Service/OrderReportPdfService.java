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
import com.switflow.swiftFlow.utility.Department;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] generateReportPdf(long orderId, ReportType reportType) throws IOException {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfTextCursor cursor = new PdfTextCursor(doc);
            try {
                cursor.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cursor.writeLine(buildReportTitle(order, reportType));
                cursor.newLine();

                cursor.setFont(PDType1Font.HELVETICA, 10);
                writeSharedHeader(cursor, order, statuses);
                cursor.newLine();

                writeRoleSpecificSection(cursor, order, statuses, reportType);
            } finally {
                cursor.close();
            }

            doc.save(out);
            return out.toByteArray();
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

    private void writeSharedHeader(PdfTextCursor cursor, Orders order, List<Status> statuses) throws IOException {
        cursor.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cursor.writeLine("Production stage / order progress");
        cursor.setFont(PDType1Font.HELVETICA, 10);

        Department currentStage = order.getDepartment();
        String progressPercentage = findLatestProgressPercentage(statuses).orElse("—");
        cursor.writeKeyValue("Current stage", currentStage != null ? currentStage.name() : "—");
        cursor.writeKeyValue("Progress percentage", progressPercentage);
        cursor.newLine();

        cursor.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cursor.writeLine("Project Details");
        cursor.setFont(PDType1Font.HELVETICA, 10);
        cursor.writeKeyValue("Units", safe(order.getUnits()));
        cursor.writeKeyValue("Material", safe(order.getMaterial()));

        Customer customer = (order.getCustomers() != null && !order.getCustomers().isEmpty())
                ? order.getCustomers().get(0)
                : null;
        cursor.writeKeyValue("Customer", customer != null ? safe(customer.getCustomerName()) : "—");
        cursor.writeKeyValue("Billing Address", customer != null ? safe(customer.getBillingAddress()) : "—");
        cursor.writeKeyValue("Shipping Address", customer != null ? safe(customer.getShippingAddress()) : "—");
        cursor.newLine();

        cursor.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cursor.writeLine("Design-selected checkbox data");
        cursor.setFont(PDType1Font.HELVETICA, 10);

        List<JsonNode> selectedItems = extractSelectedItemsPreferDepartments(statuses, Department.PRODUCTION, Department.DESIGN);
        if (selectedItems.isEmpty()) {
            cursor.writeLine("No design selection details available");
            return;
        }

        cursor.writeLine("Design-selected parts:");
        cursor.newLine();

        for (JsonNode item : selectedItems) {
            String ncFile = textOrEmpty(item.get("ncFile"));
            String material = textOrEmpty(item.get("material"));
            String thickness = textOrEmpty(item.get("thickness"));
            String sizeX = textOrEmpty(item.get("sizeX"));
            String sizeY = textOrEmpty(item.get("sizeY"));
            String qty = textOrEmpty(item.get("quantity"));
            String area = textOrEmpty(item.get("area"));
            String time = textOrEmpty(item.get("time"));

            cursor.writeLine("• NC File: " + safeInline(ncFile));
            cursor.writeLine("  Material: " + safeInline(material) + " | Thk: " + safeInline(thickness));
            cursor.writeLine("  Size: " + safeInline(sizeX) + " x " + safeInline(sizeY));
            cursor.writeLine("  Qty: " + safeInline(qty) + " | Area: " + safeInline(area));
            cursor.writeLine("  Time: " + safeInline(time));
            cursor.newLine();
        }
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
                .filter(s -> s.getNewStatus() == department && s.getComment() != null && s.getComment().contains("selectedItems"))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(latest.getComment());
            JsonNode arr = root.get("selectedItems");
            if (arr == null || !arr.isArray() || arr.isEmpty()) {
                return List.of();
            }
            List<JsonNode> items = new ArrayList<>();
            for (JsonNode n : arr) {
                if (n != null && n.isObject()) {
                    items.add(n);
                }
            }
            return items;
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
        cursor.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cursor.writeLine("Report Details");
        cursor.setFont(PDType1Font.HELVETICA, 10);

        switch (reportType) {
            case DESIGN -> {
                cursor.writeLine("Design-specific data");
                cursor.writeKeyValue("Custom product details", safe(order.getCustomProductDetails()));
                cursor.writeKeyValue("Product details", safe(order.getProductDetails()));
                List<Product> products = order.getProducts();
                if (products != null && !products.isEmpty()) {
                    String productCodes = products.stream()
                            .map(Product::getProductCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "));
                    cursor.writeKeyValue("Product codes", productCodes.isBlank() ? "—" : productCodes);
                }
                writeLatestStatusSummary(cursor, statuses, Department.DESIGN);
            }
            case PRODUCTION -> {
                cursor.writeLine("Production-specific data");
                List<String> productionSelection = extractSelectedRowIdsForDepartment(statuses, Department.PRODUCTION);
                cursor.writeKeyValue("Selected items count", String.valueOf(productionSelection.size()));
                if (!productionSelection.isEmpty()) {
                    cursor.writeLine("Selected items:");
                    for (String id : productionSelection) {
                        cursor.writeLine("- " + id);
                    }
                }
                writeLatestStatusSummary(cursor, statuses, Department.PRODUCTION);
            }
            case MACHINISTS -> {
                cursor.writeLine("Machinists-specific data");
                List<String> machineSelection = extractSelectedRowIdsForDepartment(statuses, Department.MACHINING);
                cursor.writeKeyValue("Selected items count", String.valueOf(machineSelection.size()));
                if (!machineSelection.isEmpty()) {
                    cursor.writeLine("Selected items:");
                    for (String id : machineSelection) {
                        cursor.writeLine("- " + id);
                    }
                }
                writeLatestStatusSummary(cursor, statuses, Department.MACHINING);
            }
            case INSPECTION -> {
                cursor.writeLine("Inspection-specific data");
                List<String> inspectionSelection = extractSelectedRowIdsForDepartment(statuses, Department.INSPECTION);
                cursor.writeKeyValue("Selected items count", String.valueOf(inspectionSelection.size()));
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
            cursor.writeKeyValue("Latest comment", latest.getComment());
        }
        if (latest.getAttachmentUrl() != null && !latest.getAttachmentUrl().isBlank()) {
            cursor.writeKeyValue("Attachment", latest.getAttachmentUrl());
        }
        if (latest.getCreatedAt() != null && !latest.getCreatedAt().isBlank()) {
            cursor.writeKeyValue("Updated at", latest.getCreatedAt());
        }
        if (latest.getPercentage() != null && !latest.getPercentage().isBlank()) {
            cursor.writeKeyValue("Progress percentage", latest.getPercentage());
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

        private PdfTextCursor(PDDocument doc) throws IOException {
            this.doc = doc;
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
            this.y = mediaBox.getUpperRightY() - margin;
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
