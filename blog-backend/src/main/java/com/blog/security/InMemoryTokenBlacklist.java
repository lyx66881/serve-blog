package com.blog.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 退出登录的 token 黑名单（内存实现）。
 *
 * 说明：重启后黑名单清空，失效 token 在剩余有效期内仍可用（风险有限）；
 * 未来引入 Redis 时可平滑替换本实现，保持接口不变。
 */
@Component
public class InMemoryTokenBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void add(String token, long expireAtMillis) {
        purge();
        blacklist.put(token, expireAtMillis);
    }

    public boolean contains(String token) {
        Long expireAt = blacklist.get(token);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    private void purge() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
