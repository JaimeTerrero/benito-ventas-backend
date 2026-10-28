package com.benitoventas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "customers")
public class Customer {
    public ObjectId id;
    public String name;
    public String email;
    public BigDecimal balance;
    public Instant createdAt;
    public List<CustomerDeposit> deposits = new ArrayList<>();
    public String passwordHash;
    public String role;
}
