package org.uvhnael.fbadsbe2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uvhnael.fbadsbe2.model.entity.Insight;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InsightsRepository extends JpaRepository<Insight, Long> {
    
    Optional<Insight> findTopByOrderByCreatedAtDesc();
    
    List<Insight> findByReportDateBetween(LocalDate startDate, LocalDate endDate);
}
