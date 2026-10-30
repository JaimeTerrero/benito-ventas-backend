package com.benitoventas.entities;

import java.math.BigDecimal;
import java.time.Instant;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "processed_purchases")
public class ProcessedPurchase {

    @BsonId
    public String orderId;

    public String eventId;
    public boolean approved;
    public BigDecimal total;
    public String rejectionReason;
    public Instant processedAt;
    public boolean resultEventPublished;
}
