package com.hng.stage3.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    private String gender;

    @JsonProperty("gender_probability")
    @Column(name = "gender_probability")
    private Double genderProbability;

    private Integer age;

    @JsonProperty("age_group")
    @Column(name = "age_group")
    private String ageGroup;

    @JsonProperty("country_id")
    @Column(name = "country_id", length = 2)
    private String countryId;

    @JsonProperty("country_name")
    @Column(name = "country_name")
    private String countryName;

    @JsonProperty("country_probability")
    @Column(name = "country_probability")
    private Double countryProbability;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
