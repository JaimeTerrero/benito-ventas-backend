package com.benitoventas.repositories;

import com.benitoventas.entities.Order;
import com.benitoventas.entities.OrderStatus;
import com.benitoventas.messaging.events.OrderProcessedEvent;

import java.util.Date;

import org.bson.types.ObjectId;
import java.time.Instant;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
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

        public Order updateAdminNote(ObjectId orderId, String note, String adminId) {

                return mongoCollection().findOneAndUpdate(
                        Filters.eq("_id", orderId),
                        Updates.combine(
                                Updates.set("adminNote", note),
                                Updates.set("adminNoteUpdatedBy", adminId),
                                Updates.set("adminNoteUpdatedAt", Date.from(Instant.now()))
                        ),new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
                );
        }

        public List<Order> findPendingCreationEvents() {
                return mongoCollection()
                        .find(Filters.and(Filters.ne("creationEventId", null), Filters.ne("creationEventPublished", true)))
                        .sort(Sorts.ascending("createdAt", "_id"))
                        .limit(25)
                        .into(new ArrayList<>());
        }

        public void markCreationEventPublished(ObjectId orderId, String eventId) {

                var result = mongoCollection().updateOne(
                        Filters.and(Filters.eq("_id", orderId), Filters.eq("creationEventId", eventId)),
                        Updates.set("creationEventPublished", true)
                );

                if (result.getMatchedCount() != 1) {
                        throw new IllegalStateException(
                                "No se pudo registrar la publicacion de la orden");
                }
        }
}
