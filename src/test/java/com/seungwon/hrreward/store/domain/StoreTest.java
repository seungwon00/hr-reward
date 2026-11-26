package com.seungwon.hrreward.store.domain;

import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreTest {

    private final Store store = Store.create("테스트 매장", null, null, null);

    @Test
    void createdStoreIsActive() {
        assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
    }

    @Test
    void closeChangesStatus() {
        store.close();

        assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
    }

    @Test
    void closedStoreCannotBeClosedAgain() {
        store.close();

        assertThatThrownBy(store::close)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STORE_ALREADY_CLOSED);
    }
}
