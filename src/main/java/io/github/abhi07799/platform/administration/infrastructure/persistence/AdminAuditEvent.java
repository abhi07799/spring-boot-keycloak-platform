package io.github.abhi07799.platform.administration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_events")
public class AdminAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_subject_id", nullable = false, length = 255, updatable = false)
    private String actorSubjectId;

    @Column(name = "action", nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "target_user_id", nullable = false, updatable = false)
    private UUID targetUserId;

    @Column(name = "assigned_role", length = 20, updatable = false)
    private String assignedRole;

    @Column(name = "previous_enabled", updatable = false)
    private Boolean previousEnabled;

    @Column(name = "new_enabled", updatable = false)
    private Boolean newEnabled;

    @Column(name = "previous_role", length = 20, updatable = false)
    private String previousRole;

    @Column(name = "new_role", length = 20, updatable = false)
    private String newRole;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AdminAuditEvent() {
    }

    public AdminAuditEvent(String actorSubjectId, String action, UUID targetUserId,
                           String assignedRole, Boolean previousEnabled, Boolean newEnabled,
                           String previousRole, String newRole) {
        this.actorSubjectId = actorSubjectId;
        this.action = action;
        this.targetUserId = targetUserId;
        this.assignedRole = assignedRole;
        this.previousEnabled = previousEnabled;
        this.newEnabled = newEnabled;
        this.previousRole = previousRole;
        this.newRole = newRole;
    }

    @PrePersist
    void onCreate() {
        occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getActorSubjectId() { return actorSubjectId; }
    public String getAction() { return action; }
    public UUID getTargetUserId() { return targetUserId; }
    public String getAssignedRole() { return assignedRole; }
    public Boolean getPreviousEnabled() { return previousEnabled; }
    public Boolean getNewEnabled() { return newEnabled; }
    public String getPreviousRole() { return previousRole; }
    public String getNewRole() { return newRole; }
    public Instant getOccurredAt() { return occurredAt; }
}
