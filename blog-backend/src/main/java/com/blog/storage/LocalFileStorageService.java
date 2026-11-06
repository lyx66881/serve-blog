package com.blog.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.config.AppProperties;

import jakarta.annotation.PostConstruct;

/**
 * 本地磁盘文件存储实现。
 *
 * 目录结构：{base-dir}/{bizType}/{yyyy}/{MM}/{uuid}.{ext}
 * 安全措施：
 *  - 存储文件名由服务端随机生成，不使用用户输入的文件名；
 *  - 扩展名从白名单 MIME 类型映射，不信任原始文件名；
 *  - 删除时 normalize + startsWith 校验，防止路径穿越。
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    public static final Set<String> BIZ_TYPES = Set.of("avatar", "cover", "article");

    /** 允许的图片 MIME 类型 -> 扩展名 */
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp");

    private final AppProperties props;
    private Path baseDir;
    private long maxSizeBytes;

    public LocalFileStorageService(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() throws IOException {
        this.baseDir = Paths.get(props.getUpload().getBaseDir()).toAbsolutePath().normalize();
        this.maxSizeBytes = props.getUpload().getMaxSizeMb() * 1024L * 1024L;
        Files.createDirectories(baseDir);
        log.info("文件存储目录: {}", baseDir);
    }

    @Override
    public StoredFile store(MultipartFile file, String bizType) {
        if (!BIZ_TYPES.contains(bizType)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "无效的文件业务类型: " + bizType);
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String ext = ALLOWED_CONTENT_TYPES.get(contentType);
        if (ext == null) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        LocalDate now = LocalDate.now();
        String monthDir = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = bizType + "/" + monthDir + "/" + storedName;

        Path target = baseDir.resolve(relativePath).normalize();
        if (!target.startsWith(baseDir)) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("保存上传文件失败: {}", relativePath, e);
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String url = props.getUpload().getBaseUrl() + "/" + relativePath;
        String originalName = file.getOriginalFilename() == null ? storedName : file.getOriginalFilename();
        return new StoredFile(relativePath, url, originalName, storedName, file.getSize(), contentType);
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path target = baseDir.resolve(relativePath).normalize();
        if (!target.startsWith(baseDir)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "非法的文件路径");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.error("删除文件失败: {}", relativePath, e);
        }
    }
}
