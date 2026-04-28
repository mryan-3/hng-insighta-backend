package com.hng.stage3.services;

import com.hng.stage3.config.GithubConfig;
import com.hng.stage3.entities.User;
import com.hng.stage3.repositories.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final GithubConfig githubConfig;

    public Map<String, String> loginWithGithub(String code, String codeVerifier) {
        // 1. Exchange code for GitHub Access Token
        String githubAccessToken = exchangeCodeForToken(code, codeVerifier);

        // 2. Get User Info from GitHub
        GithubUser githubUser = fetchGithubUserInfo(githubAccessToken);

        // 3. Create or Update local user
        User user = userRepository.findByGithubId(githubUser.getId())
                .map(existingUser -> {
                    existingUser.setLastLoginAt(OffsetDateTime.now());
                    existingUser.setAvatarUrl(githubUser.getAvatarUrl());
                    existingUser.setUsername(githubUser.getLogin());
                    existingUser.setEmail(githubUser.getEmail());
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .githubId(githubUser.getId())
                            .username(githubUser.getLogin())
                            .email(githubUser.getEmail())
                            .avatarUrl(githubUser.getAvatarUrl())
                            .role("analyst") // Default role
                            .isActive(true)
                            .lastLoginAt(OffsetDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });

        // 4. Issue our own JWTs
        org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                "",
                user.getIsActive(),
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
        );

        return Map.of(
                "access_token", jwtService.generateAccessToken(userDetails),
                "refresh_token", jwtService.generateRefreshToken(userDetails)
        );
    }

    private String exchangeCodeForToken(String code, String codeVerifier) {
        String url = "https://github.com/login/oauth/access_token";
        
        Map<String, String> params = Map.of(
                "client_id", githubConfig.getClientId(),
                "client_secret", githubConfig.getClientSecret(),
                "code", code,
                "redirect_uri", githubConfig.getRedirectUri(),
                "code_verifier", codeVerifier != null ? codeVerifier : ""
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String accessToken = (String) response.getBody().get("access_token");
            if (accessToken == null) {
                throw new RuntimeException("GitHub OAuth failed: " + response.getBody().get("error_description"));
            }
            return accessToken;
        }
        throw new RuntimeException("Failed to exchange code for token");
    }

    private GithubUser fetchGithubUserInfo(String accessToken) {
        String url = "https://api.github.com/user";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<GithubUser> response = restTemplate.exchange(url, HttpMethod.GET, entity, GithubUser.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException("Failed to fetch user info from GitHub");
    }

    @Data
    private static class GithubUser {
        private String id;
        private String login;
        private String email;
        private String avatarUrl;

        @com.fasterxml.jackson.annotation.JsonProperty("avatar_url")
        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("id")
        public void setId(Object id) {
            this.id = String.valueOf(id);
        }
    }
}
