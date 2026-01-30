package com.switflow.swiftFlow.Repo;

import com.switflow.swiftFlow.Entity.OrderCheckboxBaseSelection;
import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderCheckboxBaseSelectionRepository extends JpaRepository<OrderCheckboxBaseSelection, Long> {

    List<OrderCheckboxBaseSelection> findByOrderIdAndPdfTypeAndScope(Long orderId, PdfType pdfType, SelectionScope scope);

    boolean existsByOrderIdAndPdfTypeAndScopeAndRowKey(Long orderId, PdfType pdfType, SelectionScope scope, String rowKey);

    void deleteByOrderIdAndPdfTypeAndScope(Long orderId, PdfType pdfType, SelectionScope scope);
}
