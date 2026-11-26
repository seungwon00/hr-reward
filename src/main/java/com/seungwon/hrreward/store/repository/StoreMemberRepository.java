package com.seungwon.hrreward.store.repository;

import com.seungwon.hrreward.store.domain.StoreMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreMemberRepository extends JpaRepository<StoreMember, Long> {

    Optional<StoreMember> findByStoreIdAndUserId(Long storeId, Long userId);
}
