package com.example.mod;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReportRepo extends JpaRepository<Report, Long> {
    @Query("select r from Report r where (:status is null or r.status = :status) and (:category is null or r.category = :category)")
    Page<Report> search(@Param("status") String status, @Param("category") String category, Pageable pageable);

    @Query("select r.category, count(r) from Report r group by r.category")
    List<Object[]> countByCategory();

    @Query("select r.status, count(r) from Report r group by r.status")
    List<Object[]> countByStatus();

    long countByPriority(int priority);
}
