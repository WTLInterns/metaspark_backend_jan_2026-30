package com.switflow.swiftFlow.Repo;

import com.switflow.swiftFlow.Entity.OrderCheckboxAssignment;
import com.switflow.swiftFlow.utility.PdfType;
import com.switflow.swiftFlow.utility.SelectionScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderCheckboxAssignmentRepository extends JpaRepository<OrderCheckboxAssignment, Long> {

    List<OrderCheckboxAssignment> findByOrderIdAndPdfTypeAndScope(Long orderId, PdfType pdfType, SelectionScope scope);

    List<OrderCheckboxAssignment> findByOrderIdAndPdfTypeAndScopeAndAssignedToUserId(Long orderId, PdfType pdfType, SelectionScope scope, Long assignedToUserId);
}
