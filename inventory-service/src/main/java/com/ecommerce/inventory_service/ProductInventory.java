package com.ecommerce.inventory_service;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "inventory")
public class ProductInventory {
    @Id
    private String productId;
    private String productName;
    private Integer availableQuantity = 100; // Default stock
    private Integer reservedQuantity = 0;
    private Integer soldQuantity = 0;
    private Double price;
    
    public boolean canReserve(int quantity) {
        return availableQuantity >= quantity;
    }
    
    public void reserve(int quantity) {
        if (canReserve(quantity)) {
            availableQuantity -= quantity;
            reservedQuantity += quantity;
        } else {
            throw new IllegalArgumentException("Insufficient stock");
        }
    }
    
    public void release(int quantity) {
        if (reservedQuantity >= quantity) {
            reservedQuantity -= quantity;
            availableQuantity += quantity;
        }
    }
    
    public void sell(int quantity) {
        if (reservedQuantity >= quantity) {
            reservedQuantity -= quantity;
            soldQuantity += quantity;
        }
    }
    
    public void restock(int quantity) {
        availableQuantity += quantity;
    }
}
