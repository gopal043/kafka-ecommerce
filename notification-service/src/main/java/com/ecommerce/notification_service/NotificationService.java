package com.ecommerce.notification_service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

	
	private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
	
	@KafkaListener(topics = "user-orders", groupId = "notification-service-group")
    public void handleUserOrdersTopic(@Payload OrderEvent message,
                                      ConsumerRecord<String, String> record
                                      //,Acknowledgment acknowledgment
                                      ) {
        try {
            log.info("Received message from 'user-orders': Key={}, Partition={}, Offset={}", 
                    record.key(), record.partition(), record.offset());
            
            // Parse the JSON message
            //OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            
            log.info("Processed user order: User={}, Order={}, Status={}", 
            		message.getUserId(), message.getOrderId(), message.getStatus());
            
            // Process the event
            processUserOrderEvent(message);
            
            // Manually acknowledge the message
            //acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("❌ ERROR processing message from 'user-orders' topic. Message: {}, Error: {}", 
                    message, e.getMessage(), e);
            
            // Log the raw message for debugging
            System.err.println("RAW MESSAGE THAT FAILED:");
            System.err.println(message);
            System.err.println("ERROR: " + e.getMessage());
            
            // Still acknowledge to move past the bad message
            //acknowledgment.acknowledge();
        }
    }
    
    @KafkaListener(topics = "orders", groupId = "notification-service-group")
    public void handleOrdersTopic(@Payload OrderEvent message,
                                  ConsumerRecord<String, String> record
                                  //,Acknowledgment acknowledgment
                                  ) {
        try {
            log.info("Received message from 'orders': Key={}, Partition={}, Offset={}", 
                    record.key(), record.partition(), record.offset());
            
            //OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            
            log.info("Processed order: Order={}, Status={}", 
            		message.getOrderId(), message.getStatus());
            
            processOrderEvent(message);
            
            //acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("❌ ERROR processing message from 'orders' topic. Message: {}, Error: {}", 
                    message, e.getMessage(), e);
            
            // Print the problematic message
            System.err.println("PROBLEMATIC ORDER MESSAGE:");
            System.err.println(message);
            
            //acknowledgment.acknowledge(); // Acknowledge to continue
        }
    }
    
    private void processOrderEvent(OrderEvent event) {
        try {
            switch (event.getStatus()) {
                case CREATED:
                    sendOrderConfirmation(event);
                    break;
                case INVENTORY_RESERVED:
                    log.info("📦 Inventory reserved for order: {}", event.getOrderId());
                    break;
                case CANCELLED:
                    sendCancellationNotification(event);
                    break;
                case SHIPPED:
                    sendShippingNotification(event);
                    break;
                case DELIVERED:
                    sendDeliveryNotification(event);
                    break;
                default:
                    log.info("Order {} status: {}", event.getOrderId(), event.getStatus());
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", event.getOrderId(), e);
        }
    }
    
    private void processUserOrderEvent(OrderEvent event) {
        try {
            log.info("👤 User-specific notification for user: {}, order: {}", 
                    event.getUserId(), event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing user order event: {}", event.getUserId(), e);
        }
    }
    
    private void sendOrderConfirmation(OrderEvent event) {
        try {
            String message = String.format(
                    "📧 ORDER CONFIRMATION\n" +
                    "Order ID: %s\n" +
                    "Total: $%s\n" +
                    "Thank you!",
                    event.getOrderId(),
                    event.getTotalAmount()
            );
            
            log.info("Sending order confirmation: {}", event.getOrderId());
            System.out.println("=".repeat(50));
            System.out.println(message);
            System.out.println("=".repeat(50));
        } catch (Exception e) {
            log.error("Error sending order confirmation: {}", event.getOrderId(), e);
        }
    }
    
    private void sendCancellationNotification(OrderEvent event) {
        String message = String.format("Order %s cancelled", event.getOrderId());
        log.info(message);
        System.out.println("❌ " + message);
    }
    
    private void sendShippingNotification(OrderEvent event) {
        String message = String.format("Order %s shipped", event.getOrderId());
        log.info(message);
        System.out.println("🚚 " + message);
    }
    
    private void sendDeliveryNotification(OrderEvent event) {
        String message = String.format("Order %s delivered", event.getOrderId());
        log.info(message);
        System.out.println("✅ " + message);
    }
}
