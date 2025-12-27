package com.ecommerce.auth_service;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationManager customAuthenticationManager;
    private final JwtTokenProvider tokenProvider;
    
    //@CircuitBreaker(name = "authService", fallbackMethod = "authenticateFallback")
    public AuthResponse authenticate(AuthRequest request) {
    	log.info("Authenticating user: {}", request.getUsername());
    	
    	Authentication authentication = null;
    	
    	try {
    	     authentication = customAuthenticationManager.authenticate(
    	        new UsernamePasswordAuthenticationToken(
    	            request.getUsername(), 
    	            request.getPassword()
    	        )
    	    );
    	} catch (BadCredentialsException e) {
    	    // Wrong username/password
    	    log.error("Bad credentials for user: {}", request.getUsername());
    	} catch (DisabledException e) {
    	    // User account disabled
    	    log.error("User account disabled: {}", request.getUsername());
    	} catch (LockedException e) {
    	    // User account locked
    	    log.error("User account locked: {}", request.getUsername());
    	} catch (AuthenticationException e) {
    	    // General authentication failure
             log.error("Authentication failed with FULL stack trace:", e);
            
            // Also log the root cause
            Throwable rootCause = getRootCause(e);
            log.error("Root cause: {}", rootCause.getMessage());
            rootCause.printStackTrace();  // This shows the real error
    	}
    	
    	SecurityContextHolder.getContext().setAuthentication(authentication);
    	
    	String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        
        log.info("User {} authenticated successfully", request.getUsername());
        
    	return new AuthResponse( accessToken,
                refreshToken,
                "Bearer",
                86400L,
                request.getUsername());
    }
    
    public AuthResponse authenticateFallback(AuthRequest request, Throwable t) {
        log.error("Authentication fallback triggered for user: {}", request.getUsername(), t);
        throw new RuntimeException("Authentication service temporarily unavailable");
    }
    
    
    @Transactional
    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        if (user.getRoles().isEmpty()) {
            user.getRoles().add("ROLE_USER");
        }
        
        return userRepository.save(user);
    }
    
    public ValidateTokenResponse validateToken(String token) {
        try {
            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                List<String> roles = tokenProvider.getRolesFromToken(token);
                
                return new ValidateTokenResponse(
                        true,
                        username,
                        roles,
                        "Token is valid"
                );
            }
        } catch (Exception e) {
            log.error("Token validation failed", e);
        }
        
        return new ValidateTokenResponse(
                false,
                null,
                null,
                "Token is invalid"
        );
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                user.getRoles().stream()
                        .map(role -> (org.springframework.security.core.GrantedAuthority) 
                                () -> role)
                        .toList()
        );
        
        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);
        
        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                86400L,
                username
        );
    }
    
    private Throwable getRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
    
    
    @PostConstruct
    public void initDefaultUser() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@ecommerce.com");
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.getRoles().add("ROLE_ADMIN");
            admin.getRoles().add("ROLE_USER");
            
            userRepository.save(admin);
            log.info("Default admin user created");
            
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@ecommerce.com");
            user.setFirstName("Regular");
            user.setLastName("User");
            user.getRoles().add("ROLE_USER");
            
            userRepository.save(user);
            log.info("Default regular user created");
        }
    }
}
