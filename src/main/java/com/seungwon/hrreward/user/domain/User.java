package com.seungwon.hrreward.user.domain;

import com.seungwon.hrreward.global.common.BaseTimeEntity;
import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    // 소셜 로그인 전용 계정은 null
    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    private User(String email, String password, String name, String phone) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.status = UserStatus.ACTIVE;
    }

    public static User create(String email, String encodedPassword, String name, String phone) {
        return new User(email, encodedPassword, name, phone);
    }

    public void withdraw() {
        if (status == UserStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }
        this.status = UserStatus.WITHDRAWN;
    }
}
