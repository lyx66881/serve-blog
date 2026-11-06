package com.blog.common.util;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 从 TipTap / ProseMirror JSON 文档中提取纯文本，用于搜索与摘要。
 * 不修改原始 JSON 结构。
 */
public final class TipTapTextExtractor {

    private static final Set<String> BLOCK_TYPES = Set.of(
            "doc", "paragraph", "heading", "blockquote", "codeBlock",
            "bulletList", "orderedList", "listItem",
            "table", "tableRow", "tableCell", "tableHeader");

    private TipTapTextExtractor() {
    }

    /**
     * 提取正文纯文本。块级节点之间以换行分隔。
     */
    public static String extractText(JsonNode doc) {
        if (doc == null || doc.isNull() || doc.isMissingNode()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        walk(doc, sb);
        return sb.toString().replaceAll("\n{2,}", "\n").trim();
    }

    /**
     * 基于纯文本生成摘要：换行合并为空格后截取前 limit 个字符。
     */
    public static String summarize(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String flat = text.replaceAll("\\s*\\n+\\s*", " ").trim();
        if (flat.codePointCount(0, flat.length()) <= limit) {
            return flat;
        }
        return flat.substring(0, flat.offsetByCodePoints(0, limit));
    }

    private static void walk(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isTextual()) {
            sb.append(node.asText());
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> walk(child, sb));
            return;
        }
        if (node.isObject()) {
            String type = node.path("type").asText("");
            if ("text".equals(type)) {
                sb.append(node.path("text").asText(""));
                return;
            }
            if ("hardBreak".equals(type)) {
                sb.append('\n');
            }
            if ("image".equals(type)) {
                String alt = node.path("attrs").path("alt").asText("");
                if (!alt.isBlank()) {
                    sb.append(alt);
                }
            }
            walk(node.get("content"), sb);
            if (BLOCK_TYPES.contains(type)) {
                sb.append('\n');
            }
        }
    }
}
