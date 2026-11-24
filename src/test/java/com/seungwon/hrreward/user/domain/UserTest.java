package com.seungwon.hrreward.user.domain;

import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private final User user = User.create("user@test.com", "hash", "홍길동", "010-0000-0000");

    @Test
    void createdUserIsActive() {
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void withdrawChangesStatus() {
        user.withdraw();

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @Test
    void withdrawnUserCannotWithdrawAgain() {
        user.withdraw();

        assertThatThrownBy(user::withdraw)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_ALREADY_WITHDRAWN);
    }
}
