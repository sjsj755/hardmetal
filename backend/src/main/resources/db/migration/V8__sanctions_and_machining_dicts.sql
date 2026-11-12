-- 合规筛查状态不再默认 clear：P0 尚无筛查实现，统一落 unreviewed（前端显示“未筛查”），
-- 避免“没做筛查却显示正常”的合规误导；真正的 clear 只能由将来的筛查流程写入。
ALTER TABLE customer DROP CONSTRAINT ck_customer_sanctions;
ALTER TABLE customer ALTER COLUMN sanctions_status SET DEFAULT 'unreviewed';
ALTER TABLE customer ADD CONSTRAINT ck_customer_sanctions
    CHECK (sanctions_status IN ('unreviewed', 'clear', 'review', 'blocked'));
UPDATE customer SET sanctions_status = 'unreviewed' WHERE sanctions_status = 'clear';

-- 加工材料 / 工序纳入字典（取值来自《接口文档》3.2 与 V1 DDL 注释的并集），
-- 由 DictService.assertAllowed 在写入时校验，前端标签也从这里取。
INSERT INTO sys_dict (dict_type, dict_key, dict_label, sort_no) VALUES
    ('machining_material', 'steel',          '碳钢/合金钢', 10),
    ('machining_material', 'stainless',      '不锈钢',     20),
    ('machining_material', 'cast_iron',      '铸铁',       30),
    ('machining_material', 'aluminum',       '铝合金',     40),
    ('machining_material', 'superalloy',     '高温合金',   50),
    ('machining_material', 'composite',      '复合材料',   60),
    ('machining_material', 'hardened_steel', '淬硬钢',     70),
    ('machining_material', 'titanium',       '钛合金',     80),

    ('machining_op', 'turning',    '车削', 10),
    ('machining_op', 'milling',    '铣削', 20),
    ('machining_op', 'drilling',   '钻孔', 30),
    ('machining_op', 'boring',     '镗削', 40),
    ('machining_op', 'threading',  '螺纹', 50),
    ('machining_op', 'parting',    '切断', 60),
    ('machining_op', 'grooving',   '切槽', 70),
    ('machining_op', 'mold',       '模具加工', 80)
ON CONFLICT (dict_type, dict_key) DO NOTHING;