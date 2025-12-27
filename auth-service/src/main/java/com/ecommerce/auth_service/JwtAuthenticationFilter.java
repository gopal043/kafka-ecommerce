package com.ecommerce.auth_service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	 final JwtTokenProvider jwtTokenProvider;
	 private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
        
		 String requestURI = request.getRequestURI();
		
		 if (isPublicEndpoint(requestURI)) {
	            filterChain.doFilter(request, response);
	            return;
	        }
		 
		String token = getJwtFromRequest(request);
		
		if(token != null && jwtTokenProvider.validateToken(token)) {
			String userName = jwtTokenProvider.getUsernameFromToken(token);
			List<String> roles = jwtTokenProvider.getRolesFromToken(token);
			
			List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
			
			UsernamePasswordAuthenticationToken authenticationToken = 
					new UsernamePasswordAuthenticationToken(userName,null, authorities);
			
			SecurityContextHolder.getContext().setAuthentication(authenticationToken);
			log.debug("Authenticated user: {} with roles: {}", userName, roles);
		}else {
			log.warn("Invalid JWT token provided for endpoint: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid token\"}");
            return;
		}
		
		filterChain.doFilter(request, response);
	}

	private boolean isPublicEndpoint(String uri) {
        return pathMatcher.match("/api/auth/**", uri) ||
               pathMatcher.match("/actuator/**", uri) ||
               pathMatcher.match("/error", uri);
    }
	
	private String getJwtFromRequest(HttpServletRequest request) {
		 
          String bearerToken =  request.getHeader("Authorization");
          if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
              return bearerToken.substring(7);
          }
          return null;
	}
	
	
}
