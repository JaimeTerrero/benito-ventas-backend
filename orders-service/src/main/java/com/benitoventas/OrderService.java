package com.benitoventas;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.bson.types.ObjectId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped 
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Inject 
    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher){
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    public CompletionStage<OrderResponse> create(CreateOrderRequest request){
        Order order = new Order();
        order.customerId = request.customerId();
        order.status = OrderStatus.PENDIENTE;
        order.createdAt = Instant.now();

        order.items = request.items().stream()
                    .map(itemRequest -> {
                        if (!ObjectId.isValid(itemRequest.productId())){
                            throw new BadRequestException("El ID del producto no tiene formato válido");
                        }

                        OrderItem item = new OrderItem();
                        item.productId = itemRequest.productId();
                        item.quantity = itemRequest.quantity();
                        return item;
                    }).toList();

        orderRepository.persist(order);

        var items = order.items.stream().map(item -> new OrderCreatedEvent.Item(item.productId, item.quantity)).toList();

        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID().toString(), order.id.toHexString(), order.customerId, items, order.createdAt);

        OrderResponse response = toResponse(order);

        return orderEventPublisher.publish(event).thenApply(ignored -> response);
    }

    public OrderResponse findById(String id){
        if (!ObjectId.isValid(id)){
            throw new BadRequestException("El ID de la orden no tiene formato válido");
        }

        Order order = orderRepository.findById(new ObjectId(id));

        if (order == null){
            throw new NotFoundException("Orden no encontrada");
        }

        return toResponse(order);
    }

    public void applyResult(OrderProcessedEvent event) {
        if (event == null
                || event.orderId() == null
                || !ObjectId.isValid(event.orderId())
                || event.eventId() == null
                || event.eventId().isBlank()
                || event.processedAt() == null) {
            throw new IllegalArgumentException("El evento de resultado no tiene los datos requeridos");
        }

        if (orderRepository.applyResult(event)) {
            return;
        }

        var order = orderRepository.findById(new ObjectId(event.orderId()));

        if (order == null) {
            throw new IllegalStateException("Se recibio un resultado para una orden inexistente");
        }

        if (event.eventId().equals(order.resultEventId)) {
            return;
        }

        throw new IllegalStateException("La orden ya tiene otro resultado final");
    }

    private OrderResponse toResponse(Order order){
        var items = order.items.stream()
                .map(item -> new OrderResponse.Item(
                    item.productId,
                    item.quantity))
                .toList();

        return new OrderResponse(
            order.id.toHexString(),
            order.customerId,
            items,
            order.status,
            order.createdAt,
            order.total,
            order.rejectionReason,
            order.processedAt
        );
    }
}
