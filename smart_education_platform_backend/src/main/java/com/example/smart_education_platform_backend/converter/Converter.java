package com.example.smart_education_platform_backend.converter;

import com.example.smart_education_platform_backend.model.dto.AdminCourseDTO;
import com.example.smart_education_platform_backend.model.dto.AdminJobDTO;
import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.CourseCategory;
import com.example.smart_education_platform_backend.model.entity.CourseChapter;
import com.example.smart_education_platform_backend.model.entity.CourseComment;
import com.example.smart_education_platform_backend.model.entity.CourseQa;
import com.example.smart_education_platform_backend.model.entity.Job;
import com.example.smart_education_platform_backend.model.entity.JobCategory;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.AdminCommentVO;
import com.example.smart_education_platform_backend.model.vo.AdminQuestionVO;
import com.example.smart_education_platform_backend.model.vo.CourseCardVO;
import com.example.smart_education_platform_backend.model.vo.CourseCategoryVO;
import com.example.smart_education_platform_backend.model.vo.CourseChapterVO;
import com.example.smart_education_platform_backend.model.vo.CourseCommentVO;
import com.example.smart_education_platform_backend.model.vo.CourseDetailVO;
import com.example.smart_education_platform_backend.model.vo.CourseQuestionVO;
import com.example.smart_education_platform_backend.model.vo.JobCardVO;
import com.example.smart_education_platform_backend.model.vo.JobCategoryVO;
import com.example.smart_education_platform_backend.model.vo.JobDetailVO;
import com.example.smart_education_platform_backend.model.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface Converter {

    Converter INSTANCE = Mappers.getMapper(Converter.class);

    UserVO toUserVO(Users users);

    List<UserVO> toUserVOList(List<Users> users);

    CourseCardVO toCourseCardVO(Course course);

    List<CourseCardVO> toCourseCardVOList(List<Course> courses);

    CourseDetailVO toCourseDetailVO(Course course);

    CourseChapterVO toCourseChapterVO(CourseChapter chapter);

    List<CourseChapterVO> toCourseChapterVOList(List<CourseChapter> chapters);

    CourseCategoryVO toCourseCategoryVO(CourseCategory category);

    List<CourseCategoryVO> toCourseCategoryVOList(List<CourseCategory> categories);

    CourseCommentVO toCourseCommentVO(CourseComment comment);

    List<CourseCommentVO> toCourseCommentVOList(List<CourseComment> comments);

    CourseQuestionVO toCourseQuestionVO(CourseQa qa);

    List<CourseQuestionVO> toCourseQuestionVOList(List<CourseQa> qas);

    JobCategoryVO toJobCategoryVO(JobCategory category);

    List<JobCategoryVO> toJobCategoryVOList(List<JobCategory> categories);

    JobCardVO toJobCardVO(Job job);

    List<JobCardVO> toJobCardVOList(List<Job> jobs);

    JobDetailVO toJobDetailVO(Job job);

    Course toCourse(AdminCourseDTO dto);

    Job toJob(AdminJobDTO dto);

    List<AdminCommentVO> toAdminCommentVOList(List<CourseComment> comments);

    List<AdminQuestionVO> toAdminQuestionVOList(List<CourseQa> qas);
}
