package com.hng.stage3.dto;

import lombok.Data;
import java.util.List;

public class ExternalApiDtos {

    @Data
    public static class GenderizeResponse {
        private String name;
        private String gender;
        private Double probability;
        private Integer count;
    }

    @Data
    public static class AgifyResponse {
        private String name;
        private Integer age;
        private Integer count;
    }

    @Data
    public static class NationalizeResponse {
        private String name;
        private List<Country> country;
    }

    @Data
    public static class Country {
        @com.fasterxml.jackson.annotation.JsonProperty("country_id")
        private String countryId;
        private Double probability;
    }
}
