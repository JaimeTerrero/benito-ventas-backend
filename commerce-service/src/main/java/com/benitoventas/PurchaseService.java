package com.benitoventas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bson.types.ObjectId;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PurchaseService {
    private final MongoClient mongoClient;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProcessedPurchaseRepository processedPurchaseRepository;

    @Inject
    public PurchaseService(MongoClient mongoClient, CustomerRepository customerRepository, ProductRepository productRepository, ProcessedPurchaseRepository processedPurchaseRepository) {
        this.mongoClient = mongoClient;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.processedPurchaseRepository = processedPurchaseRepository;
    }

    public ProcessedPurchase process(OrderCreatedEvent event) {
        if (event == null || event.orderId() == null || !ObjectId.isValid(event.orderId())) {
            throw new IllegalArgumentException("El evento no contiene un ID de orden valido");}

        var options = TransactionOptions.builder()
                .readConcern(ReadConcern.SNAPSHOT)
                .writeConcern(WriteConcern.MAJORITY)
                .readPreference(ReadPreference.primary())
                .build();

        try (ClientSession session = mongoClient.startSession()) {
            return session.withTransaction(() -> processInTransaction(session, event), options);
        } catch (MongoWriteException exception) {
            if (exception.getError().getCategory()
                    == ErrorCategory.DUPLICATE_KEY) {

                var existing = processedPurchaseRepository.findById(event.orderId());

                if (existing != null) {
                    return existing;
                }
            }

            throw exception;
        }
    }

    private ProcessedPurchase processInTransaction(ClientSession session, OrderCreatedEvent event) {

        var existing = processedPurchaseRepository.findByOrderId(session, event.orderId());

        if (existing != null) {
            return existing;
        }

        PurchasePlan plan;

        try {
            plan = preparePurchase(session, event);
        } catch (PurchaseRejectedException exception) {
            return saveResult(
                    session,
                    event,
                    false,
                    null,
                    exception.getMessage()
            );
        }

        var debited = customerRepository.debitBalance(session, plan.customerId(), plan.total());

        if (!debited) {
            throw new IllegalStateException("No se pudo descontar el saldo validado");
        }

        for (var entry : plan.quantities().entrySet()) {
            var decreased = productRepository.decreaseStock(
                    session,
                    new ObjectId(entry.getKey()),
                    entry.getValue()
            );

            if (!decreased) {
                throw new IllegalStateException("No se pudo descontar el stock validado");
            }
        }

        return saveResult(session, event, true, plan.total(), null);
    }

    private PurchasePlan preparePurchase(ClientSession session, OrderCreatedEvent event) {

        if (event.customerId() == null || !ObjectId.isValid(event.customerId())) {
            throw new PurchaseRejectedException("El ID del cliente no tiene un formato valido");
        }

        var customerId = new ObjectId(event.customerId());

        var customer = customerRepository.findById(session, customerId);

        if (customer == null) {
            throw new PurchaseRejectedException("Cliente no encontrado");
        }

        if (event.items() == null
                || event.items().isEmpty()
                || event.items().size() > 100) {
            throw new PurchaseRejectedException("La orden debe contener entre 1 y 100 lineas");
        }

        Map<String, Integer> quantities = new TreeMap<>();

        for (var item : event.items()) {
            if (item == null || item.productId() == null || !ObjectId.isValid(item.productId()) || item.quantity() <= 0) {
                throw new PurchaseRejectedException("La orden contiene un producto o cantidad invalida");
            }

            var productId = new ObjectId(item.productId()).toHexString();

            try {
                quantities.merge(productId, item.quantity(), (current, additional) -> Math.addExact(current, additional));
            } catch (ArithmeticException exception) {
                throw new PurchaseRejectedException("La cantidad total del producto excede el limite");
            }
        }

        var total = BigDecimal.ZERO;

        for (var entry : quantities.entrySet()) {
            Product product = productRepository.findById(session, new ObjectId(entry.getKey()));

            if (product == null) {
                throw new PurchaseRejectedException("Producto no encontrado: " + entry.getKey());
            }

            if (product.price == null
                    || product.price.signum() <= 0) {
                throw new IllegalStateException("El producto almacenado tiene un precio invalido");
            }

            if (product.stock < entry.getValue()) {
                throw new PurchaseRejectedException("Stock insuficiente para el producto: " + entry.getKey());
            }

            total = total.add(product.price.multiply(BigDecimal.valueOf(entry.getValue())));
        }

        if (customer.balance == null) {
            throw new IllegalStateException("El cliente almacenado no tiene saldo definido");
        }

        if (customer.balance.compareTo(total) < 0) {
            throw new PurchaseRejectedException("Saldo insuficiente");
        }

        return new PurchasePlan(customerId, quantities, total);
    }

    private ProcessedPurchase saveResult(ClientSession session, OrderCreatedEvent event, boolean approved, BigDecimal total, String rejectionReason) {

        ProcessedPurchase result = new ProcessedPurchase();
        result.orderId = event.orderId();
        result.eventId = UUID.randomUUID().toString();
        result.approved = approved;
        result.total = total;
        result.rejectionReason = rejectionReason;
        result.processedAt = Instant.now();

        processedPurchaseRepository.save(session, result);

        return result;
    }

    private record PurchasePlan(ObjectId customerId, Map<String, Integer> quantities, BigDecimal total) 
    {
        
    }
}
