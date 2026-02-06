package com.switflow.swiftFlow.Repo;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.switflow.swiftFlow.Entity.InventoryMaterialMaster;

@Repository
public interface InventoryMaterialMasterRepository extends JpaRepository<InventoryMaterialMaster, Long> {

    Optional<InventoryMaterialMaster> findByMaterialNameAndThicknessAndSheetSize(String materialName, String thickness,
            String sheetSize);

    List<InventoryMaterialMaster> findByIsActiveTrue();
}
