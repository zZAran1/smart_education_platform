package com.example.smart_education_platform_backend.converter;

import com.example.smart_education_platform_backend.model.entity.Course;
import com.example.smart_education_platform_backend.model.entity.CourseCategory;
import com.example.smart_education_platform_backend.model.entity.CourseChapter;
import com.example.smart_education_platform_backend.model.entity.Users;
import com.example.smart_education_platform_backend.model.vo.CourseCardVO;
import com.example.smart_education_platform_backend.model.vo.CourseCategoryVO;
import com.example.smart_education_platform_backend.model.vo.CourseChapterVO;
import com.example.smart_education_platform_backend.model.vo.CourseDetailVO;
import com.example.smart_education_platform_backend.model.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface Converter {

    Converter INSTANCE = Mappers.getMapper(Converter.class);

    UserVO toUserVO(Users users);

    CourseCardVO toCourseCardVO(Course course);

    List<CourseCardVO> toCourseCardVOList(List<Course> courses);

    CourseDetailVO toCourseDetailVO(Course course);

    CourseChapterVO toCourseChapterVO(CourseChapter chapter);

    List<CourseChapterVO> toCourseChapterVOList(List<CourseChapter> chapters);

    CourseCategoryVO toCourseCategoryVO(CourseCategory category);

    List<CourseCategoryVO> toCourseCategoryVOList(List<CourseCategory> categories);
}
