package com.seungwon.hrreward.auth.controller;

import com.jayway.jsonpath.JsonPath;
import com.seungwon.hrreward.TestcontainersConfiguration;
import com.seungwon.hrreward.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

    private static final String SIGN_UP_BODY = """
            {"email":"user@test.com","password":"password123!","name":"홍길동","phone":"01012345678"}
            """;
    private static final String LOGIN_BODY = """
            {"email":"user@test.com","password":"password123!"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        redisTemplate.execute(connection -> {
            connection.serverCommands().flushAll();
            return null;
        }, true);
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void signUpReturnsCreatedUserId() throws Exception {
        postJson("/api/v1/auth/signup", SIGN_UP_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    void signUpWithDuplicateEmailReturnsConflict() throws Exception {
        postJson("/api/v1/auth/signup", SIGN_UP_BODY);

        postJson("/api/v1/auth/signup", SIGN_UP_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("U002"));
    }

    @Test
    void signUpWithInvalidInputReturnsFieldErrors() throws Exception {
        postJson("/api/v1/auth/signup", """
                {"email":"not-email","password":"short","name":"","phone":"010-1234-5678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    void loginReturnsTokens() throws Exception {
        postJson("/api/v1/auth/signup", SIGN_UP_BODY);

        postJson("/api/v1/auth/login", LOGIN_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessTokenExpiresIn").value(1800));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        postJson("/api/v1/auth/signup", SIGN_UP_BODY);

        postJson("/api/v1/auth/login", """
                {"email":"user@test.com","password":"wrong-password"}
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void publicEndpointIgnoresInvalidAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGN_UP_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorizedJson() throws Exception {
        postJson("/api/v1/auth/logout", "{\"refreshToken\":\"any\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
    }

    @Test
    void protectedEndpointWithInvalidTokenReturnsInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"any\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    void fullFlow_login_reissue_logout() throws Exception {
        postJson("/api/v1/auth/signup", SIGN_UP_BODY);
        String login = postJson("/api/v1/auth/login", LOGIN_BODY).andReturn().getResponse().getContentAsString();
        String firstRefresh = JsonPath.read(login, "$.refreshToken");

        // 재발급: 새 토큰 쌍, 이전 refresh token은 재사용 불가
        String reissued = postJson("/api/v1/auth/reissue", refreshBody(firstRefresh))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(reissued, "$.accessToken");
        String secondRefresh = JsonPath.read(reissued, "$.refreshToken");

        postJson("/api/v1/auth/reissue", refreshBody(firstRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A002"));

        // 로그아웃 후 해당 refresh token으로 재발급 불가
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(secondRefresh)))
                .andExpect(status().isNoContent());

        postJson("/api/v1/auth/reissue", refreshBody(secondRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String refreshBody(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }
}
