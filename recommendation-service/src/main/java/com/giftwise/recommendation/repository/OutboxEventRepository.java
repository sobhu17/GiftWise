package com.giftwise.recommendation.repository;

import com.giftwise.recommendation.model.OutboxEvent;
import com.giftwise.recommendation.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find all outbox events in a given status, oldest first — the outbox relay uses this to
     * fetch {@code PENDING} rows in creation order (FIFO), so events are published to Kafka
     * in the same order the underlying recommendations were created.
     *
     * @param status : the status to filter by (typically {@code PENDING})
     * @return matching events ordered by {@code createdAt} ascending, oldest first
     */
    List<OutboxEvent> findByStatusOrderByCreatedAt(OutboxStatus status);
}
