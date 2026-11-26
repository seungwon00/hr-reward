package com.seungwon.hrreward.user.repository;

import com.seungwon.hrreward.TestcontainersConfiguration;
import com.seungwon.hrreward.global.config.JpaAuditingConfig;
import com.seungwon.hrreward.user.domain.User;
import com.seungwon.hrreward.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void savedUserCanBeFoundByEmail() {
        User saved = userRepository.save(User.create("user@test.com", "hash", "홍길동", "010-0000-0000"));
        saved.withdraw();
        em.flush();
        em.clear();

        User found = userRepository.findByEmail("user@test.com").orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getEmail()).isEqualTo("user@test.com");
        assertThat(found.getPassword()).isEqualTo("hash");
        assertThat(found.getName()).isEqualTo("홍길동");
        assertThat(found.getPhone()).isEqualTo("010-0000-0000");
        assertThat(found.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void existsByEmailChecksPresence() {
        userRepository.save(User.create("user@test.com", null, "홍길동", "010-0000-0000"));

        assertThat(userRepository.existsByEmail("user@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@test.com")).isFalse();
    }

    @Test
    void duplicateEmailIsRejected() {
        userRepository.save(User.create("user@test.com", "hash", "홍길동", "010-0000-0000"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                User.create("user@test.com", "hash", "김철수", "010-1111-1111")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
