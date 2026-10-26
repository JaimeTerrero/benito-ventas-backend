package com.benitoventas;


import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped 
public class OrderCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(OrderCreatedConsumer.class);

    @Incoming("orders-created-in")
    public void consume(OrderCreatedEvent event){
        LOG.infof("Orden recibida: orderId=%s, eventId=%s, lineas=%d", event.orderId(), event.eventId(), event.items().size());
    }
}
