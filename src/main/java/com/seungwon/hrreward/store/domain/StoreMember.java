package com.seungwon.hrreward.store.domain;

import com.seungwon.hrreward.global.common.BaseTimeEntity;
import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import com.seungwon.hrreward.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "store_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private StoreRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreMemberStatus status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    private StoreMember(Store store, User user, StoreRole role, StoreMemberStatus status, LocalDateTime approvedAt) {
        this.store = store;
        this.user = user;
        this.role = role;
        this.status = status;
        this.approvedAt = approvedAt;
    }

    public static StoreMember createOwner(Store store, User user, LocalDateTime now) {
        return new StoreMember(store, user, StoreRole.OWNER, StoreMemberStatus.ACTIVE, now);
    }

    // 초대 코드 가입은 항상 EMPLOYEE. MANAGER는 가입 후 changeRole()로만 부여
    public static StoreMember apply(Store store, User user) {
        return new StoreMember(store, user, StoreRole.EMPLOYEE, StoreMemberStatus.PENDING, null);
    }

    public void approve(LocalDateTime now) {
        requireStatus(StoreMemberStatus.PENDING);
        this.status = StoreMemberStatus.ACTIVE;
        this.approvedAt = now;
    }

    public void reject() {
        requireStatus(StoreMemberStatus.PENDING);
        this.status = StoreMemberStatus.REJECTED;
    }

    public void resign() {
        requireStatus(StoreMemberStatus.ACTIVE);
        if (role == StoreRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_RESIGN);
        }
        this.status = StoreMemberStatus.LEFT;
    }

    // UNIQUE(store_id, user_id) 때문에 재가입은 기존 행을 재사용. 이전 역할은 이어받지 않는다
    public void reapply() {
        if (status != StoreMemberStatus.REJECTED && status != StoreMemberStatus.LEFT) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
        }
        this.role = StoreRole.EMPLOYEE;
        this.status = StoreMemberStatus.PENDING;
        this.approvedAt = null;
    }

    public void changeRole(StoreRole newRole) {
        requireStatus(StoreMemberStatus.ACTIVE);
        if (role == StoreRole.OWNER || newRole == StoreRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_ROLE_NOT_CHANGEABLE);
        }
        this.role = newRole;
    }

    private void requireStatus(StoreMemberStatus required) {
        if (status != required) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
        }
    }
}
