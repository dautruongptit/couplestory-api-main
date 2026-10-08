package com.couplestory.service;

import com.couplestory.entity.ActivityAction;
import com.couplestory.entity.User;
import com.couplestory.repository.UserActivityRepository;
import com.couplestory.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Not @Transactional on purpose: afterCommit hooks only fire when the test's own transaction really commits. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserActivityServiceTest {

    @Autowired private UserActivityService service;
    @Autowired private UserActivityRepository repository;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private MockMvc mockMvc;

    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void recordsImmediatelyOutsideATransaction() {
        service.record(userA, ActivityAction.LOGIN, "USER", userA, "Đăng nhập bằng mật khẩu");

        assertThat(repository.findAll()).singleElement().satisfies(a -> {
            assertThat(a.getUserId()).isEqualTo(userA);
            assertThat(a.getAction()).isEqualTo("LOGIN");
            assertThat(a.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void insideATransactionTheRowAppearsOnlyAfterCommit() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            service.record(userA, ActivityAction.STORY_CREATED, "STORY", UUID.randomUUID(), "Đã tạo câu chuyện \"A & B\"");
            assertThat(repository.count()).isZero();
        });

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void aRolledBackActionLeavesNoActivity() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            service.record(userA, ActivityAction.STORY_DELETED, "STORY", UUID.randomUUID(), "Đã xóa");
            status.setRollbackOnly();
        });

        assertThat(repository.count()).isZero();
    }

    @Test
    void badInputNeverThrowsAndIsNotStored() {
        assertThatCode(() -> {
            service.record(null, ActivityAction.LOGIN, null, null, "x");
            service.record(userA, null, null, null, "x");
            service.record(userA, ActivityAction.LOGIN, null, null, null);
            service.record(userA, ActivityAction.LOGIN, null, null, "  ");
        }).doesNotThrowAnyException();

        assertThat(repository.count()).isZero();
    }

    @Test
    void aLongSummaryIsTruncatedInsteadOfFailing() {
        service.record(userA, ActivityAction.LOGIN, null, null, "x".repeat(400));

        assertThat(repository.findAll()).singleElement().satisfies(a -> assertThat(a.getSummary()).hasSize(255));
    }

    @Test
    void repeatedUpdatesOfTheSameEntityAreCollapsed() {
        UUID story = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        service.record(userA, ActivityAction.STORY_UPDATED, "STORY", story, "Đã cập nhật");
        service.record(userA, ActivityAction.STORY_UPDATED, "STORY", story, "Đã cập nhật");
        service.record(userA, ActivityAction.STORY_UPDATED, "STORY", other, "Đã cập nhật");
        service.record(userA, ActivityAction.STORY_PUBLISHED, "STORY", story, "Đã xuất bản");
        service.record(userA, ActivityAction.STORY_PUBLISHED, "STORY", story, "Đã xuất bản");

        assertThat(repository.count()).isEqualTo(4);
    }

    @Test
    void theEndpointReturnsOnlyTheCallersRowsNewestFirst() throws Exception {
        service.record(userA, ActivityAction.LOGIN, null, null, "cũ");
        service.record(userA, ActivityAction.ORDER_CREATED, "ORDER", UUID.randomUUID(), "mới");
        service.record(userB, ActivityAction.LOGIN, null, null, "của người khác");

        mockMvc.perform(get("/api/users/activity").with(user(principal(userA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].summary").value("mới"))
                .andExpect(jsonPath("$.items[1].summary").value("cũ"));

        mockMvc.perform(get("/api/users/activity").with(user(principal(userB))))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].summary").value("của người khác"));
    }

    @Test
    void pageSizeIsCapped() throws Exception {
        mockMvc.perform(get("/api/users/activity?size=100000").with(user(principal(userA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(UserActivityService.MAX_PAGE_SIZE));
    }

    @Test
    void anonymousCallersAreRejected() throws Exception {
        mockMvc.perform(get("/api/users/activity")).andExpect(status().isUnauthorized());
    }

    private UserDetailsImpl principal(UUID id) {
        return UserDetailsImpl.build(User.builder().id(id).email(id + "@test.local").passwordHash("x").build());
    }
}
