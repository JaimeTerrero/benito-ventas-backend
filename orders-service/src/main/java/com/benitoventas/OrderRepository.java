package com.benitoventas;

import java.util.Date;

import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped 
public class OrderRepository implements PanacheMongoRepository<Order>{

    public boolean applyResult(OrderProcessedEvent event) {

    var status = event.approved() ? OrderStatus.COMPLETADA : OrderStatus.RECHAZADA;

    var result = mongoCollection().updateOne(
            Filters.and(Filters.eq("_id", new ObjectId(event.orderId())), Filters.in(
                            "status",
                            OrderStatus.PENDIENTE.name(),
                            OrderStatus.PROCESANDO.name())
            ),
            Updates.combine(
                    Updates.set("status", status.name()),
                    Updates.set("total", event.total()),
                    Updates.set("rejectionReason", event.rejectionReason()),
                    Updates.set("processedAt", Date.from(event.processedAt())),
                    Updates.set("resultEventId", event.eventId())
            )
    );

    return result.getModifiedCount() == 1;
}
}
