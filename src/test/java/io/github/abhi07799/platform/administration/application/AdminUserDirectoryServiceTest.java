package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserDirectoryServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private AdminUserDirectoryService directoryService;

    @Test
    void returnsRequestedPageSortedByNewestProfileFirst() {
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        UserProfile profile = new UserProfile(
                "subject-1", "Test Person", "person@example.com", "+919876543210");
        when(userProfileRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(profile), pageable, 11));

        Page<UserProfile> result = directoryService.listUsers(1, 10);

        assertEquals(1, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(11, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals("Test Person", result.getContent().getFirst().getFullName());
        verify(userProfileRepository).findAll(pageable);
    }
}
