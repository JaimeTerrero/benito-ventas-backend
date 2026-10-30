package com.benitoventas.messaging.outbox;

import com.benitoventas.entities.ProcessedPurchase;
import com.benitoventas.messaging.events.OrderProcessedEvent;
import com.benitoventas.messaging.publishers.OrderProcessedPublisher;
import com.benitoventas.repositories.ProcessedPurchaseRepository;

import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PurchaseResultOutboxDispatcher {

    private static final Logger LOG = Logger.getLogger(PurchaseResultOutboxDispatcher.class);
    private final ProcessedPurchaseRepository repository;
    private final OrderProcessedPublisher publisher;

    @Inject
    public PurchaseResultOutboxDispatcher(ProcessedPurchaseRepository repository, OrderProcessedPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void dispatch() {
        for (ProcessedPurchase purchase : repository.findPendingResults()) {
            try {
                OrderProcessedEvent event = new OrderProcessedEvent(
                        purchase.eventId,
                        purchase.orderId,
                        purchase.approved,
                        purchase.total,
                        purchase.rejectionReason,
                        purchase.processedAt
                );

                publisher.publish(event).toCompletableFuture().get(15, TimeUnit.SECONDS);

                repository.markResultPublished(purchase.orderId, purchase.eventId);

                LOG.infof("Resultado publicado: orderId=%s", purchase.orderId);

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;

            } catch (Exception exception) {
                LOG.warnf(exception, "Resultado pendiente; se reintentara: orderId=%s", purchase.orderId);

                return;
            }
        }
    }
}