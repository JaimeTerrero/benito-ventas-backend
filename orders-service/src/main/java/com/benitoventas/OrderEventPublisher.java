package com.benitoventas;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderEventPublisher {
    private final Emitter<OrderCreatedEvent> emitter;

    @Inject 
    public OrderEventPublisher(@Channel("orders-created-out") Emitter<OrderCreatedEvent> emitter){
        this.emitter = emitter;
    }

    public CompletionStage<Void> publish(OrderCreatedEvent event){
        return emitter.send(event);
    }
}
