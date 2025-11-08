package org.uvhnael.fbadsbe2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uvhnael.fbadsbe2.model.entity.Insight;

public interface InsightsRepository extends JpaRepository<Insight, Long> {
}
