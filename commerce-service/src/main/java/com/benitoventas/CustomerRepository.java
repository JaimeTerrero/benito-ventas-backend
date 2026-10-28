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
}
