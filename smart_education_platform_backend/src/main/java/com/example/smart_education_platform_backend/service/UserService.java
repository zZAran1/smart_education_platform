package com.example.smart_education_platform_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.smart_education_platform_backend.model.dto.LoginDTO;
import com.example.smart_education_platform_backend.model.dto.RegisterDTO;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.LoginVO;

public interface UserService extends IService<Users> {

    /** 用户注册：两次密码一致性校验、用户名唯一校验、BCrypt 加密存储 */
    void register(RegisterDTO dto);

    /** 用户登录：图形验证码校验、连续失败限制、封禁校验，成功签发 JWT */
    LoginVO login(LoginDTO dto);
}
