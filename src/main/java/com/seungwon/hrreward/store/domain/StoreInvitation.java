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
@Table(name = "store_invitations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "code", nullable = false, unique = true, length = 16)
    private String code;

    // 초대 코드로는 항상 EMPLOYEE로만 가입한다
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private StoreRole role;

    // null이면 무제한
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // DB 생성 컬럼. 메모리 값이 DB와 다를 수 있어 getter 없이 조회 조건으로만 사용
    @Getter(AccessLevel.NONE)
    @Column(name = "active_store_id", unique = true, insertable = false, updatable = false)
    private Long activeStoreId;

    private StoreInvitation(Store store, User createdBy, String code, Integer maxUses, LocalDateTime expiresAt) {
        this.store = store;
        this.createdBy = createdBy;
        this.code = code;
        this.role = StoreRole.EMPLOYEE;
        this.maxUses = maxUses;
        this.usedCount = 0;
        this.expiresAt = expiresAt;
    }

    public static StoreInvitation issue(Store store, User createdBy, String code, Integer maxUses,
                                        LocalDateTime expiresAt) {
        return new StoreInvitation(store, createdBy, code, maxUses, expiresAt);
    }

    public void use(LocalDateTime now) {
        if (revokedAt != null) {
            throw new BusinessException(ErrorCode.INVITATION_REVOKED);
        }
        if (!now.isBefore(expiresAt)) {
            throw new BusinessException(ErrorCode.INVITATION_EXPIRED);
        }
        if (maxUses != null && usedCount >= maxUses) {
            throw new BusinessException(ErrorCode.INVITATION_EXHAUSTED);
        }
        this.usedCount++;
    }

    // active_store_id는 만료와 무관하므로 만료된 코드도 폐기해야 새 코드를 발급할 수 있다
    public void revoke(LocalDateTime now) {
        if (revokedAt != null) {
            throw new BusinessException(ErrorCode.INVITATION_REVOKED);
        }
        this.revokedAt = now;
    }
}
