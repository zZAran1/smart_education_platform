package com.example.smart_education_platform_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.smart_education_platform_backend.model.dto.ApplyJobDTO;
import com.example.smart_education_platform_backend.model.dto.JobQueryDTO;
import com.example.smart_education_platform_backend.model.entity.Job;
import com.example.smart_education_platform_backend.model.vo.JobCardVO;
import com.example.smart_education_platform_backend.model.vo.JobCategoryVO;
import com.example.smart_education_platform_backend.model.vo.JobDetailVO;
import com.example.smart_education_platform_backend.model.vo.MyInterviewVO;
import com.example.smart_education_platform_backend.model.vo.MyJobApplicationVO;

import java.util.List;

public interface JobService extends IService<Job> {

    /** 职位分类两级字典，缓存 24h */
    List<JobCategoryVO> getCategoryTree();

    /** 职位分页：分类筛选 + 职位/公司关键词搜索，缓存 5min */
    Page<JobCardVO> pageJobs(JobQueryDTO dto);

    /** 职位详情：基础信息（缓存） + 登录增强（收藏/投递状态） */
    JobDetailVO getJobDetail(Long id);

    /** 职位详情基础信息（@Cacheable jobDetail，不含用户相关字段） */
    JobDetailVO getJobBase(Long id);

    /** 感兴趣（收藏切换，幂等），返回收藏后的状态 */
    Boolean toggleCollect(Long jobId);

    /** 申请职位（同一职位仅可投递一次） */
    void apply(Long jobId, ApplyJobDTO dto);

    /** 申请 AI 面试（须已投递该职位），返回面试记录 ID */
    Long applyAiInterview(Long jobId);

    /** 我的投递：当前登录用户的投递记录 + 职位/公司信息，按投递时间倒序 */
    Page<MyJobApplicationVO> pageMyApplications(Integer pageNum, Integer pageSize, Integer status);

    /** 我的数字人面试：当前登录用户的面试记录 + 职位/公司信息，按申请时间倒序 */
    Page<MyInterviewVO> pageMyInterviews(Integer pageNum, Integer pageSize, Integer status);
}
