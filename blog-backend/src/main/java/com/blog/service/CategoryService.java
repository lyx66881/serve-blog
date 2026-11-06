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
import com.blog.entity.Category;
import com.blog.mapper.CategoryMapper;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
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
}
