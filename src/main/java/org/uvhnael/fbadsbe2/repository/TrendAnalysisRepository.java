package org.uvhnael.fbadsbe2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uvhnael.fbadsbe2.model.entity.TrendAnalysis;

import java.time.LocalDate;
import java.util.Optional;

public interface TrendAnalysisRepository extends JpaRepository<TrendAnalysis, Long> {
    Optional<TrendAnalysis> findTopByOrderByCreatedAtDesc();
    Optional<TrendAnalysis> findByAnalysisDate(LocalDate date);
    Optional<TrendAnalysis>  findTopByOrderByAnalysisDateDesc();
}
