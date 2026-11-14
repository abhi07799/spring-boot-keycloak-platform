package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.administration.infrastructure.persistence.AdminAuditEvent;
import io.github.abhi07799.platform.administration.infrastructure.persistence.AdminAuditEventRepository;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminAuditPersistenceService {

    private final UserProfileRepository userProfileRepository;
    private final AdminAuditEventRepository auditEventRepository;

    public AdminAuditPersistenceService(
            UserProfileRepository userProfileRepository,
            AdminAuditEventRepository auditEventRepository) {
        this.userProfileRepository = userProfileRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public UserProfile persistProvisionedUser(UserProfile profile, String actorSubjectId, String assignedRole) {
        UserProfile saved = userProfileRepository.saveAndFlush(profile);
        auditEventRepository.saveAndFlush(new AdminAuditEvent(
                actorSubjectId, "USER_PROVISIONED", saved.getId(), assignedRole, null, null));
        return saved;
    }

    @Transactional
    public UserProfile persistStatusChange(
            UUID userId, String actorSubjectId, boolean previousEnabled, boolean newEnabled) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(AdminUserNotFoundException::new);
        if (previousEnabled == newEnabled) {
            return profile;
        }
        profile.updateEnabled(newEnabled);
        UserProfile saved = userProfileRepository.saveAndFlush(profile);
        {
            auditEventRepository.saveAndFlush(new AdminAuditEvent(
                    actorSubjectId,
                    newEnabled ? "USER_ENABLED" : "USER_DISABLED",
                    saved.getId(),
                    null,
                    previousEnabled,
                    newEnabled));
        }
        return saved;
    }
}
