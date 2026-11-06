package com.blog.common;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 统一分页结构。
 * <pre>
 * { "items": [...], "total": 100, "page": 1, "pageSize": 10, "pages": 10 }
 * </pre>
 */
public record PageResult<T>(List<T> items, long total, long page, long pageSize, long pages) {

    public static <T, R> PageResult<R> of(IPage<T> p, java.util.function.Function<T, R> mapper) {
        List<R> items = p.getRecords().stream().map(mapper).toList();
        return new PageResult<>(items, p.getTotal(), p.getCurrent(), p.getSize(), p.getPages());
    }

    public static <T> PageResult<T> of(IPage<T> p) {
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize(), p.getPages());
    }
}
