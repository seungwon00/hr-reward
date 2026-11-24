package com.seungwon.hrreward.store.repository;

import com.seungwon.hrreward.store.domain.StoreInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreInvitationRepository extends JpaRepository<StoreInvitation, Long> {

    Optional<StoreInvitation> findByCode(String code);

    // 매장에서 폐기되지 않은 초대(최대 1개)
    Optional<StoreInvitation> findByActiveStoreId(Long storeId);
}
