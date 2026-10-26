package com.benitoventas;

import java.time.Instant;

import org.bson.types.ObjectId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped 
public class OrderService {

    private final OrderRepository orderRepository;

    @Inject 
    public OrderService(OrderRepository orderRepository){
        this.orderRepository = orderRepository;
    }

    public OrderResponse create(CreateOrderRequest request){
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

        return toResponse(order);
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
            order.createdAt
        );
    }
}
