package com.benitoventas;

import org.bson.types.ObjectId;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped 
public class ProductRepository implements PanacheMongoRepository<Product> {

    public Product findById(ClientSession session, ObjectId productId) {

        return mongoCollection().find(session, Filters.eq("_id", productId)).first();
    }

    public boolean decreaseStock(ClientSession session, ObjectId productId, int quantity) {

    if (quantity <= 0) {
        throw new IllegalArgumentException(
                "La cantidad a descontar debe ser positiva");
    }

    var result = mongoCollection().updateOne(
        session,
        Filters.and(Filters.eq("_id", productId), Filters.gte("stock", quantity)),
        Updates.inc("stock", -quantity)
    );

    return result.getModifiedCount() == 1;
}
}
