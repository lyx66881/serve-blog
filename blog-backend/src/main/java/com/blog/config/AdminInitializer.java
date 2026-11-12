package com.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;

/**
 * 启动时确保初始管理员账号存在：
 *  - 按 app.admin.username 查找用户，不存在则创建（role=admin）；
 *  - 已存在但角色不是 admin 时提升为 admin；
 *  - 密码仅在创建时写入，已有账号不会被覆盖。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public AdminInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder, AppProperties appProperties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin conf = appProperties.getAdmin();
        String username = conf.getUsername().trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("LIMIT 1"));
        if (user == null) {
            User admin = new User();
            admin.setUsername(username);
            admin.setPassword(passwordEncoder.encode(conf.getPassword()));
            admin.setNickname(conf.getNickname());
            admin.setRole(User.ROLE_ADMIN);
            admin.setStatus(User.STATUS_NORMAL);
            userMapper.insert(admin);
            log.info("Initial admin account created: username={}", username);
            return;
        }
        if (!User.ROLE_ADMIN.equals(user.getRole())) {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .set(User::getRole, User.ROLE_ADMIN));
            log.info("Existing user '{}' promoted to admin", username);
        }
    }
}
