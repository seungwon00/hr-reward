package com.seungwon.hrreward.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    INVALID_TYPE(HttpStatus.BAD_REQUEST, "C002", "입력값의 타입이 올바르지 않습니다."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "C003", "요청 본문을 읽을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C004", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C006", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C007", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 내부 오류가 발생했습니다."),

    // User
    USER_ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "U001", "이미 탈퇴한 회원입니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),

    // Store
    STORE_ALREADY_CLOSED(HttpStatus.CONFLICT, "S001", "이미 폐업 처리된 매장입니다."),

    // Store member
    INVALID_MEMBER_STATUS(HttpStatus.CONFLICT, "M001", "현재 직원 상태에서는 처리할 수 없습니다."),
    OWNER_CANNOT_RESIGN(HttpStatus.CONFLICT, "M002", "매장 소유자는 퇴사 처리할 수 없습니다."),
    OWNER_ROLE_NOT_CHANGEABLE(HttpStatus.CONFLICT, "M003", "소유자 역할로 변경하거나 소유자의 역할을 변경할 수 없습니다."),

    // Store invitation
    INVITATION_REVOKED(HttpStatus.CONFLICT, "I001", "폐기된 초대 코드입니다."),
    INVITATION_EXPIRED(HttpStatus.CONFLICT, "I002", "만료된 초대 코드입니다."),
    INVITATION_EXHAUSTED(HttpStatus.CONFLICT, "I003", "사용 횟수를 모두 소진한 초대 코드입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
