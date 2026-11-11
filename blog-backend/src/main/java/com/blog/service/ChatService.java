package com.blog.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.dto.blog.BlogAuthorResponse;
import com.blog.dto.chat.ConversationResponse;
import com.blog.dto.chat.MessageResponse;
import com.blog.dto.chat.SendMessageRequest;
import com.blog.entity.Conversation;
import com.blog.entity.Message;
import com.blog.entity.UploadedFile;
import com.blog.entity.User;
import com.blog.mapper.ConversationMapper;
import com.blog.mapper.MessageMapper;
import com.blog.mapper.UploadedFileMapper;
import com.blog.mapper.UserMapper;
import com.blog.websocket.ChatWebSocketHandler;

/**
 * 站内私信业务：双人会话（user_a_id 恒为较小 ID）+ 消息（支持文本与图片）。
 * <p>
 * 发送走 REST 保证可靠；在线时由 ChatWebSocketHandler 向双方长连接实时推送。
 * 图片消息：客户端先经 POST /upload/image?bizType=chat 上传拿 imageId，再随消息发送；
 * 服务端校验上传记录归属与业务类型，消息内存 image_url，content 作为可选配文。
 */
@Service
public class ChatService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int PREVIEW_LENGTH = 200;
    private static final String MSG_TEXT = "text";
    private static final String MSG_IMAGE = "image";

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UploadedFileMapper uploadedFileMapper;
    private final ChatWebSocketHandler wsHandler;
    private final SocialService socialService;

    public ChatService(ConversationMapper conversationMapper, MessageMapper messageMapper,
                       UserMapper userMapper, UploadedFileMapper uploadedFileMapper,
                       ChatWebSocketHandler wsHandler, SocialService socialService) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.uploadedFileMapper = uploadedFileMapper;
        this.wsHandler = wsHandler;
        this.socialService = socialService;
    }

    /**
     * 发送私信：对方须为存在用户且不能是自己；会话不存在自动创建；成功后实时推送双方。
     * imageId 非空时为图片消息（content 作为可选配文），否则为文本消息（content 必填）。
     */
    @Transactional
    public MessageResponse send(Long senderId, SendMessageRequest req) {
        if (req.toUserId().equals(senderId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能给自己发私信");
        }
        User toUser = userMapper.selectById(req.toUserId());
        if (toUser == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        String content = req.content() == null ? "" : req.content().trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BizException(ErrorCode.PARAM_INVALID, "消息内容最长 2000 字符");
        }

        String msgType = MSG_TEXT;
        String imageUrl = null;
        Integer imageWidth = null;
        Integer imageHeight = null;
        if (req.imageId() != null) {
            UploadedFile file = uploadedFileMapper.selectById(req.imageId());
            if (file == null) {
                throw new BizException(ErrorCode.FILE_NOT_FOUND);
            }
            if (!file.getUploaderId().equals(senderId)) {
                throw new BizException(ErrorCode.FORBIDDEN, "只能发送自己上传的图片");
            }
            if (!"chat".equals(file.getBizType())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "仅支持 chat 类型上传的图片");
            }
            msgType = MSG_IMAGE;
            imageUrl = file.getUrl();
            imageWidth = req.imageWidth();
            imageHeight = req.imageHeight();
        } else if (content.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "消息内容不能为空");
        }

        socialService.assertCanDm(senderId, toUser.getId(), toUser.getDmPolicy());

        Conversation conv = getOrCreateConversation(senderId, req.toUserId());
        LocalDateTime now = LocalDateTime.now();

        Message message = new Message();
        message.setConversationId(conv.getId());
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setImageUrl(imageUrl);
        message.setImageWidth(imageWidth);
        message.setImageHeight(imageHeight);
        message.setCreatedAt(now);
        messageMapper.insert(message);

        String preview = MSG_IMAGE.equals(msgType)
                ? "[图片]" + (content.isEmpty() ? "" : " " + content)
                : content;
        boolean senderIsA = conv.getUserAId().equals(senderId);
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conv.getId())
                .set(Conversation::getLastMessage, truncate(preview, PREVIEW_LENGTH))
                .set(Conversation::getLastMessageAt, now)
                .set(Conversation::getLastSenderId, senderId)
                .setSql(senderIsA ? "unread_b = unread_b + 1" : "unread_a = unread_a + 1"));
        conv.setLastMessage(truncate(preview, PREVIEW_LENGTH));
        conv.setLastMessageAt(now);
        conv.setLastSenderId(senderId);

        MessageResponse response = toMessageResponse(message);
        wsHandler.pushToUser(req.toUserId(), pushJson(response));
        wsHandler.pushToUser(senderId, pushJson(response));
        return response;
    }

    /**
     * 我的会话列表：按最后消息时间倒序，含对方用户、最后一条预览与我的未读数。
     */
    public PageResult<ConversationResponse> listConversations(Long page, Long pageSize, Long userId) {
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 20 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserAId, userId).or().eq(Conversation::getUserBId, userId)
                .orderByDesc(Conversation::getLastMessageAt);
        Page<Conversation> result = conversationMapper.selectPage(Page.of(p, s), wrapper);

        Set<Long> otherIds = result.getRecords().stream()
                .map(c -> otherId(c, userId)).collect(Collectors.toSet());
        Map<Long, BlogAuthorResponse> users = otherIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectByIds(otherIds).stream()
                        .collect(Collectors.toMap(User::getId, this::toAuthor, (a, b) -> a));

        return PageResult.of(result, c -> new ConversationResponse(
                c.getId(),
                users.get(otherId(c, userId)),
                new ConversationResponse.LastMessage(c.getLastSenderId(), c.getLastMessage(), c.getLastMessageAt()),
                (long) (c.getUserAId().equals(userId) ? c.getUnreadA() : c.getUnreadB()),
                c.getUpdatedAt()));
    }

    /**
     * 会话历史消息：按时间正序返回；拉取即把对方发来的未读消息全部置为已读并清零我的未读数。
     */
    @Transactional
    public PageResult<MessageResponse> listMessages(Long conversationId, Long page, Long pageSize, Long userId) {
        Conversation conv = requireMember(conversationId, userId);
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 20 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getId);
        Page<Message> result = messageMapper.selectPage(Page.of(p, s), wrapper);
        List<Message> ordered = result.getRecords().stream().sorted(java.util.Comparator.comparing(Message::getId)).toList();

        boolean iAmA = conv.getUserAId().equals(userId);
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(iAmA ? Conversation::getUnreadA : Conversation::getUnreadB, 0));
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .ne(Message::getSenderId, userId)
                .isNull(Message::getReadAt)
                .set(Message::getReadAt, LocalDateTime.now()));

        return new PageResult<>(ordered.stream().map(this::toMessageResponse).toList(),
                result.getTotal(), p, s, result.getPages());
    }

    /**
     * 我的未读消息总数（所有会话未读数求和），用于全局角标。
     */
    public long totalUnread(Long userId) {
        List<Conversation> convs = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .and(w -> w.eq(Conversation::getUserAId, userId).or().eq(Conversation::getUserBId, userId))
                .select(Conversation::getUserAId, Conversation::getUserBId,
                        Conversation::getUnreadA, Conversation::getUnreadB));
        return convs.stream().mapToLong(c -> c.getUserAId().equals(userId) ? c.getUnreadA() : c.getUnreadB()).sum();
    }

    // ==================== 私有方法 ====================

    private Conversation getOrCreateConversation(Long user1, Long user2) {
        long a = Math.min(user1, user2);
        long b = Math.max(user1, user2);
        Conversation conv = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserAId, a).eq(Conversation::getUserBId, b).last("LIMIT 1"));
        if (conv != null) {
            return conv;
        }
        Conversation created = new Conversation();
        created.setUserAId(a);
        created.setUserBId(b);
        created.setUnreadA(0);
        created.setUnreadB(0);
        try {
            conversationMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            // 并发下另一请求已创建，直接查回
            return conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                    .eq(Conversation::getUserAId, a).eq(Conversation::getUserBId, b).last("LIMIT 1"));
        }
    }

    private Conversation requireMember(Long conversationId, Long userId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || (!conv.getUserAId().equals(userId) && !conv.getUserBId().equals(userId))) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
        return conv;
    }

    private Long otherId(Conversation c, Long userId) {
        return c.getUserAId().equals(userId) ? c.getUserBId() : c.getUserAId();
    }

    private MessageResponse toMessageResponse(Message m) {
        User sender = userMapper.selectById(m.getSenderId());
        String msgType = m.getMsgType() == null ? "text" : m.getMsgType();
        return new MessageResponse(m.getId(), m.getConversationId(),
                sender == null ? null : toAuthor(sender), m.getContent(),
                msgType, m.getImageUrl(), m.getImageWidth(), m.getImageHeight(),
                m.getReadAt() != null, m.getCreatedAt());
    }

    private BlogAuthorResponse toAuthor(User user) {
        return new BlogAuthorResponse(user.getId(), user.getUsername(),
                user.getNickname() == null ? user.getUsername() : user.getNickname(), user.getAvatar());
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String pushJson(MessageResponse response) {
        try {
            return wsHandler.getObjectMapper().writeValueAsString(Map.of("type", "message", "data", response));
        } catch (Exception e) {
            return "{\"type\":\"message\"}";
        }
    }
}
