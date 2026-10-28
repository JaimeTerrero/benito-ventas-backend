package com.benitoventas;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped 
public class OrderProcessedPublisher {

    private final Emitter<OrderProcessedEvent> emitter;

    @Inject 
    public OrderProcessedPublisher(@Channel("orders-processed-out") Emitter<OrderProcessedEvent> emitter){
        this.emitter = emitter;
    }

    public CompletionStage<Void> publish(OrderProcessedEvent event){
        return emitter.send(event);
    }
}
