package com.benitoventas.messaging.consumers;

import com.benitoventas.messaging.events.OrderProcessedEvent;
import com.benitoventas.services.OrderService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.reactive.messaging.annotations.Blocking;

@ApplicationScoped
public class OrderProcessedConsumer {

    private static final Logger LOG = Logger.getLogger(OrderProcessedConsumer.class);
    private final OrderService orderService;

    @Inject
    public OrderProcessedConsumer(OrderService orderService){
        this.orderService = orderService;
    }

    @Incoming("orders-processed-in")
    @Blocking
    public void consume(OrderProcessedEvent event) {
        orderService.applyResult(event);

        LOG.infof("Resultado aplicado o previamente registrado: orderId=%s", event.orderId());
    }
}
