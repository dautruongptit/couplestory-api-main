package com.couplestory.service;

import com.couplestory.entity.Website;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.WebsiteRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for the shared ownership check every website-scoped
 * service/controller (Website, Photo, Event, Message, Moment, Collaborator) relies on.
 * This is the single point of failure for the whole IDOR fix, so it gets direct
 * unit coverage independent of any web/security context.
 */
class WebsiteAccessServiceTest {

    private final WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
    private final WebsiteAccessService service = new WebsiteAccessService(websiteRepository);

    @Test
    void throwsNotFoundWhenWebsiteDoesNotExist() {
        when(websiteRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.requireOwnedWebsite("missing", "user-1"));
    }

    @Test
    void throwsForbiddenWhenCallerIsNotOwner() {
        Website website = Website.builder().id("w1").userId("owner-1").build();
        when(websiteRepository.findById("w1")).thenReturn(Optional.of(website));

        assertThrows(ForbiddenOperationException.class,
                () -> service.requireOwnedWebsite("w1", "intruder"));
    }

    @Test
    void returnsWebsiteWhenCallerIsOwner() {
        Website website = Website.builder().id("w1").userId("owner-1").build();
        when(websiteRepository.findById("w1")).thenReturn(Optional.of(website));

        Website result = service.requireOwnedWebsite("w1", "owner-1");

        assertSame(website, result);
    }
}
