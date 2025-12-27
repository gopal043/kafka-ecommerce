package com.ecommerce.inventory_service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

private final InventoryService inventoryService;
    
    @GetMapping(value="/{productId}",consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductInventory> getInventory(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }
    
    @PostMapping(value="/{productId}/restock/{quantity}",consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> restock(@PathVariable String productId, 
                                        @PathVariable int quantity) {
        inventoryService.restockProduct(productId, quantity);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Inventory Service is healthy");
    }
}
