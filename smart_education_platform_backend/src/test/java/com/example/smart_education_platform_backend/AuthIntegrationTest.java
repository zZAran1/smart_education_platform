package com.example.smart_education_platform_backend;

import com.example.smart_education_platform_backend.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 鉴权链路集成测试：验证「所有业务接口必须携带有效 Token，并按角色放行」。
 */
@DisplayName("集成测试 · 鉴权与角色")
class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("白名单接口匿名可访问：获取图形验证码")
    void captchaIsPublic() throws Exception {
        ok(get("/api/user/captcha"));
    }

    @Test
    @DisplayName("无 Token 访问课程列表被拒（1002）")
    void coursePageRequiresToken() throws Exception {
        expectCode(get("/api/course/page?type=0&page_num=1&page_size=5"), 1002);
    }

    @Test
    @DisplayName("无 Token 访问职位列表被拒（1002）")
    void jobPageRequiresToken() throws Exception {
        expectCode(get("/api/job/page?page_num=1&page_size=5"), 1002);
    }

    @Test
    @DisplayName("伪造 Token 被拒（1002）")
    void forgedTokenRejected() throws Exception {
        expectCode(get("/api/course/page?type=0")
                .header("Authorization", "Bearer forged.token.value"), 1002);
    }

    @Test
    @DisplayName("学员 Token 正常访问课程与职位列表")
    void studentCanBrowse() throws Exception {
        ok(asStudent(get("/api/course/page?type=0&page_num=1&page_size=5")));
        ok(asStudent(get("/api/job/page?page_num=1&page_size=5")));
    }

    @Test
    @DisplayName("学员 Token 访问后台接口被拒（1013 越权）")
    void studentCannotAccessAdmin() throws Exception {
        expectCode(asStudent(get("/api/admin/users?page_num=1&page_size=5")), 1013);
    }

    @Test
    @DisplayName("管理员 Token 可访问后台接口")
    void adminCanAccessAdmin() throws Exception {
        ok(asAdmin(get("/api/admin/users?page_num=1&page_size=5")));
    }
}
