package com.ecommerce.inventory_service;

import java.time.Duration;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class InventoryStream {

	@Bean
	public KStream<String, InventoryEvent> streamInventory(StreamsBuilder builder){
		
		JsonSerde<InventoryEvent> inventorySerde = new JsonSerde<>(InventoryEvent.class);
		
		KStream<String,InventoryEvent> stream = builder.stream("inventory-events", Consumed.with(Serdes.String(), inventorySerde));
		
		
		// 1. Simple processing: Count messages per product
		/*
		 * KTable<String, Long> productCount = stream .peek((key, value) -> {
		 * log.info("Stream received - Key: {}, Value: {}", key, value);
		 * System.out.println("📊 STREAM PROCESSING: " + value); }) .groupBy((key,
		 * value) -> { // Extract productId from JSON (simple parsing) if
		 * (value.contains("\"productId\"")) { int start =
		 * value.indexOf("\"productId\":\"") + 13; int end = value.indexOf("\"", start);
		 * return value.substring(start, end); } return "unknown"; })
		 * .count(Materialized.as("product-count-store"));
		 * 
		 * // Log the counts productCount.toStream() .foreach((productId, count) -> {
		 * log.info("📊 Product {} has been updated {} times", productId, count);
		 * System.out.println("📊 STREAM OUTPUT: Product " + productId + " count: " +
		 * count); });
		 */
		
		// Real-time aggregation: total reserved quantity per product
        KTable<String, Long> totalReserved = stream
                .filter((key, event) -> 
                        event.getUpdateType() == InventoryEvent.InventoryUpdateType.RESERVED)
                .groupBy((key, event) -> event.getProductId())
                .count();
        
        totalReserved.toStream().foreach((productId, count) -> 
        log.info("Product {} has {} reserved items", productId, count));
        
        // Windowed aggregation: hourly reserved items
        KTable<Windowed<String>, Long> hourlyReservations = stream
                .filter((key, event) -> 
                        event.getUpdateType() == InventoryEvent.InventoryUpdateType.RESERVED)
                .groupBy((key, event) -> event.getProductId())
                .windowedBy(TimeWindows.of(Duration.ofHours(1)))
                .count();
        
        hourlyReservations.toStream()
                .foreach((windowedKey, count) -> 
                        log.info("Product {} reserved {} times in last hour", 
                                windowedKey.key(), count));
        
        // Send analytics to another topic
        stream.to("inventory-analytics");
		
		return stream;
	}
}
