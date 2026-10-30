package com.benitoventas.messaging.consumers;

import com.benitoventas.messaging.events.OrderCreatedEvent;
import com.benitoventas.services.PurchaseService;


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

    @Inject
    public OrderCreatedConsumer(PurchaseService purchaseService){
        this.purchaseService = purchaseService;
    }

    @Incoming("orders-created-in")
    @Blocking
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public void consume(OrderCreatedEvent event) {
        var purchase = purchaseService.process(event);

        LOG.infof("Resultado guardado: orderId=%s, aprobada=%s", purchase.orderId, purchase.approved);
    }
}
