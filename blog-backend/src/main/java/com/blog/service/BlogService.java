package com.blog.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.common.util.TipTapTextExtractor;
import com.blog.dto.blog.BlogAuthorResponse;
import com.blog.dto.blog.BlogResponse;
import com.blog.dto.blog.BlogSaveRequest;
import com.blog.dto.blog.BlogSummaryResponse;
import com.blog.dto.tag.TagResponse;
import com.blog.entity.Blog;
import com.blog.entity.BlogTag;
import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.mapper.BlogMapper;
import com.blog.mapper.BlogTagMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.TagMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class BlogService {

    private static final String SORT_LATEST = "latest";
    private static final String SORT_HOT = "hot";
    private static final Set<String> LIST_EXCLUDED_COLUMNS = Set.of("content", "content_html", "content_text");
    private static final int MAX_PAGE_SIZE = 50;

    private final BlogMapper blogMapper;
    private final BlogTagMapper blogTagMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final TagService tagService;

    public BlogService(BlogMapper blogMapper, BlogTagMapper blogTagMapper, UserMapper userMapper,
                       CategoryMapper categoryMapper, TagMapper tagMapper, TagService tagService) {
        this.blogMapper = blogMapper;
        this.blogTagMapper = blogTagMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.tagService = tagService;
    }

    // ==================== 写操作 ====================

    @Transactional
    public BlogResponse create(Long authorId, BlogSaveRequest req) {
        String content = requireValidContent(req.content());
        String title = requireValidTitle(req.title());

        Blog blog = new Blog();
        blog.setAuthorId(authorId);
        blog.setTitle(title);
        blog.setCoverImage(req.coverImage());
        blog.setCategoryId(requireValidCategoryId(req.categoryId()));
        blog.setContent(content);
        blog.setContentHtml(req.contentHtml());
        blog.setContentText(TipTapTextExtractor.extractText(req.content()));
        blog.setSummary(resolveSummary(req.summary(), blog.getContentText()));
        blog.setContentVersion("1");

        String status = req.status() == null ? Blog.STATUS_DRAFT : req.status();
        if (!Blog.STATUS_DRAFT.equals(status) && !Blog.STATUS_PUBLISHED.equals(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 draft / published");
        }
        blog.setStatus(status);
        if (Blog.STATUS_PUBLISHED.equals(status)) {
            blog.setPublishedAt(LocalDateTime.now());
        }
        blog.setIsPublic(Boolean.TRUE.equals(req.isPublic()) ? 1 : 0);
        blogMapper.insert(blog);
        saveBlogTags(blog.getId(), req.tags());

        return getDetail(blog.getId(), authorId);
    }

    @Transactional
    public BlogResponse update(Long operatorId, Long blogId, BlogSaveRequest req) {
        Blog blog = requireOwnedBlog(operatorId, blogId);

        if (req.title() != null) {
            blog.setTitle(requireValidTitle(req.title()));
        }
        if (req.content() != null) {
            blog.setContent(requireValidContent(req.content()));
            blog.setContentText(TipTapTextExtractor.extractText(req.content()));
            blog.setSummary(resolveSummary(req.summary(), blog.getContentText()));
        } else if (req.summary() != null) {
            blog.setSummary(resolveSummary(req.summary(), blog.getContentText()));
        }
        if (req.contentHtml() != null) {
            blog.setContentHtml(req.contentHtml());
        }
        if (req.coverImage() != null) {
            blog.setCoverImage(req.coverImage());
        }
        if (req.categoryId() != null) {
            blog.setCategoryId(requireValidCategoryId(req.categoryId()));
        }
        if (req.isPublic() != null) {
            blog.setIsPublic(Boolean.TRUE.equals(req.isPublic()) ? 1 : 0);
        }
        blogMapper.updateById(blog);
        if (req.tags() != null) {
            saveBlogTags(blogId, req.tags());
        }

        return getDetail(blogId, operatorId);
    }

    @Transactional
    public BlogResponse publish(Long operatorId, Long blogId) {
        Blog blog = requireOwnedBlog(operatorId, blogId);
        if (!Blog.STATUS_PUBLISHED.equals(blog.getStatus())) {
            blog.setStatus(Blog.STATUS_PUBLISHED);
            if (blog.getPublishedAt() == null) {
                blog.setPublishedAt(LocalDateTime.now());
            }
            blogMapper.updateById(blog);
        }
        return getDetail(blogId, operatorId);
    }

    @Transactional
    public BlogResponse unpublish(Long operatorId, Long blogId) {
        Blog blog = requireOwnedBlog(operatorId, blogId);
        if (!Blog.STATUS_OFFLINE.equals(blog.getStatus())) {
            blog.setStatus(Blog.STATUS_OFFLINE);
            blogMapper.updateById(blog);
        }
        return getDetail(blogId, operatorId);
    }

    @Transactional
    public void delete(Long operatorId, Long blogId) {
        requireOwnedBlog(operatorId, blogId);
        blogMapper.deleteById(blogId);
        cleanTagRelations(blogId);
    }

    // ==================== 读操作 ====================

    /**
     * 博客详情：已发布博客对所有人可见，非公开博客仅作者本人可见（否则视为不存在）。
     */
    public BlogResponse getDetail(Long blogId, Long viewerId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        boolean isAuthor = viewerId != null && viewerId.equals(blog.getAuthorId());
        if (!Blog.STATUS_PUBLISHED.equals(blog.getStatus()) && !isAuthor) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        if (!isAuthor && !isPublicBlog(blog)) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        if (Blog.STATUS_PUBLISHED.equals(blog.getStatus())) {
            blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                    .eq(Blog::getId, blogId)
                    .setSql("view_count = view_count + 1"));
            blog.setViewCount(blog.getViewCount() == null ? 1 : blog.getViewCount() + 1);
        }
        return toDetailResponse(blog, loadTags(List.of(blogId)).getOrDefault(blogId, List.of()));
    }

    /**
     * 公开博客列表（仅已发布且公开；最新 / 热门 / 筛选均只展示公开文章）。
     */
    public PageResult<BlogSummaryResponse> listPublic(Long page, Long pageSize, String keyword,
                                                      Long categoryId, Long tagId, Long authorId, String sort) {
        long[] paging = clampPaging(page, pageSize);
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .eq(Blog::getStatus, Blog.STATUS_PUBLISHED)
                .eq(Blog::getIsPublic, 1)
                .select(Blog.class, f -> !LIST_EXCLUDED_COLUMNS.contains(f.getColumn()));

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Blog::getTitle, kw).or().like(Blog::getContentText, kw));
        }
        if (categoryId != null) {
            wrapper.eq(Blog::getCategoryId, categoryId);
        }
        if (authorId != null) {
            wrapper.eq(Blog::getAuthorId, authorId);
        }
        List<Long> blogIds = resolveBlogIdsByTag(tagId);
        if (blogIds != null) {
            if (blogIds.isEmpty()) {
                return emptyPage(paging[0], paging[1]);
            }
            wrapper.in(Blog::getId, blogIds);
        }

        if (SORT_HOT.equals(sort)) {
            wrapper.orderByDesc(Blog::getViewCount).orderByDesc(Blog::getPublishedAt);
        } else {
            wrapper.orderByDesc(Blog::getPublishedAt).orderByDesc(Blog::getCreatedAt);
        }

        Page<Blog> result = blogMapper.selectPage(Page.of(paging[0], paging[1]), wrapper);
        return PageResult.of(result, blog -> toSummaryResponse(blog, result.getRecords()));
    }

    /**
     * 用户主页博客列表（仅已发布）：访问者为主页主人时包含未公开文章，他人仅见公开文章。
     */
    public PageResult<BlogSummaryResponse> listByAuthor(Long page, Long pageSize, Long authorId, Long viewerId) {
        long[] paging = clampPaging(page, pageSize);
        boolean owner = viewerId != null && viewerId.equals(authorId);
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .eq(Blog::getStatus, Blog.STATUS_PUBLISHED)
                .eq(Blog::getAuthorId, authorId)
                .select(Blog.class, f -> !LIST_EXCLUDED_COLUMNS.contains(f.getColumn()));
        if (!owner) {
            wrapper.eq(Blog::getIsPublic, 1);
        }
        wrapper.orderByDesc(Blog::getPublishedAt).orderByDesc(Blog::getCreatedAt);

        Page<Blog> result = blogMapper.selectPage(Page.of(paging[0], paging[1]), wrapper);
        return PageResult.of(result, blog -> toSummaryResponse(blog, result.getRecords()));
    }

    /**
     * 我的博客列表（含草稿 / 下架），可选按状态筛选。
     */
    public PageResult<BlogSummaryResponse> listMine(String status, Long page, Long pageSize) {
        long[] paging = clampPaging(page, pageSize);
        Long userId = SecurityUtils.currentUserId();

        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .eq(Blog::getAuthorId, userId)
                .select(Blog.class, f -> !LIST_EXCLUDED_COLUMNS.contains(f.getColumn()));
        if (status != null && !status.isBlank()) {
            if (!isKnownStatus(status)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 draft / published / offline");
            }
            wrapper.eq(Blog::getStatus, status);
        }
        wrapper.orderByDesc(Blog::getUpdatedAt);

        Page<Blog> result = blogMapper.selectPage(Page.of(paging[0], paging[1]), wrapper);
        return PageResult.of(result, blog -> toSummaryResponse(blog, result.getRecords()));
    }

    // ==================== 管理端操作（/api/v1/admin/**，仅管理员） ====================

    /**
     * 管理端博客列表：全部作者、全部状态；keyword 匹配标题或作者用户名/昵称。
     */
    public PageResult<BlogSummaryResponse> adminList(Long page, Long pageSize, String keyword,
                                                     String status, Long categoryId, Long authorId) {
        long[] paging = clampPaging(page, pageSize);

        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .select(Blog.class, f -> !LIST_EXCLUDED_COLUMNS.contains(f.getColumn()));
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<Long> matchedAuthorIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getUsername, kw).or().like(User::getNickname, kw))
                    .stream().map(User::getId).toList();
            wrapper.and(w -> {
                w.like(Blog::getTitle, kw);
                if (!matchedAuthorIds.isEmpty()) {
                    w.or().in(Blog::getAuthorId, matchedAuthorIds);
                }
            });
        }
        if (status != null && !status.isBlank()) {
            if (!isKnownStatus(status)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 draft / published / offline");
            }
            wrapper.eq(Blog::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Blog::getCategoryId, categoryId);
        }
        if (authorId != null) {
            wrapper.eq(Blog::getAuthorId, authorId);
        }
        wrapper.orderByDesc(Blog::getCreatedAt);

        Page<Blog> result = blogMapper.selectPage(Page.of(paging[0], paging[1]), wrapper);
        return PageResult.of(result, blog -> toSummaryResponse(blog, result.getRecords()));
    }

    /**
     * 管理端博客详情：不校验作者与公开性，不增加浏览量。
     */
    public BlogResponse adminDetail(Long blogId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        return toDetailResponse(blog, loadTags(List.of(blogId)).getOrDefault(blogId, List.of()));
    }

    /**
     * 管理端修改博客状态（上架 / 下架 / 转草稿）。
     */
    @Transactional
    public BlogResponse adminUpdateStatus(Long blogId, String status) {
        if (!isKnownStatus(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 draft / published / offline");
        }
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        blog.setStatus(status);
        if (Blog.STATUS_PUBLISHED.equals(status) && blog.getPublishedAt() == null) {
            blog.setPublishedAt(LocalDateTime.now());
        }
        blogMapper.updateById(blog);
        return toDetailResponse(blog, loadTags(List.of(blogId)).getOrDefault(blogId, List.of()));
    }

    /**
     * 管理端删除博客（逻辑删除）。
     */
    @Transactional
    public void adminDelete(Long blogId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        blogMapper.deleteById(blogId);
        cleanTagRelations(blogId);
    }

    // ==================== 私有方法 ====================

    /**
     * 清理博客-标签关联（博客逻辑删除后关联无意义，且会影响管理端标签计数）。
     */
    private void cleanTagRelations(Long blogId) {
        blogTagMapper.delete(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getBlogId, blogId));
    }

    private String requireValidTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "博客标题不能为空");
        }
        if (title.length() > 200) {
            throw new BizException(ErrorCode.PARAM_INVALID, "博客标题最长 200 字符");
        }
        return title.trim();
    }

    private String requireValidContent(JsonNode content) {
        if (content == null || content.isNull()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "博客正文 content 不能为空");
        }
        if (!content.isObject()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "博客正文 content 必须是 TipTap JSON 对象");
        }
        return content.toString();
    }

    private Long requireValidCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return categoryId;
    }

    private String resolveSummary(String summary, String contentText) {
        if (summary != null && !summary.isBlank()) {
            return TipTapTextExtractor.summarize(summary.trim(), 500);
        }
        return TipTapTextExtractor.summarize(contentText, 200);
    }

    private Blog requireOwnedBlog(Long operatorId, Long blogId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        if (!operatorId.equals(blog.getAuthorId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return blog;
    }

    private void saveBlogTags(Long blogId, List<String> tags) {
        blogTagMapper.delete(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getBlogId, blogId));
        List<Tag> resolved = tagService.resolveTags(tags);
        for (Tag tag : resolved) {
            BlogTag blogTag = new BlogTag();
            blogTag.setBlogId(blogId);
            blogTag.setTagId(tag.getId());
            blogTagMapper.insert(blogTag);
        }
    }

    /**
     * 按标签筛选博客 ID 列表；tagId 为 null 返回 null（不筛选）。
     */
    private List<Long> resolveBlogIdsByTag(Long tagId) {
        if (tagId == null) {
            return null;
        }
        return blogTagMapper.selectList(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getTagId, tagId))
                .stream().map(BlogTag::getBlogId).distinct().toList();
    }

    private long[] clampPaging(Long page, Long pageSize) {
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 10 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        return new long[]{p, s};
    }

    private PageResult<BlogSummaryResponse> emptyPage(long page, long pageSize) {
        return new PageResult<>(List.of(), 0, page, pageSize, 0);
    }

    private boolean isKnownStatus(String status) {
        return Blog.STATUS_DRAFT.equals(status) || Blog.STATUS_PUBLISHED.equals(status)
                || Blog.STATUS_OFFLINE.equals(status);
    }

    private boolean isPublicBlog(Blog blog) {
        return blog.getIsPublic() != null && blog.getIsPublic() == 1;
    }

    // ==================== 响应组装 ====================

    private BlogSummaryResponse toSummaryResponse(Blog blog, List<Blog> context) {
        Map<Long, BlogAuthorResponse> authors = loadAuthors(context);
        Map<Long, String> categoryNames = loadCategoryNames(context);
        Map<Long, List<TagResponse>> tags = loadTags(context.stream().map(Blog::getId).toList());

        return new BlogSummaryResponse(
                blog.getId(),
                authors.get(blog.getAuthorId()),
                blog.getTitle(),
                blog.getSummary(),
                blog.getCoverImage(),
                blog.getCategoryId(),
                categoryNames.get(blog.getCategoryId()),
                tags.getOrDefault(blog.getId(), List.of()),
                blog.getStatus(),
                isPublicBlog(blog),
                blog.getViewCount(),
                blog.getPublishedAt(),
                blog.getCreatedAt(),
                blog.getUpdatedAt());
    }

    private BlogResponse toDetailResponse(Blog blog, List<TagResponse> tags) {
        Map<Long, BlogAuthorResponse> authors = loadAuthors(List.of(blog));
        Map<Long, String> categoryNames = loadCategoryNames(List.of(blog));

        return new BlogResponse(
                blog.getId(),
                authors.get(blog.getAuthorId()),
                blog.getTitle(),
                blog.getSummary(),
                blog.getCoverImage(),
                blog.getCategoryId(),
                categoryNames.get(blog.getCategoryId()),
                tags,
                blog.getStatus(),
                isPublicBlog(blog),
                blog.getContent(),
                blog.getContentHtml(),
                blog.getContentVersion(),
                blog.getViewCount(),
                blog.getPublishedAt(),
                blog.getCreatedAt(),
                blog.getUpdatedAt());
    }

    private Map<Long, BlogAuthorResponse> loadAuthors(List<Blog> blogs) {
        Set<Long> authorIds = blogs.stream().map(Blog::getAuthorId).collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> new BlogAuthorResponse(
                        u.getId(), u.getUsername(), u.getNickname(), u.getAvatar()), (a, b) -> a));
    }

    private Map<Long, String> loadCategoryNames(List<Blog> blogs) {
        Set<Long> categoryIds = blogs.stream().map(Blog::getCategoryId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private Map<Long, List<TagResponse>> loadTags(List<Long> blogIds) {
        if (blogIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BlogTag> relations = blogTagMapper.selectList(
                new LambdaQueryWrapper<BlogTag>().in(BlogTag::getBlogId, blogIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> tagIds = relations.stream().map(BlogTag::getTagId).collect(Collectors.toSet());
        Map<Long, TagResponse> tagsById = tagMapper.selectByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, t -> new TagResponse(t.getId(), t.getName()), (a, b) -> a));
        return relations.stream()
                .collect(Collectors.groupingBy(BlogTag::getBlogId,
                        Collectors.mapping(r -> tagsById.get(r.getTagId()),
                                Collectors.collectingAndThen(Collectors.toList(), list ->
                                        list.stream().filter(java.util.Objects::nonNull).toList()))));
    }
}
