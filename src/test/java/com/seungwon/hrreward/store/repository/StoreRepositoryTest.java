package com.seungwon.hrreward.store.repository;

import com.seungwon.hrreward.TestcontainersConfiguration;
import com.seungwon.hrreward.global.config.JpaAuditingConfig;
import com.seungwon.hrreward.store.domain.Store;
import com.seungwon.hrreward.store.domain.StoreStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class StoreRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void savedStoreCanBeFound() {
        Store saved = storeRepository.save(Store.create("테스트 매장", "1234567890", "서울시", "02-000-0000"));
        saved.close();
        em.flush();
        em.clear();

        Store found = storeRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("테스트 매장");
        assertThat(found.getBusinessNumber()).isEqualTo("1234567890");
        assertThat(found.getAddress()).isEqualTo("서울시");
        assertThat(found.getPhone()).isEqualTo("02-000-0000");
        assertThat(found.getStatus()).isEqualTo(StoreStatus.CLOSED);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }
}
