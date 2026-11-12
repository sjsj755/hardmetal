-- =====================================================================
-- V4__two_roles.sql 角色收敛：6 个角色 → 2 个（ADMIN / SALES）
-- 依据：《设计清单》H 权限矩阵、《内部自用精简-影响与后端清单》§2.2 / §3 P0-1
-- 说明：
--   1) sys_user.role_id 是 NOT NULL 外键，必须先把人改挂到保留角色，再删角色
--   2) 待删角色的用户在无法判断时默认升 ADMIN；确需降为 SALES 的个别人，
--      在本文件里追加一条按 username 的 UPDATE（不要修改已应用的 V2__seed.sql）
--   3) Flyway 在 PostgreSQL 上已在单事务中执行本迁移，这里不再写 BEGIN/COMMIT，
--      否则内层 COMMIT 会提前提交、破坏迁移的原子性
--   4) 执行前建议先人工看一眼分布（Flyway 不会展示 SELECT 结果）：
--        SELECT r.code, count(u.id) FROM sys_role r
--        LEFT JOIN sys_user u ON u.role_id = r.id GROUP BY r.code ORDER BY r.code;
-- =====================================================================

-- 1) 归并：原 BOSS / SALES_MANAGER / DOC_SPECIALIST / FINANCE 默认并入 ADMIN
UPDATE sys_user
SET role_id = (SELECT id FROM sys_role WHERE code = 'ADMIN'),
    updated_at = now()
WHERE role_id IN (
    SELECT id FROM sys_role
    WHERE code IN ('BOSS', 'SALES_MANAGER', 'DOC_SPECIALIST', 'FINANCE')
);

-- 2) 删除多余角色（此时已无用户引用，外键不会报错）
DELETE FROM sys_role
WHERE code IN ('BOSS', 'SALES_MANAGER', 'DOC_SPECIALIST', 'FINANCE');

-- 3) 更新表注释，避免注释继续宣称 6 个角色
COMMENT ON TABLE sys_role IS '角色：SALES 业务员 / ADMIN 管理员（两级）';