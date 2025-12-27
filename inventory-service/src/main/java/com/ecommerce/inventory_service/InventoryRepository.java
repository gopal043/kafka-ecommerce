package com.ecommerce.inventory_service;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<ProductInventory, String> {

}
