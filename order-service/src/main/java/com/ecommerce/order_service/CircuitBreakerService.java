package com.ecommerce.order_service;

import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CircuitBreakerService {

	@CircuitBreaker(name = "orderService", fallbackMethod = "fallbackMethod")
    public String processOrder(String orderId) {
        log.info("Processing order: {}", orderId);
        
        if (orderId.equals("fail")) {
            throw new RuntimeException("Order processing failed");
        }
        
        return "Order " + orderId + " processed successfully";
    }
    
    public String fallbackMethod(String orderId, Throwable t) {
        log.error("Circuit breaker fallback for order: {}", orderId, t);
        return "Order service temporarily unavailable. Please try again later.";
    }
}
