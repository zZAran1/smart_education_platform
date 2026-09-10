package com.example.smart_education_platform_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.smart_education_platform_backend.model.dto.LoginDTO;
import com.example.smart_education_platform_backend.model.dto.RegisterDTO;
import com.example.smart_education_platform_backend.model.dto.ResetPasswordDTO;
import com.example.smart_education_platform_backend.model.dto.UpdateProfileDTO;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.LoginVO;
import com.example.smart_education_platform_backend.model.vo.ResetCodeVO;
import com.example.smart_education_platform_backend.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<Users> {

    /** 用户注册：两次密码一致性校验、用户名唯一校验、BCrypt 加密存储 */
    void register(RegisterDTO dto);

    /** 用户登录：图形验证码校验、连续失败限制、封禁校验，成功签发 JWT */
    LoginVO login(LoginDTO dto);

    /** 下发重置密码验证码：Redis edu:reset:{target}，5 分钟有效 */
    ResetCodeVO sendResetCode(String target);

    /** 通过邮箱/手机验证码重置密码（验证码一次性） */
    void resetPassword(ResetPasswordDTO dto);

    /** 当前登录用户信息 */
    UserVO getProfile();

    /** 修改昵称/姓名 */
    UserVO updateProfile(UpdateProfileDTO dto);

    /** 上传头像（JPG/PNG，≤2MB），返回 /uploads/ 前缀的访问地址 */
    String updateAvatar(MultipartFile file);

    /** 退出登录：Token 加入黑名单立即失效 */
    void logout(String token);
}
