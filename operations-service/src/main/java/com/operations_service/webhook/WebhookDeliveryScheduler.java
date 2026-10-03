package com.operations_service.webhook;

import com.common_lib.enums.WebhookEventStatus;
import com.operations_service.entity.WebhookEvent;
import com.operations_service.repository.WebhookEventRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliveryScheduler {

    private final WebhookRetryQueue retryQueue;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDeliverExecutor deliverExecutor;
    private ExecutorService executorService;

    @Value("${app.webhook.delivery.poll-batch-size:100}")
    private int batchSize;

    @PostConstruct
    void init() {
        executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdown();
    }

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(name = "operations-service-webhook-delivery-poll-and-deliver", lockAtMostFor = "10s", lockAtLeastFor = "1s")
    public void pollAndDeliver() {
        Set<UUID> due = retryQueue.pollDue(batchSize);

        if (due.isEmpty()) return;

        for (UUID webhookEventId : due) {
            executorService.submit(() -> {
                deliverExecutor.deliver(webhookEventId);
            });
        }
    }

    @Scheduled(fixedDelay = 10000)
    @SchedulerLock(name = "operations-service-webhook-delivery-reconcile-from-db", lockAtMostFor = "10s", lockAtLeastFor = "1s")
    public void reconcileFromDatabase() {
        LocalDateTime now = LocalDateTime.now();
        List<WebhookEvent> due = webhookEventRepository
                .findByStatusAndNextRetryAtBefore(WebhookEventStatus.PENDING, now);

        for (WebhookEvent event : due) {
            retryQueue.enqueueIfAbsent(event.getId(), event.getNextRetryAt());
        }
    }

}
