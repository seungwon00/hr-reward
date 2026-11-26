package com.seungwon.hrreward.store.domain;

import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import com.seungwon.hrreward.user.domain.User;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreInvitationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);
    private static final LocalDateTime EXPIRES_AT = NOW.plusDays(7);

    private final Store store = Store.create("테스트 매장", null, null, null);
    private final User owner = User.create("owner@test.com", "hash", "홍길동", "010-0000-0000");

    @Test
    void issuedInvitationGrantsEmployeeRole() {
        StoreInvitation invitation = invitation(null);

        assertThat(invitation.getRole()).isEqualTo(StoreRole.EMPLOYEE);
        assertThat(invitation.getUsedCount()).isZero();
        assertThat(invitation.getRevokedAt()).isNull();
    }

    @Test
    void useIncreasesUsedCountUntilLimit() {
        StoreInvitation invitation = invitation(2);

        invitation.use(NOW);
        invitation.use(NOW);

        assertThat(invitation.getUsedCount()).isEqualTo(2);
        assertError(() -> invitation.use(NOW), ErrorCode.INVITATION_EXHAUSTED);
        assertThat(invitation.getUsedCount()).isEqualTo(2);
    }

    @Test
    void nullMaxUsesMeansUnlimited() {
        StoreInvitation invitation = invitation(null);

        for (int i = 0; i < 100; i++) {
            invitation.use(NOW);
        }

        assertThat(invitation.getUsedCount()).isEqualTo(100);
    }

    @Test
    void usableRightBeforeExpiration() {
        StoreInvitation invitation = invitation(null);

        invitation.use(EXPIRES_AT.minusNanos(1000));

        assertThat(invitation.getUsedCount()).isEqualTo(1);
    }

    @Test
    void notUsableFromExpirationTime() {
        StoreInvitation invitation = invitation(null);

        assertError(() -> invitation.use(EXPIRES_AT), ErrorCode.INVITATION_EXPIRED);
        assertThat(invitation.getUsedCount()).isZero();
    }

    @Test
    void revokedInvitationCannotBeUsed() {
        StoreInvitation invitation = invitation(null);
        invitation.revoke(NOW);

        assertError(() -> invitation.use(NOW), ErrorCode.INVITATION_REVOKED);
    }

    @Test
    void revokeRecordsTime() {
        StoreInvitation invitation = invitation(null);

        invitation.revoke(NOW);

        assertThat(invitation.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    void cannotRevokeTwice() {
        StoreInvitation invitation = invitation(null);
        invitation.revoke(NOW);

        assertError(() -> invitation.revoke(NOW.plusDays(1)), ErrorCode.INVITATION_REVOKED);
        assertThat(invitation.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    void expiredInvitationCanBeRevoked() {
        StoreInvitation invitation = invitation(null);
        LocalDateTime afterExpiration = EXPIRES_AT.plusDays(1);

        invitation.revoke(afterExpiration);

        assertThat(invitation.getRevokedAt()).isEqualTo(afterExpiration);
    }

    private StoreInvitation invitation(Integer maxUses) {
        return StoreInvitation.issue(store, owner, "CODE0001", maxUses, EXPIRES_AT);
    }

    private void assertError(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(expected);
    }
}
