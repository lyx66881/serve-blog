package com.blog.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.dto.blog.BlogAuthorResponse;
import com.blog.dto.comment.AdminCommentResponse;
import com.blog.dto.comment.CommentCreateRequest;
import com.blog.dto.comment.CommentResponse;
import com.blog.entity.Blog;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.mapper.BlogMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.SecurityUtils;

/**
 * 评论业务：两层结构（顶级评论 + 楼中楼回复）。
 * <p>
 * 公开接口只展示 status=visible 且未删除的评论；隐藏/删除仅管理端与作者侧可见差异。
 */
@Service
public class CommentService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final CommentMapper commentMapper;
    private final BlogMapper blogMapper;
    private final UserMapper userMapper;

    public CommentService(CommentMapper commentMapper, BlogMapper blogMapper, UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
    }

    // ==================== 公开接口 ====================

    /**
     * 某博客的顶级评论分页（不含回复，回复数以 replyCount 汇总，按需调 replies 接口拉取）。
     */
    public PageResult<CommentResponse> listTopLevel(Long blogId, Long page, Long pageSize, Long viewerId) {
        requireVisibleBlog(blogId, viewerId);
        long[] paging = clampPaging(page, pageSize);

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getBlogId, blogId)
                .isNull(Comment::getParentId)
                .eq(Comment::getStatus, Comment.STATUS_VISIBLE)
                .orderByDesc(Comment::getCreatedAt)
                .orderByDesc(Comment::getId);
        Page<Comment> result = commentMapper.selectPage(Page.of(paging[0], paging[1]), wrapper);
        return PageResult.of(result, c -> toResponse(c, loadReplyCounts(idsOf(result.getRecords()))));
    }

    /**
     * 某条评论下的回复平铺分页（按时间正序）。
     */
    public PageResult<CommentResponse> listReplies(Long commentId, Long page, Long pageSize, Long viewerId) {
        Comment parent = commentMapper.selectById(commentId);
        if (parent == null || !Comment.STATUS_VISIBLE.equals(parent.getStatus())) {
            throw new BizException(ErrorCode.COMMENT_NOT_FOUND);
        }
        requireVisibleBlog(parent.getBlogId(), viewerId);
        long[] paging = clampPaging(page, pageSize);

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getParentId, commentId)
                .eq(Comment::getStatus, Comment.STATUS_VISIBLE)
                .orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId);
        Page<Comment> result = commentMapper.selectPage(Page.of(paging[0], paging[1]), wrapper);
        return PageResult.of(result, c -> toResponse(c, Map.of()));
    }

    /**
     * 发表评论 / 回复。
     * parentId 为空：顶级评论；parentId 非空：回复（parent 必须是同一文章下的顶级评论）。
     */
    @Transactional
    public CommentResponse create(Long userId, Long blogId, CommentCreateRequest req) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null || !Blog.STATUS_PUBLISHED.equals(blog.getStatus())
                || blog.getIsPublic() == null || blog.getIsPublic() != 1) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }

        String content = req.content() == null ? "" : req.content().trim();
        if (content.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "评论内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BizException(ErrorCode.PARAM_INVALID, "评论内容最长 1000 字符");
        }

        Comment comment = new Comment();
        comment.setBlogId(blogId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setStatus(Comment.STATUS_VISIBLE);

        if (req.parentId() != null) {
            Comment parent = commentMapper.selectById(req.parentId());
            if (parent == null || !parent.getBlogId().equals(blogId)
                    || parent.getParentId() != null
                    || !Comment.STATUS_VISIBLE.equals(parent.getStatus())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "被回复的评论不存在或不允许回复");
            }
            comment.setParentId(parent.getId());
            comment.setReplyToUserId(resolveReplyToUser(parent, req.replyToUserId()));
        }

        commentMapper.insert(comment);
        return toResponse(comment, Map.of());
    }

    /**
     * 删除评论（逻辑删除）：评论作者、文章作者或管理员可操作；顶级评论连同其回复一起删除。
     */
    @Transactional
    public void delete(Long operatorId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ErrorCode.COMMENT_NOT_FOUND);
        }
        Blog blog = blogMapper.selectById(comment.getBlogId());
        boolean commentAuthor = operatorId.equals(comment.getUserId());
        boolean blogAuthor = blog != null && operatorId.equals(blog.getAuthorId());
        if (!commentAuthor && !blogAuthor && !SecurityUtils.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        commentMapper.deleteById(commentId);
        if (comment.getParentId() == null) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getParentId, commentId)
                    .set(Comment::getDeleted, 1));
        }
    }

    // ==================== 管理端 ====================

    /**
     * 全站评论分页（管理端）：status 可选 visible / hidden，附文章标题。
     */
    public PageResult<AdminCommentResponse> adminPage(String status, Long page, Long pageSize) {
        if (status != null && !status.isBlank() && !Comment.STATUS_VISIBLE.equals(status)
                && !Comment.STATUS_HIDDEN.equals(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 visible / hidden");
        }
        long[] paging = clampPaging(page, pageSize);

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Comment::getStatus, status);
        }
        wrapper.orderByDesc(Comment::getCreatedAt).orderByDesc(Comment::getId);
        Page<Comment> result = commentMapper.selectPage(Page.of(paging[0], paging[1]), wrapper);

        Map<Long, BlogAuthorResponse> authors = loadAuthors(result.getRecords());
        Map<Long, String> blogTitles = loadBlogTitles(result.getRecords());
        return PageResult.of(result, c -> new AdminCommentResponse(
                c.getId(), c.getBlogId(), blogTitles.get(c.getBlogId()), c.getParentId(),
                authors.get(c.getUserId()), c.getContent(), c.getStatus(), c.getCreatedAt()));
    }

    /**
     * 管理端设置评论状态：visible-恢复显示 / hidden-隐藏。
     */
    @Transactional
    public AdminCommentResponse adminSetStatus(Long commentId, String status) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ErrorCode.COMMENT_NOT_FOUND);
        }
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .set(Comment::getStatus, status));
        comment.setStatus(status);
        Map<Long, BlogAuthorResponse> authors = loadAuthors(List.of(comment));
        Map<Long, String> blogTitles = loadBlogTitles(List.of(comment));
        return new AdminCommentResponse(comment.getId(), comment.getBlogId(),
                blogTitles.get(comment.getBlogId()), comment.getParentId(),
                authors.get(comment.getUserId()), comment.getContent(), status, comment.getCreatedAt());
    }

    // ==================== 博客侧计数 ====================

    /**
     * 批量统计博客可见评论数（含回复），供博客列表/详情 DTO 的 commentCount 使用。
     */
    public Map<Long, Long> countByBlogIds(Collection<Long> blogIds) {
        if (blogIds == null || blogIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<Comment> qw = new QueryWrapper<>();
        qw.select("blog_id", "COUNT(*) AS cnt")
                .in("blog_id", blogIds)
                .eq("status", Comment.STATUS_VISIBLE)
                .groupBy("blog_id");
        Map<Long, Long> counts = new HashMap<>();
        for (Map<String, Object> row : commentMapper.selectMaps(qw)) {
            Object blogId = row.get("blog_id");
            Object cnt = row.get("cnt");
            if (blogId != null && cnt != null) {
                counts.put(Long.valueOf(blogId.toString()), Long.valueOf(cnt.toString()));
            }
        }
        return counts;
    }

    public Long countByBlog(Long blogId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getBlogId, blogId)
                .eq(Comment::getStatus, Comment.STATUS_VISIBLE));
    }

    // ==================== 私有方法 ====================

    /**
     * 校验博客对当前访问者可见（已发布且公开，或访问者是作者）。
     */
    private Blog requireVisibleBlog(Long blogId, Long viewerId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        boolean isAuthor = viewerId != null && viewerId.equals(blog.getAuthorId());
        boolean publicVisible = Blog.STATUS_PUBLISHED.equals(blog.getStatus())
                && blog.getIsPublic() != null && blog.getIsPublic() == 1;
        if (!publicVisible && !isAuthor) {
            throw new BizException(ErrorCode.BLOG_NOT_FOUND);
        }
        return blog;
    }

    /**
     * 解析被回复人：不传默认为父评论作者；传入时必须是父评论作者或其可见回复的作者。
     */
    private Long resolveReplyToUser(Comment parent, Long replyToUserId) {
        Set<Long> validTargets = new HashSet<>();
        validTargets.add(parent.getUserId());
        List<Comment> replies = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getParentId, parent.getId())
                .eq(Comment::getStatus, Comment.STATUS_VISIBLE)
                .select(Comment::getUserId));
        replies.forEach(r -> validTargets.add(r.getUserId()));
        if (replyToUserId == null) {
            return parent.getUserId();
        }
        if (!validTargets.contains(replyToUserId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "被回复人不属于该评论");
        }
        return replyToUserId;
    }

    private CommentResponse toResponse(Comment comment, Map<Long, Long> replyCounts) {
        Map<Long, BlogAuthorResponse> authors = loadAuthors(List.of(comment));
        BlogAuthorResponse replyToUser = null;
        if (comment.getReplyToUserId() != null) {
            User u = userMapper.selectById(comment.getReplyToUserId());
            if (u != null) {
                replyToUser = toAuthor(u);
            }
        }
        long replyCount = comment.getParentId() == null
                ? replyCounts.getOrDefault(comment.getId(), 0L)
                : 0L;
        return new CommentResponse(
                comment.getId(),
                comment.getBlogId(),
                comment.getParentId(),
                authors.get(comment.getUserId()),
                comment.getContent(),
                replyToUser,
                replyCount,
                comment.getStatus(),
                comment.getCreatedAt());
    }

    private Map<Long, Long> loadReplyCounts(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<Comment> qw = new QueryWrapper<>();
        qw.select("parent_id", "COUNT(*) AS cnt")
                .in("parent_id", parentIds)
                .eq("status", Comment.STATUS_VISIBLE)
                .groupBy("parent_id");
        Map<Long, Long> counts = new HashMap<>();
        for (Map<String, Object> row : commentMapper.selectMaps(qw)) {
            Object parentId = row.get("parent_id");
            Object cnt = row.get("cnt");
            if (parentId != null && cnt != null) {
                counts.put(Long.valueOf(parentId.toString()), Long.valueOf(cnt.toString()));
            }
        }
        return counts;
    }

    private Map<Long, BlogAuthorResponse> loadAuthors(List<Comment> comments) {
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::toAuthor, (a, b) -> a));
    }

    private BlogAuthorResponse toAuthor(User user) {
        return new BlogAuthorResponse(user.getId(), user.getUsername(),
                user.getNickname() == null ? user.getUsername() : user.getNickname(), user.getAvatar());
    }

    private Map<Long, String> loadBlogTitles(List<Comment> comments) {
        Set<Long> blogIds = comments.stream().map(Comment::getBlogId).collect(Collectors.toSet());
        if (blogIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return blogMapper.selectByIds(blogIds).stream()
                .collect(Collectors.toMap(Blog::getId, Blog::getTitle, (a, b) -> a));
    }

    private List<Long> idsOf(List<Comment> comments) {
        return comments.stream().map(Comment::getId).toList();
    }

    private long[] clampPaging(Long page, Long pageSize) {
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 10 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        return new long[]{p, s};
    }
}
