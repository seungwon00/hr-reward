package com.seungwon.hrreward.store.repository;

import com.seungwon.hrreward.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
