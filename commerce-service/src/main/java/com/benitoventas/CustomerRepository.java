package com.benitoventas;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;

import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.ClientSession;

import org.bson.Document;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

@ApplicationScoped 
public class CustomerRepository implements PanacheMongoRepository<Customer> {
    @PostConstruct
    void initializeIndexes(){
        mongoCollection().createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
    }

    public Customer creditDeposit(ObjectId customerId, String reference, BigDecimal amount){
        var deposit = new Document("reference", reference).append("amount", new Decimal128(amount))
                                                    .append("createdAt", Date.from(Instant.now()));

        return mongoCollection().findOneAndUpdate(
            Filters.and(
                Filters.eq("_id", customerId),
                Filters.ne("deposits.reference", reference)
            ),
            Updates.combine(
                Updates.inc("balance", new Decimal128(amount)),
                Updates.push("deposits", deposit)
            ),
            new FindOneAndUpdateOptions()
                        .returnDocument(ReturnDocument.AFTER));
    }

    public Customer findById(ClientSession session, ObjectId customerId){
        return mongoCollection().find(session, Filters.eq("_id", customerId)).first();
    }

    public boolean debitBalance(ClientSession session, ObjectId customerId, BigDecimal amount){
        if(amount == null || amount.signum() <= 0){
            throw new IllegalArgumentException("El monto a descontar debe ser positivo");
        }

        var result = mongoCollection().updateOne(session, Filters.and(Filters.eq("_id", customerId), 
                                            Filters.gte("balance", new Decimal128(amount))
                                        ),
                                        Updates.inc("balance", new Decimal128(amount.negate())));

        return result.getModifiedCount() == 1;
    }

    public Customer findByEmail(String email){
        return find("email", email).firstResult();
    }

    public Customer updateProfile(ObjectId customerId, String name, String email) {

        return mongoCollection().findOneAndUpdate(
                Filters.and(Filters.eq("_id", customerId), Filters.ne("role", "ADMIN")),
                Updates.combine(Updates.set("name", name), Updates.set("email", email)),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
    }
}
