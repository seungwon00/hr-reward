package com.seungwon.hrreward.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @SpringBootApplication에 두면 @WebMvcTest 같은 슬라이스 테스트에서 JPA 빈이 없어 실패하므로 분리
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
