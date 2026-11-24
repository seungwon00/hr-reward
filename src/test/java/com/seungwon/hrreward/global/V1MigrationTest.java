package com.seungwon.hrreward.global;

import com.seungwon.hrreward.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// V1 스키마의 DB 제약(유니크, 생성 컬럼, CHECK)이 실제 MySQL에서 의도대로 동작하는지 검증
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class V1MigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId;
    private long storeId;

    @BeforeEach
    void setUp() {
        userId = insertUser("owner@test.com", "hash");
        storeId = insertStore("테스트 매장");
    }

    @Test
    void userPasswordCanBeNull() {
        long socialUserId = insertUser("social@test.com", null);

        assertThat(socialUserId).isPositive();
    }

    @Test
    void phoneIsNotUnique() {
        insertUser("a@test.com", "hash");
        insertUser("b@test.com", "hash");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE phone = '010-0000-0000'", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void duplicateStoreMemberIsRejected() {
        insertMember(storeId, userId);

        assertThatThrownBy(() -> insertMember(storeId, userId))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void onlyOneNonRevokedInvitationPerStore() {
        insertInvitation("CODE0001", null, 0);

        assertThatThrownBy(() -> insertInvitation("CODE0002", null, 0))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void newInvitationAllowedAfterRevokingPrevious() {
        insertInvitation("CODE0001", null, 0);
        jdbcTemplate.update("UPDATE store_invitations SET revoked_at = NOW(6) WHERE code = 'CODE0001'");

        insertInvitation("CODE0002", null, 0);

        Integer active = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM store_invitations WHERE store_id = ? AND revoked_at IS NULL",
                Integer.class, storeId);
        assertThat(active).isEqualTo(1);
    }

    @Test
    void maxUsesMustBePositive() {
        assertThatThrownBy(() -> insertInvitation("CODE0001", 0, 0))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void usedCountCannotExceedMaxUses() {
        insertInvitation("CODE0001", 1, 1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE store_invitations SET used_count = used_count + 1 WHERE code = 'CODE0001'"))
                .isInstanceOf(DataAccessException.class);
    }

    private long insertUser(String email, String password) {
        jdbcTemplate.update("""
                INSERT INTO users (email, password, name, phone, status, created_at, updated_at)
                VALUES (?, ?, '홍길동', '010-0000-0000', 'ACTIVE', NOW(6), NOW(6))
                """, email, password);
        return lastInsertId();
    }

    private long insertStore(String name) {
        jdbcTemplate.update("""
                INSERT INTO stores (name, status, created_at, updated_at)
                VALUES (?, 'ACTIVE', NOW(6), NOW(6))
                """, name);
        return lastInsertId();
    }

    private void insertMember(long storeId, long userId) {
        jdbcTemplate.update("""
                INSERT INTO store_members (store_id, user_id, role, status, created_at, updated_at)
                VALUES (?, ?, 'EMPLOYEE', 'PENDING', NOW(6), NOW(6))
                """, storeId, userId);
    }

    private void insertInvitation(String code, Integer maxUses, int usedCount) {
        jdbcTemplate.update("""
                INSERT INTO store_invitations
                    (store_id, code, role, max_uses, used_count, expires_at, created_by, created_at, updated_at)
                VALUES (?, ?, 'EMPLOYEE', ?, ?, NOW(6) + INTERVAL 7 DAY, ?, NOW(6), NOW(6))
                """, storeId, code, maxUses, usedCount, userId);
    }

    private long lastInsertId() {
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
