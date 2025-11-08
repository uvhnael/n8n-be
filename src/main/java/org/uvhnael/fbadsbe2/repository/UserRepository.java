package org.uvhnael.fbadsbe2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uvhnael.fbadsbe2.model.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
