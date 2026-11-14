package io.github.abhi07799.platform.user.application;

import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final String SUBJECT_ID = "keycloak-subject-123";

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void looksUpProfileUsingAuthenticatedKeycloakSubject() {
        UserProfile profile = new UserProfile(
                SUBJECT_ID, "Test Person", "person@example.com", "+919876543210");
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT_ID)).thenReturn(Optional.of(profile));

        assertSame(profile, userProfileService.getByKeycloakSubjectId(SUBJECT_ID));
        verify(userProfileRepository).findByKeycloakSubjectId(SUBJECT_ID);
    }

    @Test
    void throwsWhenSubjectHasNoApplicationProfile() {
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT_ID)).thenReturn(Optional.empty());

        assertThrows(UserProfileNotFoundException.class,
                () -> userProfileService.getByKeycloakSubjectId(SUBJECT_ID));
    }
}
