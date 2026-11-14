package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.administration.infrastructure.persistence.AdminAuditEvent;
import io.github.abhi07799.platform.administration.infrastructure.persistence.AdminAuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminAuditQueryService {

    private final AdminAuditEventRepository repository;

    public AdminAuditQueryService(AdminAuditEventRepository repository) {
        this.repository = repository;
    }

    public Page<AdminAuditEvent> listEvents(int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "occurredAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return repository.findAll(PageRequest.of(page, size, sort));
    }
}
