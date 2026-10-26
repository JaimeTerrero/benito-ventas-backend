package com.benitoventas;

import java.time.Instant;
import java.util.List;
import org.bson.types.ObjectId;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "orders")
public class Order {

    public ObjectId id;
    public String customerId;
    public List<OrderItem> items;
    public OrderStatus status;
    public Instant createdAt;
}