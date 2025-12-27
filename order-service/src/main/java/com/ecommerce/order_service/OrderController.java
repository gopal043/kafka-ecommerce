package com.ecommerce.order_service;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@AllArgsConstructor
public class OrderController {

	private final OrderService orderService;

	
	  @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
            produces = MediaType.APPLICATION_JSON_VALUE) 
	  public ResponseEntity<Order> createOrder(@RequestBody
	  OrderService.OrderRequest request) { Order order =
	  orderService.createOrder(request); return ResponseEntity.ok(order); }
	  
	  @GetMapping(value="/{orderId}",consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
	            produces = MediaType.APPLICATION_JSON_VALUE) 
	  public ResponseEntity<Order> getOrder(@PathVariable
	  String orderId) { return orderService.getOrder(orderId)
	  .map(ResponseEntity::ok) .orElse(ResponseEntity.notFound().build()); }
	  
	  @GetMapping(value="/user/{userId}",consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
	            produces = MediaType.APPLICATION_JSON_VALUE) 
	  public ResponseEntity<List<Order>>
	  getUserOrders(@PathVariable String userId) { return
	  ResponseEntity.ok(orderService.gerUserOrders(userId)); }
	  
	  @PostMapping(value="/{orderId}/cancel",consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_PLAIN_VALUE}, 
	            produces = MediaType.APPLICATION_JSON_VALUE) 
	  public ResponseEntity<Void>
	  cancelOrder(@PathVariable String orderId) {
	  orderService.cancelOrder(orderId); return ResponseEntity.ok().build(); }
	  
	  @GetMapping("/health") 
	  public ResponseEntity<String> health() { return
	  ResponseEntity.ok("Order Service is healthy"); }

}
