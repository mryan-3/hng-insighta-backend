package com.hng.stage3.repositories;

import com.hng.stage3.entities.Profile;
import org.springframework.data.jpa.domain.Specification;

public class ProfileSpecification {

    public static Specification<Profile> hasGender(String gender) {
        return (root, query, cb) -> gender == null ? null : cb.equal(cb.lower(root.get("gender")), gender.toLowerCase());
    }

    public static Specification<Profile> hasAgeGroup(String ageGroup) {
        return (root, query, cb) -> ageGroup == null ? null : cb.equal(cb.lower(root.get("ageGroup")), ageGroup.toLowerCase());
    }

    public static Specification<Profile> hasCountryId(String countryId) {
        return (root, query, cb) -> countryId == null ? null : cb.equal(cb.upper(root.get("countryId")), countryId.toUpperCase());
    }

    public static Specification<Profile> ageGreaterThanOrEqualTo(Integer minAge) {
        return (root, query, cb) -> minAge == null ? null : cb.greaterThanOrEqualTo(root.get("age"), minAge);
    }

    public static Specification<Profile> ageLessThanOrEqualTo(Integer maxAge) {
        return (root, query, cb) -> maxAge == null ? null : cb.lessThanOrEqualTo(root.get("age"), maxAge);
    }

    public static Specification<Profile> genderProbabilityGreaterThanOrEqualTo(Double minProb) {
        return (root, query, cb) -> minProb == null ? null : cb.greaterThanOrEqualTo(root.get("genderProbability"), minProb);
    }

    public static Specification<Profile> countryProbabilityGreaterThanOrEqualTo(Double minProb) {
        return (root, query, cb) -> minProb == null ? null : cb.greaterThanOrEqualTo(root.get("countryProbability"), minProb);
    }
}
