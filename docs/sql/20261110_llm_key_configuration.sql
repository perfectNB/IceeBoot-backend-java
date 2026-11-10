-- 已有 base 数据库升级用；仅当对应列尚不存在时执行。
-- 新部署也可沿用现有 DatabaseInitService，执行前请检查实际结构。
ALTER TABLE `t_ai_llm_key`
    ADD COLUMN `name` varchar(255) DEFAULT NULL COMMENT '密钥配置名称',
    ADD COLUMN `url` varchar(255) DEFAULT NULL COMMENT '自定义模型请求地址';
