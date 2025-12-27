package com.ecommerce.auth_service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {

	@Value("${jwt.secret}")
	private String jwtSecret;
	
	@Value("${jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    
    private SecretKey getSigningKey() {
    	return Keys.hmacShaKeyFor(
    	        jwtSecret.getBytes(StandardCharsets.UTF_8)
    	    );
    }
    
    public String generateToken(Authentication authentication) {
    	
    	String userName = authentication.getName();
    	Date now = new Date();
    	Date exparyDate = new Date(now.getTime() + jwtExpiration);
    	
    	List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
    	
    	return Jwts.builder()
    			.setSubject(userName)
    			.claim("roles",roles)
    			.setExpiration(exparyDate)
    			.setIssuedAt(now)
    			.signWith(getSigningKey(),SignatureAlgorithm.HS256)
    			.compact();
    }
    
    public String generateRefreshToken(Authentication authentication) {
    	
    	String userName = authentication.getName();
    	Date now = new Date();
    	Date exparyDate = new Date(now.getTime() + jwtExpiration);
    	
    	return Jwts.builder()
    			.setSubject(userName)
    			.setIssuedAt(now)
    			.setExpiration(exparyDate)
    			.signWith(getSigningKey(),SignatureAlgorithm.HS256)
    			.compact();
    }
    
    public String getUsernameFromToken(String token) {
    	
    	Claims claims = Jwts.parserBuilder()
    			        .setSigningKey(getSigningKey())
    			        .build()
    			        .parseClaimsJws(token)
    			        .getBody();
    	return claims.getSubject();
    }
    
    @SuppressWarnings("unchecked")
	public List<String> getRolesFromToken(String token){
    	
    	Claims claims = Jwts.parserBuilder()
    			        .setSigningKey(getSigningKey())
    			        .build()
    			        .parseClaimsJws(token)
    			        .getBody();
    	
    	return claims.get("roles",List.class);
    }
    
    public boolean validateToken(String token) {
    	
    	try { Jwts.parserBuilder()
    			        .setSigningKey(getSigningKey())
    			        .build()
    			        .parseClaimsJws(token);
    	   return true;
    	
		    } catch (MalformedJwtException ex) {
		        log.error("Invalid JWT token");
		    } catch (ExpiredJwtException ex) {
		        log.error("Expired JWT token");
		    } catch (UnsupportedJwtException ex) {
		        log.error("Unsupported JWT token");
		    } catch (IllegalArgumentException ex) {
		        log.error("JWT claims string is empty");
		    }
		    return false;
		    }
}
