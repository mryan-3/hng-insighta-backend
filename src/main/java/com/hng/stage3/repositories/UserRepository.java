package com.hng.stage3.repositories;

import com.hng.stage3.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByGithubId(String githubId);
    Optional<User> findByUsername(String username);
    boolean existsByGithubId(String githubId);
}
