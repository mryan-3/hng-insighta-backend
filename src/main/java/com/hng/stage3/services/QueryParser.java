package com.hng.stage3.services;

import com.hng.stage3.utils.CountryCodeMapper;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryParser {

    @Data
    @Builder
    public static class FilterCriteria {
        private String gender;
        private String ageGroup;
        private String countryId;
        private Integer minAge;
        private Integer maxAge;
        private boolean interpreted;
    }

    public FilterCriteria parse(String q) {
        if (q == null || q.trim().isEmpty()) {
            return FilterCriteria.builder().interpreted(false).build();
        }

        String query = q.toLowerCase().trim();
        FilterCriteria.FilterCriteriaBuilder builder = FilterCriteria.builder().interpreted(true);
        boolean foundAnything = false;

        // 1. Genders
        if (query.contains("male") && !query.contains("female")) {
            builder.gender("male");
            foundAnything = true;
        } else if (query.contains("female")) {
            builder.gender("female");
            foundAnything = true;
        }

        // 2. Age Groups
        if (query.contains("child")) { builder.ageGroup("child"); foundAnything = true; }
        else if (query.contains("teenager")) { builder.ageGroup("teenager"); foundAnything = true; }
        else if (query.contains("adult")) { builder.ageGroup("adult"); foundAnything = true; }
        else if (query.contains("senior")) { builder.ageGroup("senior"); foundAnything = true; }

        // 3. "Young" rule (16-24)
        if (query.contains("young")) {
            builder.minAge(16);
            builder.maxAge(24);
            foundAnything = true;
        }

        // 4. Comparison (above/below X)
        Pattern agePattern = Pattern.compile("(above|below|over|under|older than|younger than)\\s+(\\d+)");
        Matcher matcher = agePattern.matcher(query);
        if (matcher.find()) {
            String operator = matcher.group(1);
            int age = Integer.parseInt(matcher.group(2));
            if (operator.equals("above") || operator.equals("over") || operator.equals("older than")) {
                builder.minAge(age);
            } else {
                builder.maxAge(age);
            }
            foundAnything = true;
        }

        // 5. Countries ("from [country]")
        Pattern countryPattern = Pattern.compile("from\\s+([a-zA-Z\\s]+)");
        matcher = countryPattern.matcher(query);
        if (matcher.find()) {
            String countryName = matcher.group(1).trim();
            String code = CountryCodeMapper.getCode(countryName);
            if (code != null) {
                builder.countryId(code);
                foundAnything = true;
            }
        }

        return builder.interpreted(foundAnything).build();
    }
}
