package com.benitoventas;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

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

    public List<ProcessedPurchase> findPendingResults() {
        return mongoCollection()
                .find(Filters.and(Filters.ne("eventId", null), Filters.ne("resultEventPublished", true)))
                .sort(Sorts.ascending("processedAt", "_id"))
                .limit(25)
                .into(new ArrayList<>());
    }

    public void markResultPublished(String orderId, String eventId) {

        var result = mongoCollection().updateOne(
                Filters.and(
                        Filters.eq("_id", orderId),
                        Filters.eq("eventId", eventId)
                ),
                Updates.set("resultEventPublished", true)
        );

        if (result.getMatchedCount() != 1) {
            throw new IllegalStateException("No se pudo registrar la publicacion del resultado");
        }
    }
}
