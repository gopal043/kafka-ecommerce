package com.ecommerce.order_service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

	@Id
    private String id = UUID.randomUUID().toString();
    
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private OrderEvent.OrderStatus status;
    
    private BigDecimal totalAmount;
    private String shippingAddress;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "order")
    private List<OrderItem> items = new ArrayList<>();
	
	    @Entity
	    @Table(name = "order_items")
	    @Data
	    @NoArgsConstructor
	    @AllArgsConstructor
	    public static class OrderItem {
	        @Id
	        @GeneratedValue(strategy = GenerationType.IDENTITY)
	        private Long itemId;
	        
	        private String productId;
	        private String productName;
	        private Integer quantity;
	        private BigDecimal price;
	        @ManyToOne  
	        @JoinColumn(name = "order_id")  
	        private Order order;
	    }
}
