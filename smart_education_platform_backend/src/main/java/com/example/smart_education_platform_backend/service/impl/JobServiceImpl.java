package com.example.smart_education_platform_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.smart_education_platform_backend.converter.Converter;
import com.example.smart_education_platform_backend.exception.JobException;
import com.example.smart_education_platform_backend.mapper.AiInterviewMapper;
import com.example.smart_education_platform_backend.mapper.CompanyMapper;
import com.example.smart_education_platform_backend.mapper.JobApplicationMapper;
import com.example.smart_education_platform_backend.mapper.JobCategoryMapper;
import com.example.smart_education_platform_backend.mapper.JobCollectionMapper;
import com.example.smart_education_platform_backend.mapper.JobMapper;
import com.example.smart_education_platform_backend.model.dto.ApplyJobDTO;
import com.example.smart_education_platform_backend.model.dto.JobQueryDTO;
import com.example.smart_education_platform_backend.model.entity.AiInterview;
import com.example.smart_education_platform_backend.model.entity.Company;
import com.example.smart_education_platform_backend.model.entity.Job;
import com.example.smart_education_platform_backend.model.entity.JobApplication;
import com.example.smart_education_platform_backend.model.entity.JobCategory;
import com.example.smart_education_platform_backend.model.entity.JobCollection;
import com.example.smart_education_platform_backend.model.vo.JobCardVO;
import com.example.smart_education_platform_backend.model.vo.JobCategoryVO;
import com.example.smart_education_platform_backend.model.vo.JobDetailVO;
import com.example.smart_education_platform_backend.service.JobService;
import com.example.smart_education_platform_backend.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    private static final int STATUS_ON_SHELF = 1;
    private static final String APPLY_LOCK_KEY_PREFIX = "edu:apply:";
    /** 防重锁只用于拦截短时间内的重复提交，DB 唯一键才是最终防线；TTL 过长会导致异常时用户被误锁 */
    private static final Duration APPLY_LOCK_TTL = Duration.ofSeconds(30);

    private final JobCategoryMapper jobCategoryMapper;
    private final CompanyMapper companyMapper;
    private final JobCollectionMapper jobCollectionMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final AiInterviewMapper aiInterviewMapper;
    private final StringRedisTemplate redisTemplate;

    /** 自注入代理，保证 getJobDetail 内部调用走 @Cacheable 缓存 */
    @Lazy
    @Autowired
    private JobService self;

    @Override
    @Cacheable(cacheNames = "jobCategory", key = "'all'")
    public List<JobCategoryVO> getCategoryTree() {
        List<JobCategory> categories = jobCategoryMapper.selectList(
                new LambdaQueryWrapper<JobCategory>().orderByAsc(JobCategory::getSort));
        List<JobCategoryVO> all = Converter.INSTANCE.toJobCategoryVOList(categories);

        Map<Long, List<JobCategoryVO>> childrenMap = all.stream()
                .filter(c -> c.getParent_id() != null && c.getParent_id() != 0)
                .collect(Collectors.groupingBy(JobCategoryVO::getParent_id));
        List<JobCategoryVO> roots = all.stream()
                .filter(c -> c.getParent_id() == null || c.getParent_id() == 0)
                .collect(Collectors.toList());
        roots.forEach(root -> root.setChildren(childrenMap.getOrDefault(root.getId(), List.of())));
        return roots;
    }

    @Override
    @Cacheable(cacheNames = "jobList", key = "#dto.toString()")
    public Page<JobCardVO> pageJobs(JobQueryDTO dto) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, STATUS_ON_SHELF)
                .eq(dto.getCategory_id() != null, Job::getCategory_id, dto.getCategory_id());

        String keyword = dto.getKeyword();
        String searchType = dto.getSearch_type() == null ? "job" : dto.getSearch_type();
        if (StringUtils.hasText(keyword)) {
            if ("company".equals(searchType)) {
                List<Company> companies = companyMapper.selectList(
                        new LambdaQueryWrapper<Company>().like(Company::getName, keyword));
                List<Long> companyIds = companies.stream().map(Company::getId).collect(Collectors.toList());
                if (companyIds.isEmpty()) {
                    wrapper.eq(Job::getCompany_id, -1L);
                } else {
                    wrapper.in(Job::getCompany_id, companyIds);
                }
            } else {
                wrapper.like(Job::getTitle, keyword);
            }
        }
        wrapper.orderByDesc(Job::getId);

        Page<Job> page = page(new Page<>(dto.getPage_num(), dto.getPage_size()), wrapper);
        List<JobCardVO> records = Converter.INSTANCE.toJobCardVOList(page.getRecords());
        fillJobCompanies(records);
        Page<JobCardVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public JobDetailVO getJobDetail(Long id) {
        JobDetailVO detail = self.getJobBase(id);
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
            detail.setIs_collected(false);
            detail.setIs_applied(false);
            return detail;
        }
        Long collected = jobCollectionMapper.selectCount(new LambdaQueryWrapper<JobCollection>()
                .eq(JobCollection::getUser_id, userId)
                .eq(JobCollection::getJob_id, id));
        detail.setIs_collected(collected != null && collected > 0);

        Long applied = jobApplicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUser_id, userId)
                .eq(JobApplication::getJob_id, id));
        detail.setIs_applied(applied != null && applied > 0);
        return detail;
    }

    @Override
    @Cacheable(cacheNames = "jobDetail", key = "#id")
    public JobDetailVO getJobBase(Long id) {
        Job job = getById(id);
        if (job == null || job.getStatus() == null || job.getStatus() != STATUS_ON_SHELF) {
            throw new JobException("职位不存在或已下架");
        }
        JobDetailVO vo = Converter.INSTANCE.toJobDetailVO(job);
        Company company = companyMapper.selectById(job.getCompany_id());
        if (company != null) {
            vo.setCompany_name(company.getName());
            vo.setCompany_logo(company.getLogo());
            vo.setCompany_industry(company.getIndustry());
            vo.setCompany_scale(company.getScale());
            vo.setCompany_region(company.getRegion());
            vo.setCompany_intro(company.getIntro());
        }
        return vo;
    }

    @Override
    public Boolean toggleCollect(Long jobId) {
        self.getJobBase(jobId);
        Long userId = UserContext.getUserId();
        Long existed = jobCollectionMapper.selectCount(new LambdaQueryWrapper<JobCollection>()
                .eq(JobCollection::getUser_id, userId)
                .eq(JobCollection::getJob_id, jobId));
        if (existed != null && existed > 0) {
            jobCollectionMapper.deleteByUserAndJob(userId, jobId);
            return false;
        }
        JobCollection collection = new JobCollection();
        collection.setUser_id(userId);
        collection.setJob_id(jobId);
        jobCollectionMapper.insert(collection);
        return true;
    }

    @Override
    public void apply(Long jobId, ApplyJobDTO dto) {
        self.getJobBase(jobId);
        Long userId = UserContext.getUserId();

        String lockKey = APPLY_LOCK_KEY_PREFIX + userId + ":" + jobId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", APPLY_LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            throw new JobException("请勿重复投递");
        }

        Long existed = jobApplicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUser_id, userId)
                .eq(JobApplication::getJob_id, jobId));
        if (existed != null && existed > 0) {
            throw new JobException("请勿重复投递");
        }

        JobApplication application = new JobApplication();
        application.setUser_id(userId);
        application.setJob_id(jobId);
        application.setResume_url(dto.getResume_url());
        application.setStatus(0);
        try {
            jobApplicationMapper.insert(application);
        } catch (DuplicateKeyException e) {
            throw new JobException("请勿重复投递");
        } catch (RuntimeException e) {
            // 非重复键异常：立即释放锁，避免用户在 TTL 内被误判为"已投递"而无法重试
            redisTemplate.delete(lockKey);
            throw e;
        }
    }

    @Override
    public Long applyAiInterview(Long jobId) {
        self.getJobBase(jobId);
        Long userId = UserContext.getUserId();
        JobApplication application = jobApplicationMapper.selectOne(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUser_id, userId)
                .eq(JobApplication::getJob_id, jobId));
        if (application == null) {
            throw new JobException("请先投递该职位");
        }
        // 幂等：已申请过面试则直接返回原记录，避免重复插入
        AiInterview existed = aiInterviewMapper.selectOne(new LambdaQueryWrapper<AiInterview>()
                .eq(AiInterview::getUser_id, userId)
                .eq(AiInterview::getJob_id, jobId)
                .orderByDesc(AiInterview::getId)
                .last("LIMIT 1"));
        if (existed != null) {
            return existed.getId();
        }
        AiInterview interview = new AiInterview();
        interview.setUser_id(userId);
        interview.setJob_id(jobId);
        interview.setApplication_id(application.getId());
        interview.setStatus(0);
        aiInterviewMapper.insert(interview);
        return interview.getId();
    }

    private void fillJobCompanies(List<JobCardVO> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> companyIds = records.stream().map(JobCardVO::getCompany_id).collect(Collectors.toSet());
        Map<Long, Company> companyMap = companyMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c, (a, b) -> a));
        records.forEach(v -> {
            Company company = companyMap.get(v.getCompany_id());
            if (company != null) {
                v.setCompany_name(company.getName());
                v.setCompany_logo(company.getLogo());
                v.setCompany_industry(company.getIndustry());
                v.setCompany_scale(company.getScale());
                v.setCompany_region(company.getRegion());
            }
        });
    }
}
