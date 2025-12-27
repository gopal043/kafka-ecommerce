package com.ecommerce.order_service;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
