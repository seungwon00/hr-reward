package com.seungwon.hrreward.store.domain;

import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import com.seungwon.hrreward.user.domain.User;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreMemberTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    private final Store store = Store.create("테스트 매장", null, null, null);
    private final User user = User.create("user@test.com", "hash", "홍길동", "010-0000-0000");

    @Test
    void ownerIsActiveFromTheStart() {
        StoreMember owner = owner();

        assertThat(owner.getRole()).isEqualTo(StoreRole.OWNER);
        assertThat(owner.getStatus()).isEqualTo(StoreMemberStatus.ACTIVE);
        assertThat(owner.getApprovedAt()).isEqualTo(NOW);
    }

    @Test
    void applicantIsPendingEmployee() {
        StoreMember member = StoreMember.apply(store, user);

        assertThat(member.getRole()).isEqualTo(StoreRole.EMPLOYEE);
        assertThat(member.getStatus()).isEqualTo(StoreMemberStatus.PENDING);
        assertThat(member.getApprovedAt()).isNull();
    }

    @Test
    void approveActivatesPendingMember() {
        StoreMember member = member(StoreMemberStatus.PENDING);

        member.approve(NOW);

        assertThat(member.getStatus()).isEqualTo(StoreMemberStatus.ACTIVE);
        assertThat(member.getApprovedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @EnumSource(value = StoreMemberStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void onlyPendingMemberCanBeApprovedOrRejected(StoreMemberStatus status) {
        StoreMember member = member(status);

        assertError(() -> member.approve(NOW), ErrorCode.INVALID_MEMBER_STATUS);
        assertError(member::reject, ErrorCode.INVALID_MEMBER_STATUS);
        assertThat(member.getStatus()).isEqualTo(status);
    }

    @Test
    void rejectChangesPendingMemberToRejected() {
        StoreMember member = member(StoreMemberStatus.PENDING);

        member.reject();

        assertThat(member.getStatus()).isEqualTo(StoreMemberStatus.REJECTED);
        assertThat(member.getApprovedAt()).isNull();
    }

    @Test
    void resignChangesActiveMemberToLeft() {
        StoreMember member = member(StoreMemberStatus.ACTIVE);

        member.resign();

        assertThat(member.getStatus()).isEqualTo(StoreMemberStatus.LEFT);
    }

    @ParameterizedTest
    @EnumSource(value = StoreMemberStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void onlyActiveMemberCanResign(StoreMemberStatus status) {
        StoreMember member = member(status);

        assertError(member::resign, ErrorCode.INVALID_MEMBER_STATUS);
        assertThat(member.getStatus()).isEqualTo(status);
    }

    @Test
    void ownerCannotResign() {
        StoreMember owner = owner();

        assertError(owner::resign, ErrorCode.OWNER_CANNOT_RESIGN);
        assertThat(owner.getStatus()).isEqualTo(StoreMemberStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(value = StoreMemberStatus.class, names = {"REJECTED", "LEFT"})
    void rejectedOrLeftMemberCanReapply(StoreMemberStatus status) {
        StoreMember member = member(status);

        member.reapply();

        assertThat(member.getStatus()).isEqualTo(StoreMemberStatus.PENDING);
        assertThat(member.getApprovedAt()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = StoreMemberStatus.class, names = {"PENDING", "ACTIVE"})
    void pendingOrActiveMemberCannotReapply(StoreMemberStatus status) {
        StoreMember member = member(status);

        assertError(member::reapply, ErrorCode.INVALID_MEMBER_STATUS);
        assertThat(member.getStatus()).isEqualTo(status);
    }

    @Test
    void formerManagerReappliesAsEmployee() {
        StoreMember member = member(StoreMemberStatus.ACTIVE);
        member.changeRole(StoreRole.MANAGER);
        member.resign();

        member.reapply();

        assertThat(member.getRole()).isEqualTo(StoreRole.EMPLOYEE);
    }

    @Test
    void roleChangesBetweenEmployeeAndManager() {
        StoreMember member = member(StoreMemberStatus.ACTIVE);

        member.changeRole(StoreRole.MANAGER);
        assertThat(member.getRole()).isEqualTo(StoreRole.MANAGER);

        member.changeRole(StoreRole.EMPLOYEE);
        assertThat(member.getRole()).isEqualTo(StoreRole.EMPLOYEE);
    }

    @Test
    void memberCannotBePromotedToOwner() {
        StoreMember member = member(StoreMemberStatus.ACTIVE);

        assertError(() -> member.changeRole(StoreRole.OWNER), ErrorCode.OWNER_ROLE_NOT_CHANGEABLE);
        assertThat(member.getRole()).isEqualTo(StoreRole.EMPLOYEE);
    }

    @ParameterizedTest
    @EnumSource(value = StoreRole.class, names = {"MANAGER", "EMPLOYEE"})
    void ownerCannotBeDemoted(StoreRole newRole) {
        StoreMember owner = owner();

        assertError(() -> owner.changeRole(newRole), ErrorCode.OWNER_ROLE_NOT_CHANGEABLE);
        assertThat(owner.getRole()).isEqualTo(StoreRole.OWNER);
    }

    @ParameterizedTest
    @EnumSource(value = StoreMemberStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void onlyActiveMemberRoleCanBeChanged(StoreMemberStatus status) {
        StoreMember member = member(status);

        assertError(() -> member.changeRole(StoreRole.MANAGER), ErrorCode.INVALID_MEMBER_STATUS);
        assertThat(member.getRole()).isEqualTo(StoreRole.EMPLOYEE);
    }

    private StoreMember owner() {
        return StoreMember.createOwner(store, user, NOW);
    }

    private StoreMember member(StoreMemberStatus status) {
        StoreMember member = StoreMember.apply(store, user);
        switch (status) {
            case PENDING -> {
            }
            case ACTIVE -> member.approve(NOW);
            case REJECTED -> member.reject();
            case LEFT -> {
                member.approve(NOW);
                member.resign();
            }
        }
        return member;
    }

    private void assertError(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(expected);
    }
}
