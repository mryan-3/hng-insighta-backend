package com.hng.stage3.controllers;

import com.hng.stage3.config.GithubConfig;
import com.hng.stage3.dto.ErrorResponse;
import com.hng.stage3.entities.User;
import com.hng.stage3.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GithubConfig githubConfig;

    @GetMapping("/github")
    public RedirectView redirectToGithub(
            @RequestParam(required = false) String state,
            @RequestParam(name = "code_challenge", required = false) String codeChallenge,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            @RequestParam(name = "frontend_url", required = false) String frontendUrl
    ) {
        String effectiveRedirectUri = (redirectUri != null) ? redirectUri : githubConfig.getRedirectUri();
        String url = String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=read:user user:email",
                githubConfig.getClientId(), effectiveRedirectUri
        );
        if (state != null) url += "&state=" + state;
        
        // Pass frontend_url as part of the state or as an additional param if GitHub allows
        // Since GitHub doesn't allow custom params, we should ideally use the 'state' param 
        // to encode this, but for simplicity we'll check if we can pass it to our callback.
        return new RedirectView(url);
    }

    @GetMapping("/github/callback")
    public Object githubCallback(
            @RequestParam String code,
            @RequestParam(name = "code_verifier", required = false) String codeVerifier,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            @RequestParam(name = "frontend_url", required = false) String frontendUrl
    ) {
        try {
            Map<String, String> tokens = authService.loginWithGithub(code, codeVerifier, redirectUri);
            
            // If a frontend_url is provided, redirect back to it with the tokens
            if (frontendUrl != null && !frontendUrl.isEmpty()) {
                String targetUrl = String.format("%s?access_token=%s&refresh_token=%s",
                        frontendUrl, tokens.get("access_token"), tokens.get("refresh_token"));
                return new RedirectView(targetUrl);
            }
            
            // Otherwise return JSON (fallback/direct API use)
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", tokens
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refresh_token");
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ErrorResponse.of("refresh_token is required"));
            }
            Map<String, String> tokens = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", tokens
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refresh_token");
            if (refreshToken != null) {
                authService.logout(refreshToken);
            }
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Logged out successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser() {
        try {
            User user = authService.getCurrentAuthenticatedUser();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", user
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(e.getMessage()));
        }
    }
}
