package com.seungwon.hrreward.user.service;

import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import com.seungwon.hrreward.user.domain.User;
import com.seungwon.hrreward.user.domain.UserStatus;
import com.seungwon.hrreward.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // 없는 이메일로 로그인해도 BCrypt 비교를 한 번 수행해 응답 시간으로 가입 여부가 드러나지 않게 함
    private final String dummyPasswordHash;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public Long signUp(String email, String rawPassword, String name, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        try {
            User user = userRepository.saveAndFlush(
                    User.create(email, passwordEncoder.encode(rawPassword), name, phone));
            return user.getId();
        } catch (DataIntegrityViolationException e) {
            // existsByEmail 확인 후 동시에 같은 이메일로 가입한 경우 (uk_users_email 위반)
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
    }

    // 이메일 없음, 비밀번호 불일치, 소셜 전용 계정, 탈퇴 회원 모두 같은 에러로 응답한다
    public Long authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElse(null);
        String passwordHash = (user != null && user.getPassword() != null) ? user.getPassword() : dummyPasswordHash;
        boolean matches = passwordEncoder.matches(rawPassword, passwordHash);

        if (user == null || user.getPassword() == null || !matches || user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return user.getId();
    }
}
