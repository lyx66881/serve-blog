package com.blog.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.tag.TagResponse;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;

@Service
public class TagService {

    private static final int MAX_TAGS_PER_BLOG = 10;
    private static final int MAX_TAG_NAME_LENGTH = 30;

    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public List<TagResponse> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName))
                .stream().map(t -> new TagResponse(t.getId(), t.getName())).toList();
    }

    /**
     * 按名称解析（不存在则创建）标签集合，去重、去除首尾空白。
     */
    @Transactional
    public List<Tag> resolveTags(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > MAX_TAG_NAME_LENGTH) {
                throw new BizException(ErrorCode.PARAM_INVALID, "标签长度不能超过 " + MAX_TAG_NAME_LENGTH + " 字符");
            }
            distinct.add(trimmed);
        }
        if (distinct.size() > MAX_TAGS_PER_BLOG) {
            throw new BizException(ErrorCode.PARAM_INVALID, "标签数量不能超过 " + MAX_TAGS_PER_BLOG + " 个");
        }

        List<Tag> result = new ArrayList<>();
        for (String name : distinct) {
            result.add(findOrCreate(name));
        }
        return result;
    }

    private Tag findOrCreate(String name) {
        Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name).last("LIMIT 1"));
        if (tag != null) {
            return tag;
        }
        Tag created = new Tag();
        created.setName(name);
        try {
            tagMapper.insert(created);
        } catch (DuplicateKeyException e) {
            // 并发创建同名标签时回退为查询
            tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name).last("LIMIT 1"));
            if (tag == null) {
                throw e;
            }
            return tag;
        }
        return created;
    }
}
