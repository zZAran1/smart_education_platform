package com.example.smart_education_platform_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smart_education_platform_backend.model.entity.JobCollection;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface JobCollectionMapper extends BaseMapper<JobCollection> {

    /** 物理删除收藏记录（切换收藏时使用，避免逻辑删除残留行与唯一键冲突） */
    @Delete("DELETE FROM job_collection WHERE user_id = #{userId} AND job_id = #{jobId}")
    int deleteByUserAndJob(@Param("userId") Long userId, @Param("jobId") Long jobId);
}
