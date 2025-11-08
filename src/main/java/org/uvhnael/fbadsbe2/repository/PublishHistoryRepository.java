package org.uvhnael.fbadsbe2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uvhnael.fbadsbe2.model.entity.PublishHistory;

public interface PublishHistoryRepository extends JpaRepository<PublishHistory, Long> {
}
