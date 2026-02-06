package com.switflow.swiftFlow.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.switflow.swiftFlow.Entity.InventoryOutwardTxn;
import com.switflow.swiftFlow.Entity.InventoryMaterialMaster;

@Repository
public interface InventoryOutwardTxnRepository extends JpaRepository<InventoryOutwardTxn, Long> {

    List<InventoryOutwardTxn> findAllByOrderByDateTimeDesc();

    boolean existsByRemarkUnique(String remarkUnique);

    boolean existsByMaterial(InventoryMaterialMaster material);
}
