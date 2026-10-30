package com.benitoventas.messaging.serializers;

import com.benitoventas.messaging.events.OrderCreatedEvent;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class OrderCreatedEventDeserializer extends ObjectMapperDeserializer<OrderCreatedEvent>{

    public OrderCreatedEventDeserializer(){
        super(OrderCreatedEvent.class);
    }
}
