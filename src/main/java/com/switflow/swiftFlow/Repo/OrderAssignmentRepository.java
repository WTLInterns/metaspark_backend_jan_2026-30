package com.switflow.swiftFlow.Repo;

import com.switflow.swiftFlow.Entity.OrderAssignment;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {
    
    List<OrderAssignment> findByUserId(Long userId);
    
    List<OrderAssignment> findByOrderId(Long orderId);
    
    Optional<OrderAssignment> findByUserIdAndOrderId(Long userId, Long orderId);
    
    Optional<OrderAssignment> findByOrderIdAndDepartment(Long orderId, Department department);
    
    @Query("SELECT oa FROM OrderAssignment oa WHERE oa.user.id = :userId AND oa.department = :department")
    List<OrderAssignment> findByUserIdAndDepartment(@Param("userId") Long userId, @Param("department") Department department);
    
    @Query("SELECT DISTINCT oa.orderId FROM OrderAssignment oa WHERE oa.user.id = :userId")
    List<Long> findOrderIdsByUserId(@Param("userId") Long userId);
    
    void deleteByUserIdAndOrderId(Long userId, Long orderId);

    void deleteByOrderIdAndDepartment(Long orderId, Department department);
}
