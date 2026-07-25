package com.giftwise.recommendation.model;

import com.giftwise.recommendation.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends BaseEntity {
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "payload", nullable = false)
    private String payload;
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status =  OutboxStatus.PENDING;
    @Column(name = "error_message")
    private String errorMessage;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
