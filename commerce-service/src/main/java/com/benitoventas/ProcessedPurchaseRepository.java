package com.benitoventas;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Filters;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessedPurchaseRepository implements PanacheMongoRepositoryBase<ProcessedPurchase, String> {

    public ProcessedPurchase findByOrderId(ClientSession session, String orderId) {
        return mongoCollection().find(session, Filters.eq("_id", orderId)).first();
    }

    public void save(ClientSession session, ProcessedPurchase purchase) {
        mongoCollection().insertOne(session, purchase);
    }
}
