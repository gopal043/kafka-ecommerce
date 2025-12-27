package com.ecommerce.inventory_service;

import java.time.LocalDateTime;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    private String productId;
    private String productName;
    private Integer quantity;
    private InventoryUpdateType updateType;
    private String orderId;
    private LocalDateTime timestamp;
    
    public enum InventoryUpdateType {
        RESERVED,
        RELEASED,
        RESTOCKED,
        SOLD
    }
}
