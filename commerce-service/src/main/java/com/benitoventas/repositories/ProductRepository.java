package com.benitoventas.repositories;

import com.benitoventas.entities.Product;

import org.bson.types.ObjectId;
import java.math.BigDecimal;
import org.bson.types.Decimal128;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
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
            throw new IllegalArgumentException("La cantidad a descontar debe ser positiva");
        }

        var result = mongoCollection().updateOne(
            session,
            Filters.and(Filters.eq("_id", productId), Filters.gte("stock", quantity), Filters.ne("active", false)),
            Updates.inc("stock", -quantity)
        );

        return result.getModifiedCount() == 1;
    }

    public Product updateDetails(ObjectId id, String name, BigDecimal price, boolean active) {

        return mongoCollection().findOneAndUpdate(
                Filters.eq("_id", id),
                Updates.combine(
                        Updates.set("name", name),
                        Updates.set("price", new Decimal128(price)),
                        Updates.set("active", active)
                ),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
    }

    public boolean deactivate(ObjectId id) {
        var result = mongoCollection().updateOne(Filters.eq("_id", id),Updates.set("active", false));

        return result.getMatchedCount() == 1;
    }
}
