package com.benitoventas.messaging.serializers;

import com.benitoventas.messaging.events.OrderProcessedEvent;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class OrderProcessedEventDeserializer extends ObjectMapperDeserializer<OrderProcessedEvent>{

    public OrderProcessedEventDeserializer() {
        super(OrderProcessedEvent.class);
    }

}
