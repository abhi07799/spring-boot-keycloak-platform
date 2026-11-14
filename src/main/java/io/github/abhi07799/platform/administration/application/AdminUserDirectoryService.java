package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminUserDirectoryService {

    private final UserProfileRepository userProfileRepository;

    public AdminUserDirectoryService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public Page<UserProfile> listUsers(int page, int size) {
        return userProfileRepository.findAll(PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
    }
}
