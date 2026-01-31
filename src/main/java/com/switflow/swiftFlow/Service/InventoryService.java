package com.switflow.swiftFlow.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.switflow.swiftFlow.Entity.InventoryInwardTxn;
import com.switflow.swiftFlow.Entity.InventoryMaterialMaster;
import com.switflow.swiftFlow.Entity.InventoryOutwardTxn;
import com.switflow.swiftFlow.Repo.InventoryInwardTxnRepository;
import com.switflow.swiftFlow.Repo.InventoryMaterialMasterRepository;
import com.switflow.swiftFlow.Repo.InventoryOutwardTxnRepository;
import com.switflow.swiftFlow.Request.InventoryInwardRequest;
import com.switflow.swiftFlow.Request.InventoryOutwardRequest;
import com.switflow.swiftFlow.Response.InventoryDashboardResponse;
import com.switflow.swiftFlow.Response.InventoryDashboardResponse.InwardEntry;
import com.switflow.swiftFlow.Response.InventoryDashboardResponse.OutwardEntry;
import com.switflow.swiftFlow.Response.InventoryDashboardResponse.TotalInventoryEntry;

@Service
public class InventoryService {

    @Autowired
    private InventoryMaterialMasterRepository materialRepository;

    @Autowired
    private InventoryInwardTxnRepository inwardRepository;

    @Autowired
    private InventoryOutwardTxnRepository outwardRepository;

    private final Random random = new Random();

    public InventoryDashboardResponse getDashboard() {
        List<InventoryInwardTxn> latestInward = inwardRepository.findTop5ByOrderByDateTimeDesc();
        List<InventoryOutwardTxn> latestOutward = outwardRepository.findAllByOrderByDateTimeDesc();
        List<InventoryMaterialMaster> materials = materialRepository.findAll();

        System.out.println("[InventoryDashboard] latestInward size=" + (latestInward != null ? latestInward.size() : 0));
        System.out.println("[InventoryDashboard] latestOutward size=" + (latestOutward != null ? latestOutward.size() : 0));
        System.out.println("[InventoryDashboard] totalInventory size=" + (materials != null ? materials.size() : 0));

        InventoryDashboardResponse response = new InventoryDashboardResponse();

        response.setLatestInward(latestInward.stream().map(txn -> {
            InwardEntry entry = new InwardEntry();
            entry.setId(txn.getId());
            entry.setDateTime(txn.getDateTime());
            entry.setSupplier(txn.getSupplier());
            entry.setMaterialName(txn.getMaterial().getMaterialName());
            entry.setSheetSize(txn.getMaterial().getSheetSize());
            entry.setThickness(txn.getMaterial().getThickness());
            entry.setQuantity(txn.getQuantity());
            entry.setRemarkUnique(txn.getRemarkUnique());
            return entry;
        }).collect(Collectors.toList()));

        response.setLatestOutward(latestOutward.stream().map(txn -> {
            OutwardEntry entry = new OutwardEntry();
            entry.setId(txn.getId());
            entry.setDateTime(txn.getDateTime());
            entry.setCustomer(txn.getCustomer());
            entry.setMaterialName(txn.getMaterial().getMaterialName());
            entry.setSheetSize(txn.getMaterial().getSheetSize());
            entry.setThickness(txn.getMaterial().getThickness());
            entry.setQuantity(txn.getQuantity());
            entry.setRemarkUnique(txn.getRemarkUnique());
            return entry;
        }).collect(Collectors.toList()));

        response.setTotalInventory(materials.stream()
                .sorted(Comparator.comparing(InventoryMaterialMaster::getMaterialName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(InventoryMaterialMaster::getThickness, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(InventoryMaterialMaster::getSheetSize, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
                .map(mat -> {
            TotalInventoryEntry entry = new TotalInventoryEntry();
            entry.setId(mat.getId());
            entry.setMaterialName(mat.getMaterialName());
            entry.setThickness(mat.getThickness());
            entry.setSheetSize(mat.getSheetSize());
            entry.setQuantity(mat.getQuantity());
            entry.setLocation(mat.getLocation());
            entry.setDefaultSupplier(mat.getDefaultSupplier());
            return entry;
        }).collect(Collectors.toList()));

        return response;
    }

    @Transactional
    public void createInward(InventoryInwardRequest request) {
        InventoryMaterialMaster material = materialRepository
                .findByMaterialNameAndThicknessAndSheetSize(request.getMaterialName(), request.getThickness(),
                        request.getSheetSize())
                .orElseGet(() -> {
                    InventoryMaterialMaster m = new InventoryMaterialMaster();
                    m.setMaterialName(request.getMaterialName());
                    m.setThickness(request.getThickness());
                    m.setSheetSize(request.getSheetSize());
                    if (StringUtils.hasText(request.getLocation())) {
                        m.setLocation(request.getLocation());
                    }
                    if (StringUtils.hasText(request.getSupplier())) {
                        m.setDefaultSupplier(request.getSupplier());
                    }
                    m.setQuantity(0);
                    return m;
                });

        int newQty = (material.getQuantity() == null ? 0 : material.getQuantity())
                + (request.getQuantity() == null ? 0 : request.getQuantity());
        material.setQuantity(newQty);
        if (StringUtils.hasText(request.getLocation())) {
            material.setLocation(request.getLocation());
        }
        if (StringUtils.hasText(request.getSupplier())) {
            material.setDefaultSupplier(request.getSupplier());
        }

        InventoryMaterialMaster savedMaterial = materialRepository.save(material);

        InventoryInwardTxn txn = new InventoryInwardTxn();
        txn.setSupplier(request.getSupplier());
        txn.setMaterial(savedMaterial);
        txn.setQuantity(request.getQuantity());
        txn.setDateTime(LocalDateTime.now());
        String inwardRemark = request.getRemarkUnique();
        if (!StringUtils.hasText(inwardRemark) || inwardRepository.existsByRemarkUnique(inwardRemark)) {
            inwardRemark = generateUniqueRemark("INW");
        }
        txn.setRemarkUnique(inwardRemark);

        inwardRepository.save(txn);
    }

    @Transactional
    public void createOutward(InventoryOutwardRequest request) {
        InventoryMaterialMaster material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("Material not found"));

        int currentQty = material.getQuantity() == null ? 0 : material.getQuantity();
        int requestedQty = request.getQuantity() == null ? 0 : request.getQuantity();

        if (requestedQty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (requestedQty > currentQty) {
            throw new IllegalStateException("Insufficient stock");
        }

        material.setQuantity(currentQty - requestedQty);
        materialRepository.save(material);

        InventoryOutwardTxn txn = new InventoryOutwardTxn();
        txn.setCustomer(request.getCustomer());
        txn.setMaterial(material);
        txn.setQuantity(request.getQuantity());
        txn.setDateTime(LocalDateTime.now());
        String outwardRemark = request.getRemarkUnique();
        if (!StringUtils.hasText(outwardRemark) || outwardRepository.existsByRemarkUnique(outwardRemark)) {
            outwardRemark = generateUniqueRemark("OUT");
        }
        txn.setRemarkUnique(outwardRemark);

        outwardRepository.save(txn);
    }

    @Transactional
    public void updateInward(Long id, InventoryInwardRequest request) {
        InventoryInwardTxn existing = inwardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inward transaction not found"));

        InventoryMaterialMaster oldMaterial = existing.getMaterial();
        int oldQty = existing.getQuantity() == null ? 0 : existing.getQuantity();

        // Resolve new material (may be same or different)
        InventoryMaterialMaster newMaterial = materialRepository
                .findByMaterialNameAndThicknessAndSheetSize(request.getMaterialName(), request.getThickness(),
                        request.getSheetSize())
                .orElseGet(() -> {
                    InventoryMaterialMaster m = new InventoryMaterialMaster();
                    m.setMaterialName(request.getMaterialName());
                    m.setThickness(request.getThickness());
                    m.setSheetSize(request.getSheetSize());
                    if (StringUtils.hasText(request.getLocation())) {
                        m.setLocation(request.getLocation());
                    }
                    if (StringUtils.hasText(request.getSupplier())) {
                        m.setDefaultSupplier(request.getSupplier());
                    }
                    m.setQuantity(0);
                    return m;
                });

        int newQty = request.getQuantity() == null ? 0 : request.getQuantity();
        if (newQty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Stock adjustment rules
        if (oldMaterial.getId().equals(newMaterial.getId())) {
            int currentStock = oldMaterial.getQuantity() == null ? 0 : oldMaterial.getQuantity();
            int delta = newQty - oldQty;
            int result = currentStock + delta;
            if (result < 0) {
                throw new IllegalArgumentException("Insufficient stock for update");
            }
            oldMaterial.setQuantity(result);
            materialRepository.save(oldMaterial);
        } else {
            int oldStock = oldMaterial.getQuantity() == null ? 0 : oldMaterial.getQuantity();
            if (oldStock - oldQty < 0) {
                throw new IllegalArgumentException("Insufficient stock for update");
            }
            oldMaterial.setQuantity(oldStock - oldQty);
            materialRepository.save(oldMaterial);

            int newStock = newMaterial.getQuantity() == null ? 0 : newMaterial.getQuantity();
            newMaterial.setQuantity(newStock + newQty);
            if (StringUtils.hasText(request.getLocation())) {
                newMaterial.setLocation(request.getLocation());
            }
            if (StringUtils.hasText(request.getSupplier())) {
                newMaterial.setDefaultSupplier(request.getSupplier());
            }
            materialRepository.save(newMaterial);
        }

        // Update transaction fields
        existing.setSupplier(request.getSupplier());
        existing.setQuantity(newQty);
        existing.setMaterial(newMaterial);
        inwardRepository.save(existing);
    }

    @Transactional
    public void deleteInward(Long id) {
        InventoryInwardTxn existing = inwardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inward transaction not found"));

        InventoryMaterialMaster material = existing.getMaterial();
        int qty = existing.getQuantity() == null ? 0 : existing.getQuantity();
        int currentStock = material.getQuantity() == null ? 0 : material.getQuantity();
        int result = currentStock - qty;
        if (result < 0) {
            throw new IllegalArgumentException("Insufficient stock for delete");
        }
        material.setQuantity(result);
        materialRepository.save(material);

        inwardRepository.delete(existing);
    }

    @Transactional
    public void updateOutward(Long id, InventoryOutwardRequest request) {
        InventoryOutwardTxn existing = outwardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outward transaction not found"));

        InventoryMaterialMaster oldMaterial = existing.getMaterial();
        int oldQty = existing.getQuantity() == null ? 0 : existing.getQuantity();

        int newQty = request.getQuantity() == null ? 0 : request.getQuantity();
        if (newQty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Revert old outward
        int oldStock = oldMaterial.getQuantity() == null ? 0 : oldMaterial.getQuantity();
        oldMaterial.setQuantity(oldStock + oldQty);
        materialRepository.save(oldMaterial);

        // Resolve new material
        InventoryMaterialMaster newMaterial = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("Material not found"));

        int available = newMaterial.getQuantity() == null ? 0 : newMaterial.getQuantity();
        if (available < newQty) {
            throw new IllegalStateException("Insufficient stock");
        }

        newMaterial.setQuantity(available - newQty);
        materialRepository.save(newMaterial);

        existing.setCustomer(request.getCustomer());
        existing.setQuantity(newQty);
        existing.setMaterial(newMaterial);
        outwardRepository.save(existing);
    }

    @Transactional
    public void deleteOutward(Long id) {
        InventoryOutwardTxn existing = outwardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outward transaction not found"));

        InventoryMaterialMaster material = existing.getMaterial();
        int qty = existing.getQuantity() == null ? 0 : existing.getQuantity();
        int currentStock = material.getQuantity() == null ? 0 : material.getQuantity();
        material.setQuantity(currentStock + qty);
        materialRepository.save(material);

        outwardRepository.delete(existing);
    }

    private String generateUniqueRemark(String prefix) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDate.now().format(formatter);

        String remark;
        boolean exists;

        do {
            int sequence = 1000 + random.nextInt(9000);
            remark = prefix + "-" + datePart + "-" + sequence;
            if ("INW".equals(prefix)) {
                exists = inwardRepository.existsByRemarkUnique(remark);
            } else {
                exists = outwardRepository.existsByRemarkUnique(remark);
            }
        } while (exists);

        return remark;
    }

    public String generateNewRemark(String type) {
        String prefix = "OUT";
        if ("INW".equalsIgnoreCase(type)) {
            prefix = "INW";
        }
        return generateUniqueRemark(prefix);
    }
}
