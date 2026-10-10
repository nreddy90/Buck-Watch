package com.buckwatch.repository;

import com.buckwatch.model.FinanceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinanceEventRepository extends JpaRepository<FinanceEvent, Long> {
}
