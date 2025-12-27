package com.ecommerce.notification_service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent {

	private String orderId;
    private String userId;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private LocalDateTime timestamp;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
    
    public enum OrderStatus {
        CREATED, 
        PROCESSING, 
        PAYMENT_COMPLETED, 
        PAYMENT_FAILED,
        INVENTORY_RESERVED,
        INVENTORY_FAILED,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }
}
