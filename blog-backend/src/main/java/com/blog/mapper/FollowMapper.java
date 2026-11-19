package com.blog.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Follow;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}
