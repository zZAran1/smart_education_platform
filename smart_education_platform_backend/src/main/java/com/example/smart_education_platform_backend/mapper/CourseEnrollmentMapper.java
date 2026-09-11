package com.example.smart_education_platform_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smart_education_platform_backend.model.entity.CourseEnrollment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollment> {

    /**
     * 物理删除报名记录（含逻辑删除残留行）。
     * 报名表存在唯一键 uk_user_course(user_id, course_id)，逻辑删除的行仍会占用唯一键，
     * 因此在重新报名/支付前需先物理清除，避免 DuplicateKeyException。
     */
    @Delete("DELETE FROM course_enrollment WHERE user_id = #{userId} AND course_id = #{courseId}")
    int deleteByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
