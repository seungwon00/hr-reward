package com.seungwon.hrreward.store.repository;

import com.seungwon.hrreward.TestcontainersConfiguration;
import com.seungwon.hrreward.global.config.JpaAuditingConfig;
import com.seungwon.hrreward.store.domain.Store;
import com.seungwon.hrreward.store.domain.StoreInvitation;
import com.seungwon.hrreward.store.domain.StoreRole;
import com.seungwon.hrreward.user.domain.User;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class StoreInvitationRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Autowired
    private StoreInvitationRepository storeInvitationRepository;

    @Autowired
    private TestEntityManager em;

    private User owner;
    private Store store;

    @BeforeEach
    void setUp() {
        owner = em.persist(User.create("owner@test.com", "hash", "홍길동", "010-0000-0000"));
        store = em.persist(Store.create("테스트 매장", null, null, null));
    }

    @Test
    void savedInvitationCanBeFoundByCode() {
        StoreInvitation invitation = storeInvitationRepository.save(invitation("CODE0001", 5));
        invitation.use(NOW);
        invitation.revoke(NOW);
        em.flush();
        em.clear();

        StoreInvitation found = storeInvitationRepository.findByCode("CODE0001").orElseThrow();

        assertThat(found.getId()).isEqualTo(invitation.getId());
        assertThat(found.getStore().getId()).isEqualTo(store.getId());
        assertThat(found.getCreatedBy().getId()).isEqualTo(owner.getId());
        assertThat(found.getCode()).isEqualTo("CODE0001");
        assertThat(found.getRole()).isEqualTo(StoreRole.EMPLOYEE);
        assertThat(found.getMaxUses()).isEqualTo(5);
        assertThat(found.getUsedCount()).isEqualTo(1);
        assertThat(found.getExpiresAt()).isEqualTo(NOW.plusDays(7));
        assertThat(found.getRevokedAt()).isEqualTo(NOW);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void associationsAreLazy() {
        Long id = storeInvitationRepository.save(invitation("CODE0001", null)).getId();
        em.flush();
        em.clear();

        StoreInvitation found = storeInvitationRepository.findById(id).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getStore())).isFalse();
        assertThat(Hibernate.isInitialized(found.getCreatedBy())).isFalse();
    }

    @Test
    void activeInvitationIsFoundByGeneratedColumnUntilRevoked() {
        StoreInvitation invitation = storeInvitationRepository.save(invitation("CODE0001", null));

        assertThat(storeInvitationRepository.findByActiveStoreId(store.getId())).contains(invitation);

        invitation.revoke(NOW);

        assertThat(storeInvitationRepository.findByActiveStoreId(store.getId())).isEmpty();
    }

    @Test
    void newInvitationCanBeIssuedAfterRevokeIsFlushed() {
        StoreInvitation previous = storeInvitationRepository.save(invitation("CODE0001", null));
        previous.revoke(NOW);
        storeInvitationRepository.flush();

        StoreInvitation reissued = storeInvitationRepository.saveAndFlush(invitation("CODE0002", null));

        assertThat(storeInvitationRepository.findByActiveStoreId(store.getId())).contains(reissued);
    }

    // IDENTITY는 INSERT가 즉시 실행되고 revoke의 UPDATE는 flush 때 실행되므로, flush 없이 재발급하면 실패한다
    @Test
    void reissueWithoutFlushingRevokeViolatesUniqueConstraint() {
        StoreInvitation previous = storeInvitationRepository.save(invitation("CODE0001", null));
        previous.revoke(NOW);

        assertThatThrownBy(() -> storeInvitationRepository.save(invitation("CODE0002", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private StoreInvitation invitation(String code, Integer maxUses) {
        return StoreInvitation.issue(store, owner, code, maxUses, NOW.plusDays(7));
    }
}
