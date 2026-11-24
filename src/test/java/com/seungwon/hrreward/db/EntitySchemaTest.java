package com.seungwon.hrreward.db;

import com.seungwon.hrreward.TestcontainersConfiguration;
import com.seungwon.hrreward.global.common.BaseTimeEntity;
import com.seungwon.hrreward.store.domain.Store;
import com.seungwon.hrreward.store.domain.StoreInvitation;
import com.seungwon.hrreward.store.domain.StoreMember;
import com.seungwon.hrreward.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// ddl-auto: validate는 컬럼 존재와 타입만 확인하므로,
// 누락된 컬럼과 nullable, 길이, 생성 컬럼 여부까지 information_schema와 비교
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EntitySchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @ValueSource(classes = {User.class, Store.class, StoreMember.class, StoreInvitation.class})
    void entityColumnsMatchSchema(Class<?> entity) {
        assertThat(entityColumns(entity))
                .containsExactlyInAnyOrderElementsOf(tableColumns(entity.getAnnotation(Table.class).name()));
    }

    // length는 문자열 컬럼만 비교하고 나머지는 null
    private record ColumnSpec(String name, boolean nullable, Integer length, boolean generated) {
    }

    private List<ColumnSpec> entityColumns(Class<?> entity) {
        return Stream.of(entity.getDeclaredFields(), BaseTimeEntity.class.getDeclaredFields())
                .flatMap(Stream::of)
                .filter(field -> !field.isSynthetic())
                .map(this::toSpec)
                .toList();
    }

    private ColumnSpec toSpec(Field field) {
        if (field.isAnnotationPresent(Id.class)) {
            return new ColumnSpec("id", false, null, false);
        }
        JoinColumn join = field.getAnnotation(JoinColumn.class);
        if (join != null) {
            return new ColumnSpec(join.name(), join.nullable(), null, !join.insertable());
        }
        Column column = field.getAnnotation(Column.class);
        boolean text = field.getType() == String.class || field.getType().isEnum();
        return new ColumnSpec(column.name(), column.nullable(), text ? column.length() : null, !column.insertable());
    }

    private List<ColumnSpec> tableColumns(String table) {
        return jdbcTemplate.query("""
                SELECT column_name, is_nullable, character_maximum_length, generation_expression
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ?
                """, (rs, rowNum) -> new ColumnSpec(
                rs.getString("column_name"),
                "YES".equals(rs.getString("is_nullable")),
                rs.getObject("character_maximum_length", Integer.class),
                !rs.getString("generation_expression").isEmpty()), table);
    }
}
