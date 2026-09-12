package com.example.smart_education_platform_backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 集成测试基类：打通「图形验证码 → Redis 取答案 → 登录拿 Token」的完整链路，
 * 并提供统一的请求与断言工具。
 *
 * <p>依赖真实的 MySQL 与 Redis（与开发环境同库），运行前需注入环境变量：
 * {@code DB_PASSWORD}、{@code JWT_SECRET}。
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    /** 演示数据中的账号（见 edu_platform.sql 第 6 节） */
    protected static final String ADMIN = "admin";
    protected static final String STUDENT = "student01";
    protected static final String STUDENT2 = "student02";
    protected static final String PASSWORD = "admin123456";

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    /** 同一账号在一次测试进程内复用 Token，避免反复走验证码拖慢用例 */
    private static final Map<String, String> TOKEN_CACHE = new ConcurrentHashMap<>();

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected StringRedisTemplate redisTemplate;

    /* ---------------- 登录链路 ---------------- */

    protected String login(String username) throws Exception {
        String cached = TOKEN_CACHE.get(username);
        if (cached != null) {
            return cached;
        }
        String uuid = ok(get("/api/user/captcha")).get("data").get("uuid").asText();
        String code = redisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + uuid);
        assertNotNull(code, "未能从 Redis 读取验证码，请确认 Redis 已启动且与后端使用同一实例");

        JsonNode data = ok(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", username,
                        "password", PASSWORD,
                        "uuid", uuid,
                        "captcha", code))))
                .get("data");
        String token = data.get("token").asText();

        TOKEN_CACHE.put(username, token);
        return token;
    }

    /** 以指定账号身份发起请求 */
    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String username)
            throws Exception {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + login(username));
    }

    protected MockHttpServletRequestBuilder asStudent(MockHttpServletRequestBuilder builder) throws Exception {
        return authed(builder, STUDENT);
    }

    protected MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) throws Exception {
        return authed(builder, ADMIN);
    }

    /* ---------------- 请求与断言工具 ---------------- */

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    /** 执行请求，断言业务码为 200，返回响应 JSON */
    protected JsonNode ok(MockHttpServletRequestBuilder builder) throws Exception {
        JsonNode root = perform(builder);
        assertEquals(200, root.get("code").asInt(), "接口返回业务失败: " + root);
        return root;
    }

    /** 执行请求，断言业务码为指定值，返回响应 JSON */
    protected JsonNode expectCode(MockHttpServletRequestBuilder builder, int expectedCode) throws Exception {
        JsonNode root = perform(builder);
        assertEquals(expectedCode, root.get("code").asInt(),
                "期望业务码 " + expectedCode + "，实际响应: " + root);
        return root;
    }

    /** 仅执行请求，不做业务码断言（用于观察真实返回） */
    protected JsonNode perform(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertNotNull(body, "响应体为空");
        return objectMapper.readTree(body);
    }
}
