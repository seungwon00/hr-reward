package com.seungwon.hrreward.store.repository;

import com.seungwon.hrreward.TestcontainersConfiguration;
import com.seungwon.hrreward.global.config.JpaAuditingConfig;
import com.seungwon.hrreward.store.domain.Store;
import com.seungwon.hrreward.store.domain.StoreMember;
import com.seungwon.hrreward.store.domain.StoreMemberStatus;
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
class StoreMemberRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Autowired
    private StoreMemberRepository storeMemberRepository;

    @Autowired
    private TestEntityManager em;

    private User user;
    private Store store;

    @BeforeEach
    void setUp() {
        user = em.persist(User.create("user@test.com", "hash", "홍길동", "010-0000-0000"));
        store = em.persist(Store.create("테스트 매장", null, null, null));
    }

    @Test
    void savedMemberCanBeFoundByStoreAndUser() {
        StoreMember member = storeMemberRepository.save(StoreMember.apply(store, user));
        member.approve(NOW);
        member.changeRole(StoreRole.MANAGER);
        em.flush();
        em.clear();

        StoreMember found = storeMemberRepository.findByStoreIdAndUserId(store.getId(), user.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(member.getId());
        assertThat(found.getStore().getId()).isEqualTo(store.getId());
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getRole()).isEqualTo(StoreRole.MANAGER);
        assertThat(found.getStatus()).isEqualTo(StoreMemberStatus.ACTIVE);
        assertThat(found.getApprovedAt()).isEqualTo(NOW);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void memberOfAnotherStoreIsNotFound() {
        Store other = em.persist(Store.create("다른 매장", null, null, null));
        storeMemberRepository.save(StoreMember.apply(store, user));

        assertThat(storeMemberRepository.findByStoreIdAndUserId(other.getId(), user.getId())).isEmpty();
    }

    @Test
    void associationsAreLazy() {
        Long id = storeMemberRepository.save(StoreMember.apply(store, user)).getId();
        em.flush();
        em.clear();

        StoreMember found = storeMemberRepository.findById(id).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getStore())).isFalse();
        assertThat(Hibernate.isInitialized(found.getUser())).isFalse();
    }

    @Test
    void sameUserCannotHaveTwoRowsInOneStore() {
        storeMemberRepository.save(StoreMember.apply(store, user));

        assertThatThrownBy(() -> storeMemberRepository.saveAndFlush(StoreMember.apply(store, user)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
