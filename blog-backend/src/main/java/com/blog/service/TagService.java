package com.blog.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.admin.AdminTagResponse;
import com.blog.dto.tag.TagResponse;
import com.blog.entity.BlogTag;
import com.blog.entity.Tag;
import com.blog.mapper.BlogTagMapper;
import com.blog.mapper.TagMapper;

@Service
public class TagService {

    private static final int MAX_TAGS_PER_BLOG = 10;
    private static final int MAX_TAG_NAME_LENGTH = 30;

    private final TagMapper tagMapper;
    private final BlogTagMapper blogTagMapper;

    public TagService(TagMapper tagMapper, BlogTagMapper blogTagMapper) {
        this.tagMapper = tagMapper;
        this.blogTagMapper = blogTagMapper;
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

    // ==================== 管理端 ====================

    /**
     * 管理端：全量标签列表，附关联博客数（按 blog_tags 关联计数）。
     */
    public List<AdminTagResponse> adminList() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName));
        if (tags.isEmpty()) {
            return List.of();
        }
        QueryWrapper<BlogTag> qw = new QueryWrapper<BlogTag>()
                .select("tag_id AS tagId", "COUNT(*) AS cnt")
                .groupBy("tag_id");
        Map<Long, Long> counts = new HashMap<>();
        for (Map<String, Object> row : blogTagMapper.selectMaps(qw)) {
            Object tid = row.get("tagId");
            Object cnt = row.get("cnt");
            if (tid != null && cnt != null) {
                counts.put(Long.valueOf(tid.toString()), Long.valueOf(cnt.toString()));
            }
        }
        return tags.stream()
                .map(t -> new AdminTagResponse(t.getId(), t.getName(), counts.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    /**
     * 管理端：重命名标签（全局唯一，排除自身）。
     */
    public TagResponse adminRename(Long id, String name) {
        Tag tag = requireTag(id);
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "标签名不能为空");
        }
        if (trimmed.length() > MAX_TAG_NAME_LENGTH) {
            throw new BizException(ErrorCode.PARAM_INVALID, "标签长度不能超过 " + MAX_TAG_NAME_LENGTH + " 字符");
        }
        Long duplicated = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, trimmed).ne(Tag::getId, id));
        if (duplicated != null && duplicated > 0) {
            throw new BizException(ErrorCode.CONFLICT, "标签名已存在");
        }
        tag.setName(trimmed);
        tagMapper.updateById(tag);
        return new TagResponse(tag.getId(), tag.getName());
    }

    /**
     * 管理端：删除标签并清除其与博客的关联。
     */
    @Transactional
    public void adminDelete(Long id) {
        requireTag(id);
        blogTagMapper.delete(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getTagId, id));
        tagMapper.deleteById(id);
    }

    private Tag requireTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        return tag;
    }
}
