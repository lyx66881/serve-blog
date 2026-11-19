package com.blog.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.category.CategoryCreateRequest;
import com.blog.dto.category.CategoryResponse;
import com.blog.entity.Blog;
import com.blog.entity.Category;
import com.blog.mapper.BlogMapper;
import com.blog.mapper.CategoryMapper;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final BlogMapper blogMapper;

    public CategoryService(CategoryMapper categoryMapper, BlogMapper blogMapper) {
        this.categoryMapper = categoryMapper;
        this.blogMapper = blogMapper;
    }

    public List<CategoryResponse> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getId))
                .stream().map(c -> new CategoryResponse(c.getId(), c.getName(), c.getDescription())).toList();
    }

    public CategoryResponse getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest req) {
        String name = req.name().trim();
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getName, name)) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "分类名已存在");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(req.description());
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.CONFLICT, "分类名已存在");
        }
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

    // ==================== 管理端 ====================

    /**
     * 管理端：更新分类（名称唯一，排除自身）。
     */
    public CategoryResponse adminUpdate(Long id, CategoryCreateRequest req) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        String name = req.name().trim();
        Long duplicated = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name).ne(Category::getId, id));
        if (duplicated != null && duplicated > 0) {
            throw new BizException(ErrorCode.CONFLICT, "分类名已存在");
        }
        category.setName(name);
        category.setDescription(req.description());
        categoryMapper.updateById(category);
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

    /**
     * 管理端：删除分类；分类下仍有博客时拒绝删除。
     */
    @Transactional
    public void adminDelete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        Long inUse = blogMapper.selectCount(new LambdaQueryWrapper<Blog>().eq(Blog::getCategoryId, id));
        if (inUse != null && inUse > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该分类下仍有 " + inUse + " 篇博客，请先转移后再删除");
        }
        categoryMapper.deleteById(id);
    }
}
