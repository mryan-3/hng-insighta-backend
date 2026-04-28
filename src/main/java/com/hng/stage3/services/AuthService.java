package com.hng.stage3.services;

import com.hng.stage3.config.GithubConfig;
import com.hng.stage3.config.JwtConfig;
import com.hng.stage3.entities.RefreshToken;
import com.hng.stage3.entities.User;
import com.hng.stage3.repositories.RefreshTokenRepository;
import com.hng.stage3.repositories.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final GithubConfig githubConfig;
    private final JwtConfig jwtConfig;

    @Transactional
    public Map<String, String> loginWithGithub(String code, String codeVerifier) {
        String githubAccessToken = exchangeCodeForToken(code, codeVerifier);
        GithubUser githubUser = fetchGithubUserInfo(githubAccessToken);

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
                            .role("analyst")
                            .isActive(true)
                            .lastLoginAt(OffsetDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });

        return generateTokenPair(user);
    }

    @Transactional
    public Map<String, String> refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        // Rotation: Delete the old token immediately
        refreshTokenRepository.delete(refreshToken);
        
        return generateTokenPair(user);
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    private Map<String, String> generateTokenPair(User user) {
        org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                "",
                user.getIsActive(),
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
        );

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshTokenString = jwtService.generateRefreshToken(userDetails);

        // Store refresh token in DB
        // We only allow one active refresh token per user for simplicity, 
        // but can be adjusted for multi-session.
        refreshTokenRepository.deleteByUser(user);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiryDate(Instant.now().plusMillis(jwtConfig.getRefreshExpiry()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return Map.of(
                "access_token", accessToken,
                "refresh_token", refreshTokenString
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
