package com.benitoventas;


import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped 
public class OrderCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(OrderCreatedConsumer.class);
    private final PurchaseService purchaseService;
    private final OrderProcessedPublisher orderProcessedPublisher;

    @Inject
    public OrderCreatedConsumer(PurchaseService purchaseService, OrderProcessedPublisher orderProcessedPublisher){
        this.purchaseService = purchaseService;
        this.orderProcessedPublisher = orderProcessedPublisher;
    }

    @Incoming("orders-created-in")
    @Blocking
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public CompletionStage<Void> consume(OrderCreatedEvent event) {

        var purchase = purchaseService.process(event);

        var result = new OrderProcessedEvent(
                purchase.eventId,
                purchase.orderId,
                purchase.approved,
                purchase.total,
                purchase.rejectionReason,
                purchase.processedAt
        );

        return orderProcessedPublisher.publish(result)
                .thenRun(() -> LOG.infof(
                        "Resultado publicado: orderId=%s, aprobada=%s",
                        purchase.orderId,
                        purchase.approved
                ));
    }
}
