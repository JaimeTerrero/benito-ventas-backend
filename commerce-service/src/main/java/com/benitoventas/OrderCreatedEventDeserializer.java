package com.benitoventas;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class OrderCreatedEventDeserializer extends ObjectMapperDeserializer<OrderCreatedEvent>{

    public OrderCreatedEventDeserializer(){
        super(OrderCreatedEvent.class);
    }
}
