package com.benitoventas;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class OrderProcessedEventDeserializer extends ObjectMapperDeserializer<OrderProcessedEvent>{

    public OrderProcessedEventDeserializer() {
        super(OrderProcessedEvent.class);
    }

}
