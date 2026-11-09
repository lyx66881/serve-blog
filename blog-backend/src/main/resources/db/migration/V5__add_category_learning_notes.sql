-- 新增默认分类：学习笔记（语言语法、框架原理、开发踩坑等知识沉淀）
INSERT INTO categories (name, description, created_at, updated_at)
SELECT '学习笔记', '语言语法、框架原理、开发踩坑等知识沉淀', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '学习笔记' AND deleted = 0);
