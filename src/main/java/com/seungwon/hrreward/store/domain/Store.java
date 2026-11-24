package com.seungwon.hrreward.store.domain;

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
@Table(name = "stores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // 하이픈 없는 숫자 10자리
    @Column(name = "business_number", unique = true, length = 10)
    private String businessNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreStatus status;

    private Store(String name, String businessNumber, String address, String phone) {
        this.name = name;
        this.businessNumber = businessNumber;
        this.address = address;
        this.phone = phone;
        this.status = StoreStatus.ACTIVE;
    }

    public static Store create(String name, String businessNumber, String address, String phone) {
        return new Store(name, businessNumber, address, phone);
    }

    public void close() {
        if (status == StoreStatus.CLOSED) {
            throw new BusinessException(ErrorCode.STORE_ALREADY_CLOSED);
        }
        this.status = StoreStatus.CLOSED;
    }
}
