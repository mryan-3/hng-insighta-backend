package com.hng.stage3.services;

import com.hng.stage3.dto.PagedResponse;
import com.hng.stage3.entities.Profile;
import com.hng.stage3.repositories.ProfileRepository;
import com.hng.stage3.repositories.ProfileSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;

    public PagedResponse<Profile> getProfiles(
            String gender, String ageGroup, String countryId,
            Integer minAge, Integer maxAge,
            Double minGenderProb, Double minCountryProb,
            String sortBy, String order,
            int page, int limit
    ) {
        // 1. Validation: Page must be >= 1
        int validatedPage = Math.max(page, 1);
        
        // 2. Validation: Limit max 50
        int validatedLimit = Math.min(limit, 50);
        if (validatedLimit <= 0) validatedLimit = 10;

        // 3. Validation: SortBy fields mapping (JSON key -> Java Field)
        String validatedSortBy;
        if ("age".equalsIgnoreCase(sortBy)) {
            validatedSortBy = "age";
        } else if ("gender_probability".equalsIgnoreCase(sortBy)) {
            validatedSortBy = "genderProbability";
        } else {
            // Default or explicit created_at
            validatedSortBy = "createdAt";
        }

        // 4. Sort order
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, Sort.by(direction, validatedSortBy));

        // 5. Combine Specifications
        Specification<Profile> spec = Specification.where(ProfileSpecification.hasGender(gender))
                .and(ProfileSpecification.hasAgeGroup(ageGroup))
                .and(ProfileSpecification.hasCountryId(countryId))
                .and(ProfileSpecification.ageGreaterThanOrEqualTo(minAge))
                .and(ProfileSpecification.ageLessThanOrEqualTo(maxAge))
                .and(ProfileSpecification.genderProbabilityGreaterThanOrEqualTo(minGenderProb))
                .and(ProfileSpecification.countryProbabilityGreaterThanOrEqualTo(minCountryProb));

        Page<Profile> profilePage = profileRepository.findAll(spec, pageable);

        return PagedResponse.success(
                profilePage.getContent(),
                validatedPage,
                validatedLimit,
                profilePage.getTotalElements()
        );
    }
}
