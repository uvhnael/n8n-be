package org.uvhnael.fbadsbe2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uvhnael.fbadsbe2.model.entity.Keyword;

import java.util.List;

public interface KeywordsRepository extends JpaRepository<Keyword, Long> {
    
    List<Keyword> findByInsightIdOrderByCountDesc(Long insightId);
}
