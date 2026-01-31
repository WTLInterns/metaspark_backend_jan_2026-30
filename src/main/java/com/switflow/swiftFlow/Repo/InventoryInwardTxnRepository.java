package com.switflow.swiftFlow.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.switflow.swiftFlow.Entity.InventoryInwardTxn;

@Repository
public interface InventoryInwardTxnRepository extends JpaRepository<InventoryInwardTxn, Long> {

    List<InventoryInwardTxn> findTop5ByOrderByDateTimeDesc();

    boolean existsByRemarkUnique(String remarkUnique);
}
