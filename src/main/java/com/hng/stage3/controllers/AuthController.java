package com.hng.stage3.controllers;

import com.hng.stage3.config.GithubConfig;
import com.hng.stage3.dto.ErrorResponse;
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
            @RequestParam(name = "code_challenge", required = false) String codeChallenge
    ) {
        String url = String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=user:email",
                githubConfig.getClientId(), githubConfig.getRedirectUri()
        );
        if (state != null) url += "&state=" + state;
        return new RedirectView(url);
    }

    @GetMapping("/github/callback")
    public ResponseEntity<Object> githubCallback(
            @RequestParam String code,
            @RequestParam(name = "code_verifier", required = false) String codeVerifier
    ) {
        try {
            Map<String, String> tokens = authService.loginWithGithub(code, codeVerifier);
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
        return ResponseEntity.ok(Map.of("status", "success", "message", "Refresh logic pending"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout() {
        return ResponseEntity.ok(Map.of("status", "success", "message", "Logged out"));
    }
}
