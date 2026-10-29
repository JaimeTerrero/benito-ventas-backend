package com.benitoventas;

import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
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

    public OrderResponse create(CreateOrderRequest request, String customerId){
        Order order = new Order();
        order.customerId = customerId;
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

        order.creationEventId = UUID.randomUUID().toString();
        order.creationEventPublished = false;

        orderRepository.persist(order);

        return toResponse(order);
    }

    public OrderResponse findById(String id){
        if (!ObjectId.isValid(id)){
            throw new BadRequestException("El ID de la orden no tiene formato válido");
        }

        var order = orderRepository.findById(new ObjectId(id));

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

    public List<AdminOrderResponse> listForAdmin(String customerId, String status, int page, int size) {

            if (page < 0 || size < 1 || size > 100) {
                throw new BadRequestException("La pagina debe ser >= 0 y el tamaño entre 1 y 100");
            }

            List<Bson> filters = new ArrayList<>();

            if (customerId != null && !customerId.isBlank()) {
                if (!ObjectId.isValid(customerId)) {
                    throw new BadRequestException("El ID del cliente no tiene un formato valido");
                }

                filters.add(Filters.eq("customerId", new ObjectId(customerId).toHexString()));
            }

            if (status != null && !status.isBlank()) {
                OrderStatus orderStatus;

                try {
                    orderStatus = OrderStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    throw new BadRequestException("El estado de la orden no es valido");
                }

                filters.add(Filters.eq("status", orderStatus.name()));
            }

            Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);

            return orderRepository
                    .find(filter, new Document("createdAt", -1).append("_id", -1))
                    .page(page, size)
                    .list()
                    .stream()
                    .map(this::toAdminResponse)
                    .toList();
        }

        public AdminOrderResponse updateAdminNote(String id, UpdateOrderNoteRequest request, String adminId) {

        if (!ObjectId.isValid(id)) {
            throw new BadRequestException("El ID de la orden no tiene un formato valido");
        }

        var order = orderRepository.updateAdminNote(new ObjectId(id), request.note().strip(), adminId);

        if (order == null) {
            throw new NotFoundException("Orden no encontrada");
        }

        return toAdminResponse(order);
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

    private AdminOrderResponse toAdminResponse(Order order) {
        return new AdminOrderResponse(
                toResponse(order),
                order.adminNote,
                order.adminNoteUpdatedBy,
                order.adminNoteUpdatedAt
        );
    }
}
