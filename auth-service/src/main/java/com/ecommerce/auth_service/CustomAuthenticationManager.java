package com.ecommerce.auth_service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationManager implements AuthenticationManager {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		String password = authentication.getCredentials().toString();

		log.info("Attempting authentication for user: {}", username);

		User user = userRepository.findByUsername(username).orElseThrow(() -> {
			log.error("User not found: {}", username);
			return new BadCredentialsException("Invalid username or password");
		});

		if (!passwordEncoder.matches(password, user.getPassword())) {
			log.error("Invalid password for user: {}", username);
			throw new BadCredentialsException("Invalid username or password");
		}

		if (!user.isEnabled()) {
			log.error("User account is disabled: {}", username);
			throw new BadCredentialsException("Account is disabled");
		}

		List<SimpleGrantedAuthority> authorities = user.getRoles().stream().map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		log.info("Authentication successful for user: {}", username);

		return new UsernamePasswordAuthenticationToken(username, password, authorities);
	}

}
