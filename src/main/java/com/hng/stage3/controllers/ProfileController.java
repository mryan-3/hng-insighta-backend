package com.hng.stage3.controllers;

import com.hng.stage3.dto.ErrorResponse;
import com.hng.stage3.dto.PagedResponse;
import com.hng.stage3.entities.Profile;
import com.hng.stage3.services.ProfileService;
import com.hng.stage3.services.QueryParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    private final QueryParser queryParser;

    @GetMapping
    public ResponseEntity<Object> getProfiles(
            @RequestHeader(value = "X-API-Version", required = false) String apiVersion,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age_group,
            @RequestParam(required = false) String country_id,
            @RequestParam(required = false) Integer min_age,
            @RequestParam(required = false) Integer max_age,
            @RequestParam(required = false) Double min_gender_probability,
            @RequestParam(required = false) Double min_country_probability,
            @RequestParam(defaultValue = "created_at") String sort_by,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        if (!"1".equals(apiVersion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("API version header required"));
        }

        try {
            PagedResponse<Profile> response = profileService.getProfiles(
                    gender, age_group, country_id, min_age, max_age,
                    min_gender_probability, min_country_probability,
                    sort_by, order, page, limit, "/api/profiles"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ErrorResponse.of("Invalid query parameters"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchProfiles(
            @RequestHeader(value = "X-API-Version", required = false) String apiVersion,
            @RequestParam String q,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        if (!"1".equals(apiVersion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("API version header required"));
        }

        QueryParser.FilterCriteria criteria = queryParser.parse(q);
        
        if (!criteria.isInterpreted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("Unable to interpret query"));
        }

        PagedResponse<Profile> response = profileService.getProfiles(
                criteria.getGender(), criteria.getAgeGroup(), criteria.getCountryId(),
                criteria.getMinAge(), criteria.getMaxAge(),
                null, null, "created_at", "asc", page, limit, "/api/profiles/search"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> createProfile(
            @RequestHeader(value = "X-API-Version", required = false) String apiVersion,
            @RequestBody Map<String, String> request
    ) {
        if (!"1".equals(apiVersion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("API version header required"));
        }

        String name = request.get("name");
        if (name == null || name.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("Name is required"));
        }

        try {
            Profile profile = profileService.createProfile(name);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "status", "success",
                            "data", profile
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ErrorResponse.of(e.getMessage()));
        }
    }
}
