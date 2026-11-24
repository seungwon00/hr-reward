package com.seungwon.hrreward.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Security 필터 없이 핸들러 동작만 확인하려고 standalone MockMvc 사용
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessException_returnsErrorCodeStatusAndBody() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("C006"))
                .andExpect(jsonPath("$.message").value("매장을 찾을 수 없습니다."));
    }

    @Test
    void invalidRequestBody_returnsFieldErrors() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C003"));
    }

    @Test
    void typeMismatch_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/number/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C002"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    void unexpectedException_returnsInternalError() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("C999"));
    }

    record TestRequest(@NotBlank String name) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다.");
        }

        @PostMapping("/test/valid")
        void valid(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/number/{id}")
        void number(@PathVariable Long id) {
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("boom");
        }
    }
}
