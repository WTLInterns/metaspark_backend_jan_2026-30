package com.switflow.swiftFlow.Repo;

import com.switflow.swiftFlow.Entity.Machines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineRepository extends JpaRepository<Machines, Integer> {
}
