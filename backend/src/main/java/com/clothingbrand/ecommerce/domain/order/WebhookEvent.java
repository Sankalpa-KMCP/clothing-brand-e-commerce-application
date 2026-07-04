package com.clothingbrand.ecommerce.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "type", nullable = false, length = 255)
    private String type;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    @PrePersist
    void prePersist() {
        if (processedAt == null) {
            processedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public WebhookEvent() {}

    public WebhookEvent(String eventId, String type) {
        this.eventId = eventId;
        this.type = type;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
