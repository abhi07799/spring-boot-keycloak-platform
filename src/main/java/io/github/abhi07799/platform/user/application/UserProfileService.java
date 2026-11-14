package io.github.abhi07799.platform.user.application;

import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile getByKeycloakSubjectId(String subjectId) {
        return userProfileRepository.findByKeycloakSubjectId(subjectId)
                .orElseThrow(UserProfileNotFoundException::new);
    }
}
