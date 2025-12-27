package com.ecommerce.auth_service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping(value="/login", consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
            produces = MediaType.APPLICATION_JSON_VALUE)
    //@CircuitBreaker(name = "authService", fallbackMethod = "loginFallback")
    public ResponseEntity<AuthResponse> login( @RequestBody AuthRequest request) {
        log.info("Login request for user: {}", request.getUsername());
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }
    
    public ResponseEntity<AuthResponse> loginFallback(AuthRequest request, Throwable t) {
        log.error("Login fallback triggered", t);
        return ResponseEntity.status(503)
                .body(new AuthResponse(null, null, null, null, "Service unavailable"));
    }
    
    @PostMapping(value="/register",consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> register( @RequestBody User user) {
        User registeredUser = authService.register(user);
        return ResponseEntity.ok(registeredUser);
    }
    
    @PostMapping(value="/validate",consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ValidateTokenResponse> validateToken(
            @RequestBody ValidateTokenRequest request) {
        ValidateTokenResponse response = authService.validateToken(request.getToken());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value="/refresh",consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is healthy");
    }
    
    // DTO for refresh token request
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}