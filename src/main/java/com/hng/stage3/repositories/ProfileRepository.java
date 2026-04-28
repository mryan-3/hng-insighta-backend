package com.hng.stage3.repositories;

import com.hng.stage3.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {
    boolean existsByName(String name);
    java.util.Optional<Profile> findByNameIgnoreCase(String name);
}
