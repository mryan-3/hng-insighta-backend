package com.hng.stage3.services;

import com.hng.stage3.dto.ExternalApiDtos.*;
import com.hng.stage3.dto.PagedResponse;
import com.hng.stage3.entities.Profile;
import com.hng.stage3.repositories.ProfileRepository;
import com.hng.stage3.repositories.ProfileSpecification;
import com.hng.stage3.utils.CountryCodeMapper;
import com.hng.stage3.utils.UuidUtils;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final RestTemplate restTemplate;
    private final CountryCodeMapper countryCodeMapper;

    public Profile createProfile(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or empty name");
        }

        Optional<Profile> existing = profileRepository.findByNameIgnoreCase(name.trim());
        if (existing.isPresent()) {
            return existing.get();
        }

        GenderizeResponse genderData = fetchGender(name);
        AgifyResponse ageData = fetchAge(name);
        NationalizeResponse countryData = fetchCountry(name);

        String ageGroup = classifyAgeGroup(ageData.getAge() != null ? ageData.getAge() : 0);

        Country topCountry = countryData.getCountry().stream()
                .max(Comparator.comparing(Country::getProbability))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nationalize returned no country data"));

        Profile profile = Profile.builder()
                .id(UuidUtils.generateV7())
                .name(name.trim())
                .gender(genderData.getGender())
                .genderProbability(genderData.getProbability())
                .age(ageData.getAge())
                .ageGroup(ageGroup)
                .countryId(topCountry.getCountryId())
                .countryName(countryCodeMapper.getCountryName(topCountry.getCountryId()))
                .countryProbability(topCountry.getProbability())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return profileRepository.save(profile);
    }

    public PagedResponse<Profile> getProfiles(
            String gender, String ageGroup, String countryId,
            Integer minAge, Integer maxAge,
            Double minGenderProb, Double minCountryProb,
            String sortBy, String order,
            int page, int limit,
            String baseUri
    ) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.min(limit, 50);
        if (validatedLimit <= 0) validatedLimit = 10;

        String validatedSortBy;
        if ("age".equalsIgnoreCase(sortBy)) {
            validatedSortBy = "age";
        } else if ("gender_probability".equalsIgnoreCase(sortBy)) {
            validatedSortBy = "genderProbability";
        } else {
            validatedSortBy = "createdAt";
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, Sort.by(direction, validatedSortBy));

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
                profilePage.getTotalElements(),
                profilePage.getTotalPages(),
                generateLinks(baseUri, validatedPage, validatedLimit, profilePage.getTotalPages())
        );
    }

    private Map<String, String> generateLinks(String baseUri, int page, int limit, int totalPages) {
        Map<String, String> links = new HashMap<>();
        links.put("self", String.format("%s?page=%d&limit=%d", baseUri, page, limit));
        
        if (page < totalPages) {
            links.put("next", String.format("%s?page=%d&limit=%d", baseUri, page + 1, limit));
        } else {
            links.put("next", null);
        }
        
        if (page > 1) {
            links.put("prev", String.format("%s?page=%d&limit=%d", baseUri, page - 1, limit));
        } else {
            links.put("prev", null);
        }
        
        return links;
    }

    private GenderizeResponse fetchGender(String name) {
        String url = "https://api.genderize.io?name=" + name;
        try {
            return restTemplate.getForObject(url, GenderizeResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Genderize API failed");
        }
    }

    private AgifyResponse fetchAge(String name) {
        String url = "https://api.agify.io?name=" + name;
        try {
            return restTemplate.getForObject(url, AgifyResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agify API failed");
        }
    }

    private NationalizeResponse fetchCountry(String name) {
        String url = "https://api.nationalize.io?name=" + name;
        try {
            return restTemplate.getForObject(url, NationalizeResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nationalize API failed");
        }
    }

    public byte[] exportProfilesToCsv(
            String gender, String ageGroup, String countryId,
            Integer minAge, Integer maxAge,
            Double minGenderProb, Double minCountryProb,
            String sortBy, String order
    ) {
        String validatedSortBy;
        if ("age".equalsIgnoreCase(sortBy)) {
            validatedSortBy = "age";
        } else if ("gender_probability".equalsIgnoreCase(sortBy)) {
            validatedSortBy = "genderProbability";
        } else {
            validatedSortBy = "createdAt";
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, validatedSortBy);

        Specification<Profile> spec = Specification.where(ProfileSpecification.hasGender(gender))
                .and(ProfileSpecification.hasAgeGroup(ageGroup))
                .and(ProfileSpecification.hasCountryId(countryId))
                .and(ProfileSpecification.ageGreaterThanOrEqualTo(minAge))
                .and(ProfileSpecification.ageLessThanOrEqualTo(maxAge))
                .and(ProfileSpecification.genderProbabilityGreaterThanOrEqualTo(minGenderProb))
                .and(ProfileSpecification.countryProbabilityGreaterThanOrEqualTo(minCountryProb));

        List<Profile> profiles = profileRepository.findAll(spec, sort);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out);
             CSVWriter csvWriter = (CSVWriter) new CSVWriterBuilder(writer).build()) {

            String[] header = {"id", "name", "gender", "gender_probability", "age", "age_group", "country_id", "country_name", "country_probability", "created_at"};
            csvWriter.writeNext(header);

            for (Profile p : profiles) {
                csvWriter.writeNext(new String[]{
                        p.getId().toString(),
                        p.getName(),
                        p.getGender(),
                        String.valueOf(p.getGenderProbability()),
                        String.valueOf(p.getAge()),
                        p.getAgeGroup(),
                        p.getCountryId(),
                        p.getCountryName(),
                        String.valueOf(p.getCountryProbability()),
                        p.getCreatedAt().toString()
                });
            }
            csvWriter.flush();
            writer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    private String classifyAgeGroup(int age) {
        if (age <= 12) return "child";
        if (age <= 19) return "teenager";
        if (age <= 59) return "adult";
        return "senior";
    }
}
