package com.ecommerce.inventory_service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

	private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    
    @PostConstruct
    public void initInventory() {
    	
    	if(inventoryRepository.count() == 0) {
    		ProductInventory laptop = new ProductInventory();
            laptop.setProductId("prod001");
            laptop.setProductName("Laptop");
            laptop.setAvailableQuantity(50);
            laptop.setPrice(999.99);
            inventoryRepository.save(laptop);
            
            ProductInventory mouse = new ProductInventory();
            mouse.setProductId("prod002");
            mouse.setProductName("Mouse");
            mouse.setAvailableQuantity(200);
            mouse.setPrice(25.50);
            inventoryRepository.save(mouse);
            
            log.info("Initialized inventory with sample products");
    	}
    }
    
    @KafkaListener(topics = "orders", groupId = "inventory-service-group")
    public void handleOrderEvent(String  message) {
    	
    	try {
    		log.info("📦 Received raw message from 'orders' topic");
            
            
            // Parse JSON manually
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
    	
    	
    	log.info("Processing order event: {} with status: {}", event.getOrderId(), event.getStatus());
    	
    	if(event.getStatus() == OrderEvent.OrderStatus.CREATED) {
    		 try {
    			 
    			 for(OrderEvent.OrderItem item : event.getItems()) {
    				 
    				 Optional<ProductInventory> inventoryOpt  = inventoryRepository.findById(item.getProductId());
    				 
    				 if(inventoryOpt.isPresent()) {
    					 
    					 ProductInventory inventory = inventoryOpt.get();
    					 
    					 if(inventory.canReserve(item.getQuantity())) {
    						 inventory.reserve(item.getQuantity());
                             inventoryRepository.save(inventory);
                             
                             // Send inventory reserved event
                             InventoryEvent inventoryEvent = new InventoryEvent();
                             inventoryEvent.setProductId(item.getProductId());
                             inventoryEvent.setProductName(item.getProductName());
                             inventoryEvent.setQuantity(item.getQuantity());
                             inventoryEvent.setUpdateType(InventoryEvent.InventoryUpdateType.RESERVED);
                             inventoryEvent.setOrderId(event.getOrderId());
                             inventoryEvent.setTimestamp(LocalDateTime.now());
                             
                             kafkaTemplate.send("inventory-events",event.getOrderId(), inventoryEvent);
                             log.info("Inventory reserved for order: {}, product: {}", 
                                     event.getOrderId(), item.getProductId());
                             
                             // Check low stock
                             if (inventory.getAvailableQuantity() < 10) {
                                 sendLowStockAlert(inventory);
                             }
    					 }else {
    						 log.warn("Insufficient stock for order: {}, product: {}", 
                                     event.getOrderId(), item.getProductId());
    					 }
    				 }else {
                         log.error("Product not found: {}", item.getProductId());
                     }
    			 }
    			 
    		 }catch (Exception e) {
                 log.error("Error processing inventory for order: {}", 
                         event.getOrderId(), e);
             }
    	}
    	
    	}catch (Exception e) {
            log.error("❌ Failed to process message. Error: {}", e.getMessage());
            
            // Print the raw message for debugging
            System.err.println("=".repeat(60));
            System.err.println("RAW MESSAGE THAT FAILED:");
            System.err.println(message);
            System.err.println("=".repeat(60));
            // Still acknowledge to move past the bad message
            //acknowledgment.acknowledge();
        }
    	
    }

	private void sendLowStockAlert(ProductInventory inventory) {
		log.warn("Low stock alert: {} (Available: {})", 
                inventory.getProductName(), inventory.getAvailableQuantity());
		
	}
	
	public void restockProduct(String productId, int quantity) {
        inventoryRepository.findById(productId).ifPresentOrElse(
                inventory -> {
                    inventory.restock(quantity);
                    inventoryRepository.save(inventory);
                    log.info("Restocked product: {} with quantity: {}", productId, quantity);
                },
                () -> {
                    throw new RuntimeException("Product not found: " + productId);
                }
        );
    }
    
    public ProductInventory getInventory(String productId) {
        return inventoryRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
    }
}
