package com.ecommerce.order_service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.ecommerce.order_service.Order.OrderItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
	
	private final OrderRepository orderRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
	
	@Transactional
	@Qualifier("transactionManager")
	public Order createOrder(OrderRequest orderRequest) {
		
		BigDecimal total = orderRequest.getItems().stream()
				.map( item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		Order order = new Order();
		order.setUserId(orderRequest.getUserId());
		order.setTotalAmount(total);
		order.setShippingAddress(orderRequest.getShippingAddress());
		order.setStatus(OrderEvent.OrderStatus.CREATED);
		
		orderRequest.getItems().forEach( item -> {
			
			Order.OrderItem orderItem = new Order.OrderItem();
			
			orderItem.setQuantity(item.getQuantity());
			orderItem.setProductName(item.getProductName());
			orderItem.setProductId(item.getProductId());
			orderItem.setPrice(item.getPrice());
			
			order.getItems().add(orderItem);
		});
		
		orderRepository.save(order);
		log.info("Order created: {}", order.getId());
		
		try {
		publishOrderEvent(order, OrderEvent.OrderStatus.CREATED);
		}catch(Exception e) {
			log.error("Failed to publish Kafka event for order: {}", order.getId(), e);
		}
		return order;
	}
	
	private void publishOrderEvent(Order order, OrderEvent.OrderStatus status){
		
		OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setStatus(status);
        event.setTotalAmount(order.getTotalAmount());
        event.setShippingAddress(order.getShippingAddress());
        event.setTimestamp(LocalDateTime.now());
        
        List<com.ecommerce.order_service.OrderEvent.OrderItem> orderItems = order.getItems().stream()
        		.map( item ->  
        		new OrderEvent.OrderItem( item.getProductId(), item.getProductName(), item.getQuantity(), item.getPrice())
        		).toList();
        
        event.setItems(orderItems);
        
        // Send to orders topic
        CompletableFuture<SendResult<String, Object>> future1 = kafkaTemplate.send("orders", order.getId(), event);
        
        future1.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order event published to 'orders' topic: {} offset: {}", 
                        order.getId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order event to 'orders' topic: {}", 
                        order.getId(), ex.getMessage());
            }
        });
        
        // Send to user-specific topic
        CompletableFuture<SendResult<String, Object>> future2 = kafkaTemplate.send("user-orders", order.getUserId(), event);
        
        future2.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order event published to 'user-orders' topic: {} offset: {}", 
                        order.getId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order event to 'user-orders' topic: {}", 
                        order.getId(), ex.getMessage());
            }
        });
        
	}
	
	@Transactional
	@Qualifier("transactionManager")
	public Optional<Order> getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }
	
	@Transactional
	@Qualifier("transactionManager")
	public List<Order> gerUserOrders(String userId){
		return orderRepository.findAll().stream().filter( order -> order.getUserId().equals(userId)).toList();
	}
	

	@Transactional
	@Qualifier("transactionManager")
	public void cancelOrder(String orderId){
		
		orderRepository.findById(orderId).ifPresent( order -> {
			order.setStatus(OrderEvent.OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            
            publishOrderEvent(order, OrderEvent.OrderStatus.CANCELLED);
            log.info("Order cancelled: {}", orderId);
		});
	}
	
	 // Listen to inventory events
    @org.springframework.kafka.annotation.KafkaListener(
            topics = "inventory-events",
            groupId = "order-service-group",
    		properties = {
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
                }
    )
    public void handleInventoryEvent( String message) {
    	
    	InventoryEvent event;
		try {
			event = objectMapper.readValue(message, InventoryEvent.class);
			
			log.info("Received inventory event for order: {}", event.getOrderId());
	        
	        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
	            if (event.getUpdateType() == InventoryEvent.InventoryUpdateType.RESERVED) {
	                order.setStatus(OrderEvent.OrderStatus.INVENTORY_RESERVED);
	                order.setUpdatedAt(LocalDateTime.now());
	                orderRepository.save(order);
	                
	                publishOrderEvent(order, OrderEvent.OrderStatus.INVENTORY_RESERVED);
	                log.info("Order {} inventory reserved", order.getId());
	            }
	        });
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        
    }
	
	 @lombok.Data
	    public static class OrderRequest {
	        private String userId;
	        private List<OrderItemRequest> items;
	        private String shippingAddress;
	        
	        @lombok.Data
	        public static class OrderItemRequest {
	            private String productId;
	            private String productName;
	            private Integer quantity;
	            private BigDecimal price;
	        }
	    }
}
