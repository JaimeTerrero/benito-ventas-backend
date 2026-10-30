package com.benitoventas.entities;

import java.math.BigDecimal;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "products")
public class Product {
    public ObjectId id;
    public String name;
    public BigDecimal price;
    public int stock;
    public boolean active = true;
}
