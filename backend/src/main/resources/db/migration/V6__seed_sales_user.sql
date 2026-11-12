-- =====================================================================
-- V6__seed_sales_user.sql 补 UAT 业务员账号：sales1 / Admin@2026
-- 说明：
--   1) 复用 admin 的 BCrypt(cost=10) 哈希，便于 UAT；生产环境务必改密或删除
--   2) 本地开发库可能已手工插入过 sales1，用 NOT EXISTS 保证不因唯一约束失败
--   3) 角色只取 SALES（V4 之后系统只保留 ADMIN / SALES）
-- =====================================================================

INSERT INTO sys_user (username, password_hash, real_name, role_id, department, phone, email, status)
SELECT 'sales1',
       '$2a$10$grGyjfNXPYICV1s.6yMvW.yE2SOs0.kCYxzD2/vxELF8tcyR2kwES',
       '销售一号',
       r.id,
       '销售部',
       NULL,
       'sales1@hardmetal.local',
       1
FROM sys_role r
WHERE r.code = 'SALES'
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = 'sales1');