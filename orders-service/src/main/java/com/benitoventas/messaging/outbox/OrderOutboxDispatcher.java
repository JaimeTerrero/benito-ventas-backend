package com.benitoventas.messaging.outbox;

import com.benitoventas.entities.Order;
import com.benitoventas.messaging.events.OrderCreatedEvent;
import com.benitoventas.messaging.publishers.OrderEventPublisher;
import com.benitoventas.repositories.OrderRepository;

import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderOutboxDispatcher {

    private static final Logger LOG = Logger.getLogger(OrderOutboxDispatcher.class);
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Inject
    public OrderOutboxDispatcher(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {

        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void dispatch() {
        for (Order order : orderRepository.findPendingCreationEvents()) {
            try {
                var items = order.items.stream()
                        .map(item -> new OrderCreatedEvent.Item(item.productId, item.quantity))
                        .toList();

                var event = new OrderCreatedEvent(
                        order.creationEventId,
                        order.id.toHexString(),
                        order.customerId,
                        items,
                        order.createdAt
                );

                orderEventPublisher.publish(event).toCompletableFuture().get(15, TimeUnit.SECONDS);

                orderRepository.markCreationEventPublished(order.id, order.creationEventId);

                LOG.infof("Evento de orden publicado: orderId=%s", order.id);

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;

            } catch (Exception exception) {
                LOG.warnf(exception, "Publicacion pendiente; se reintentara: orderId=%s", order.id);

                return;
            }
        }
    }
}