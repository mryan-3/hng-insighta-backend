package com.hng.stage3.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.hng.stage3.entities.Profile;
import com.hng.stage3.repositories.ProfileRepository;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {
    private final ProfileRepository profileRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final TimeBasedEpochGenerator uuidGenerator = Generators.timeBasedEpochGenerator();

    @PostConstruct
    public void seedData() {
        try {
            Resource resource = resourceLoader.getResource("classpath:static/seed_profiles.json");
            try (InputStream inputStream = resource.getInputStream()) {
                ProfileListWrapper wrapper = objectMapper.readValue(inputStream, ProfileListWrapper.class);
                List<Profile> seedProfiles = wrapper.getProfiles();
                
                long count = 0;
                for (Profile seed : seedProfiles) {
                    if (!profileRepository.existsByName(seed.getName())) {
                        seed.setId(uuidGenerator.generate());
                        seed.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                        profileRepository.save(seed);
                        count++;
                    }
                }
                log.info("Seeding completed. Added {} new profiles.", count);
            }
        } catch (Exception e) {
            log.error("Failed to seed data: {}", e.getMessage());
        }
    }

    @Data
    private static class ProfileListWrapper {
        private List<Profile> profiles;
    }
}
