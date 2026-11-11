-- =====================================================================
-- V2__seed.sql 基础数据种子
-- 说明：
--   1) 密码哈希为 BCrypt(cost=10)，默认账号 admin / Admin@2026（仅开发环境）
--   2) HS 编码与税率属“骨架数据”，上线前须由单证员对照现行税则逐条复核
--   3) 汇率为演示值，真实值由定时任务抓取中国银行牌价后覆盖
-- =====================================================================

-- 1) 角色
INSERT INTO sys_role (code, name, remark) VALUES
    ('ADMIN',         '系统管理员', '全部权限，含用户与基础数据维护'),
    ('BOSS',          '总经理',     '全部数据可读，审批报价与订单'),
    ('SALES_MANAGER', '销售主管',   '团队报价审批、客户与价格审核'),
    ('SALES',         '销售',       '客户、报价、订单；价格表只读'),
    ('DOC_SPECIALIST','单证员',     '订单、单证、报关资料、附件'),
    ('FINANCE',       '财务',       '核价、收款、退税、审批财务节点');

-- 2) 管理员账号（admin / Admin@2026）
INSERT INTO sys_user (username, password_hash, real_name, role_id, department, phone, email, status)
SELECT 'admin',
       '$2a$10$grGyjfNXPYICV1s.6yMvW.yE2SOs0.kCYxzD2/vxELF8tcyR2kwES',
       '系统管理员',
       r.id,
       '信息部',
       '13800000000',
       'admin@hardmetal.local',
       1
FROM sys_role r WHERE r.code = 'ADMIN';

-- 3) 字典（13 组）
INSERT INTO sys_dict (dict_type, dict_key, dict_label, sort_no) VALUES
    ('customer_type',    'distributor', '经销商',    10),
    ('customer_type',    'end_user',    '终端用户',  20),
    ('customer_type',    'trader',      '贸易商',    30),
    ('customer_type',    'agent',       '代理商',    40),

    ('customer_grade',   'A', 'A 级（战略）', 10),
    ('customer_grade',   'B', 'B 级（重点）', 20),
    ('customer_grade',   'C', 'C 级（常规）', 30),
    ('customer_grade',   'D', 'D 级（观察）', 40),

    ('customer_source',  'exhibition',  '展会',      10),
    ('customer_source',  'website',     '官网询盘',  20),
    ('customer_source',  'referral',    '老客推荐',  30),
    ('customer_source',  'b2b',         'B2B 平台',  40),
    ('customer_source',  'cold_call',   '主动开发',  50),

    ('incoterm',         'EXW', 'EXW 工厂交货',       10),
    ('incoterm',         'FOB', 'FOB 装运港船上交货', 20),
    ('incoterm',         'CFR', 'CFR 成本加运费',     30),
    ('incoterm',         'CIF', 'CIF 成本保险费加运费', 40),
    ('incoterm',         'CPT', 'CPT 运费付至',       50),
    ('incoterm',         'CIP', 'CIP 运费保险费付至', 60),
    ('incoterm',         'DAP', 'DAP 目的地交货',     70),
    ('incoterm',         'DDP', 'DDP 完税后交货',     80),

    ('transport_mode',   'express', '国际快递',  10),
    ('transport_mode',   'air',     '空运',      20),
    ('transport_mode',   'lcl',     '海运拼箱',  30),
    ('transport_mode',   'fcl',     '海运整柜',  40),
    ('transport_mode',   'rail',    '中欧班列',  50),
    ('transport_mode',   'road',    '陆运',      60),

    ('payment_terms',    'T/T',        '电汇',                10),
    ('payment_terms',    'T/T_30',     '电汇 30 天',          20),
    ('payment_terms',    'T/T_60',     '电汇 60 天',          30),
    ('payment_terms',    'L/C',        '信用证',              40),
    ('payment_terms',    'L/C_90',     '信用证 90 天',        50),
    ('payment_terms',    'D/P',        '付款交单',            60),
    ('payment_terms',    'D/A',        '承兑交单',            70),
    ('payment_terms',    'O/A',        '赊销',                80),

    ('product_category', 'indexable_insert', '可转位刀片', 10),
    ('product_category', 'solid_tool',       '整体硬质合金刀具', 20),
    ('product_category', 'brazed_tip',       '焊接刀头',   30),
    ('product_category', 'die_mold',         '模具',       40),
    ('product_category', 'blank',            '毛坯',       50),
    ('product_category', 'custom_part',      '定制件',     60),

    ('grade_system',     'P', 'P 钢件',       10),
    ('grade_system',     'M', 'M 不锈钢',     20),
    ('grade_system',     'K', 'K 铸铁',       30),
    ('grade_system',     'N', 'N 有色金属',   40),
    ('grade_system',     'S', 'S 高温合金',   50),
    ('grade_system',     'H', 'H 淬硬钢',     60),

    ('coating',          'none',    '无涂层',   10),
    ('coating',          'TiN',     'TiN',      20),
    ('coating',          'TiCN',    'TiCN',     30),
    ('coating',          'TiAlN',   'TiAlN',    40),
    ('coating',          'AlCrN',   'AlCrN',    50),
    ('coating',          'CVD',     'CVD',      60),
    ('coating',          'PVD',     'PVD',      70),
    ('coating',          'diamond', '金刚石',   80),

    ('insert_shape',     'C', 'C 形 80°',   10),
    ('insert_shape',     'D', 'D 形 55°',   20),
    ('insert_shape',     'V', 'V 形 35°',   30),
    ('insert_shape',     'S', 'S 形 45°',   40),
    ('insert_shape',     'T', 'T 形 60°',   50),
    ('insert_shape',     'R', 'R 形 圆',    60),
    ('insert_shape',     'W', 'W 形 80°',   70),

    ('quote_status',     'draft',           '草稿',      10),
    ('quote_status',     'internal_review', '内部审核',  20),
    ('quote_status',     'sent',            '已发出',    30),
    ('quote_status',     'accepted',        '已接受',    40),
    ('quote_status',     'rejected',        '已拒绝',    50),
    ('quote_status',     'expired',         '已过期',    60),
    ('quote_status',     'converted',       '已转订单',  70),

    ('order_status',     'created',       '已创建',   10),
    ('order_status',     'confirmed',     '已确认',   20),
    ('order_status',     'in_production', '生产中',   30),
    ('order_status',     'ready',         '待发运',   40),
    ('order_status',     'shipped',       '已发运',   50),
    ('order_status',     'delivered',     '已送达',   60),
    ('order_status',     'settled',       '已结汇',   70),
    ('order_status',     'closed',        '已关闭',   80),
    ('order_status',     'cancelled',     '已取消',   90),

    ('currency',         'USD', '美元',   10),
    ('currency',         'EUR', '欧元',   20),
    ('currency',         'CNY', '人民币', 30),
    ('currency',         'RUB', '卢布',   40),
    ('currency',         'INR', '卢比',   50),
    ('currency',         'GBP', '英镑',   60),
    ('currency',         'JPY', '日元',   70);

-- 4) HS 编码骨架（8207 铣削/钻孔/车削刀具，8208 机用刀片，8209 刀片毛坯，8113 金属陶瓷）
--    退税率按 13% 常见档位预置，须以税则书为准
INSERT INTO hs_code (code, name_cn, name_en, unit, export_duty_rate, export_rebate_rate, supervision_condition, inspection_category, declaration_elements) VALUES
    ('8207131000', '硬质合金钻头',           'Tungsten carbide drill bits',       '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207139000', '其他带硬质合金工作部件钻探工具', 'Other drilling tools with cermet working part', '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207191000', '其他硬质合金钻探工具',   'Other drilling tools with cermet working part', '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207199000', '其他钻探工具',           'Other drilling tools',              '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207201000', '硬质合金拉模',           'Tungsten carbide drawing dies',     '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207209000', '其他金属拉拔模具',       'Other metal drawing dies',          '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207301000', '硬质合金冲压模具',       'Tungsten carbide stamping dies',    '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207309000', '其他冲压模具',           'Other stamping dies',               '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207401000', '硬质合金挤压工具',       'Tungsten carbide extrusion tools',  '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207409000', '其他挤压工具',           'Other extrusion tools',             '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207501000', '硬质合金铣削工具',       'Tungsten carbide milling tools',    '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207509000', '其他铣削工具',           'Other milling tools',               '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207601000', '硬质合金镗孔工具',       'Tungsten carbide boring tools',     '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207609000', '其他镗孔工具',           'Other boring tools',                '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207701000', '硬质合金螺纹刀具',       'Tungsten carbide threading tools',  '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207709000', '其他螺纹刀具',           'Other threading tools',             '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207801000', '硬质合金车削工具',       'Tungsten carbide turning tools',    '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207809000', '其他车削工具',           'Other turning tools',               '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207901000', '其他可互换硬质合金工具', 'Other interchangeable tools with cermet working part', '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8207909000', '其他可互换工具',         'Other interchangeable tools',       '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8208101100', '硬质合金车削用机用刀片', 'Tungsten carbide machine knives for turning', '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8208101900', '其他硬质合金机用刀片',   'Other machine knives with cermet working part', '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8208109000', '其他机用刀片',           'Other machine knives',              '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8208200000', '木工机械用刀及刀片',     'Knives and blades for wood working', '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8209001000', '硬质合金板、条、棒、尖', 'Cermet plates, sticks, tips',        '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8209002100', '硬质合金刀尖（未装配）', 'Unmounted cermet tips',             '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8209002900', '其他未装配刀尖',         'Other unmounted tips',              '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8209003000', '硬质合金分段刀片',       'Cermet segments',                   '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8209009000', '其他未装配硬质合金工具', 'Other unmounted cermet tools',      '千克', 0, 0.130000, NULL, NULL, '品名|材质|用途|品牌|型号|GTIN'),
    ('8113001000', '碳化钨为主金属陶瓷',     'Cermets based on tungsten carbide', '千克', 0, 0.130000, '两用物项和技术出口许可证', NULL, '品名|成分含量|形态|来源|品牌|型号'),
    ('8113009000', '其他金属陶瓷制品',       'Other cermet articles',             '千克', 0, 0.130000, '两用物项和技术出口许可证', NULL, '品名|成分含量|形态|来源|品牌|型号');

-- 5) 港口（9 起运 + 21 目的）
INSERT INTO port (code, name_cn, name_en, country_code, port_type, route, surcharge_template) VALUES
    ('CNSHA', '上海港',   'Shanghai',      'CN', 'seaport', 'main',   '{"thc":true,"doc":true}'),
    ('CNNGB', '宁波港',   'Ningbo',        'CN', 'seaport', 'main',   '{"thc":true,"doc":true}'),
    ('CNSZX', '深圳港',   'Shenzhen',      'CN', 'seaport', 'main',   '{"thc":true,"doc":true}'),
    ('CNCAN', '广州港',   'Guangzhou',     'CN', 'seaport', 'main',   '{"thc":true,"doc":true}'),
    ('CNTSN', '天津港',   'Tianjin',       'CN', 'seaport', 'north',  '{"thc":true,"doc":true}'),
    ('CNQIN', '青岛港',   'Qingdao',       'CN', 'seaport', 'north',  '{"thc":true,"doc":true}'),
    ('CNXMN', '厦门港',   'Xiamen',        'CN', 'seaport', 'south',  '{"thc":true,"doc":true}'),
    ('CNDLC', '大连港',   'Dalian',        'CN', 'seaport', 'north',  '{"thc":true,"doc":true}'),
    ('CNLYG', '连云港',   'Lianyungang',   'CN', 'seaport', 'north',  '{"thc":true,"doc":true}'),
    ('DEHAM', '汉堡港',   'Hamburg',       'DE', 'seaport', 'europe', '{"thc":true,"isps":true}'),
    ('DEBRV', '不来梅哈芬','Bremerhaven',  'DE', 'seaport', 'europe', '{"thc":true,"isps":true}'),
    ('NLRTM', '鹿特丹港', 'Rotterdam',     'NL', 'seaport', 'europe', '{"thc":true,"isps":true}'),
    ('BEANR', '安特卫普港','Antwerp',      'BE', 'seaport', 'europe', '{"thc":true,"isps":true}'),
    ('FRLEH', '勒阿弗尔港','Le Havre',     'FR', 'seaport', 'europe', '{"thc":true,"isps":true}'),
    ('ITGOA', '热那亚港', 'Genoa',         'IT', 'seaport', 'europe', '{"thc":true,"isps":true}'),
    ('ESVLC', '瓦伦西亚港','Valencia',     'ES', 'seaport', 'europe', '{"thc":true,"isps":true}'),
    ('RULED', '圣彼得堡港','St. Petersburg','RU','seaport', 'europe', '{"thc":true,"isps":true}'),
    ('RUNVS', '新罗西斯克港','Novorossiysk','RU','seaport','black_sea','{"thc":true,"isps":true}'),
    ('USLAX', '洛杉矶港', 'Los Angeles',   'US', 'seaport', 'namerica','{"thc":true,"isps":true}'),
    ('USNYC', '纽约港',   'New York',      'US', 'seaport', 'namerica','{"thc":true,"isps":true}'),
    ('USHOU', '休斯顿港', 'Houston',       'US', 'seaport', 'namerica','{"thc":true,"isps":true}'),
    ('GBLON', '伦敦港',   'London',        'GB', 'seaport', 'europe',  '{"thc":true,"isps":true}'),
    ('SGSIN', '新加坡港', 'Singapore',     'SG', 'seaport', 'asia',    '{"thc":true}'),
    ('KRPUS', '釜山港',   'Busan',         'KR', 'seaport', 'asia',    '{"thc":true}'),
    ('INNSA', '那瓦舍瓦港','Nhava Sheva',  'IN', 'seaport', 'sasia',   '{"thc":true}'),
    ('INMAA', '金奈港',   'Chennai',       'IN', 'seaport', 'sasia',   '{"thc":true}'),
    ('JPYOK', '横滨港',   'Yokohama',      'JP', 'seaport', 'asia',    '{"thc":true}'),
    ('AEDXB', '迪拜杰贝阿里港','Jebel Ali','AE','seaport', 'mideast', '{"thc":true}'),
    ('TRMER', '梅尔辛港', 'Mersin',        'TR', 'seaport', 'mideast', '{"thc":true}'),
    ('BRSSZ', '桑托斯港', 'Santos',        'BR', 'seaport', 'latam',   '{"thc":true,"isps":true}');

-- 6) 汇率（演示值，CNY 为基准 1）
INSERT INTO currency (currency_code, mid_rate, buy_rate, sell_rate, rate_date, source, is_locked) VALUES
    ('USD', 7.105000, 7.090000, 7.120000, DATE '2026-11-10', 'BOC', false),
    ('EUR', 7.985000, 7.965000, 8.005000, DATE '2026-11-10', 'BOC', false),
    ('CNY', 1.000000, 1.000000, 1.000000, DATE '2026-11-10', 'BOC', true),
    ('RUB', 0.088000, 0.087600, 0.088400, DATE '2026-11-10', 'BOC', false),
    ('INR', 0.084200, 0.083900, 0.084500, DATE '2026-11-10', 'BOC', false),
    ('GBP', 9.350000, 9.325000, 9.375000, DATE '2026-11-10', 'BOC', false),
    ('JPY', 0.047300, 0.047170, 0.047430, DATE '2026-11-10', 'BOC', false);

-- 7) 关税/税费规则：DE / US / RU / IN 四个主销地
INSERT INTO tariff_rule (hs_code, country_code, rule_type, rate_type, rate_value, effective_from, legal_basis, fta_name, requires_origin_cert, remark) VALUES
    -- 德国：MFN 进口关税 + 19% VAT
    ('8207501000', 'DE', 'MFN', 'ad_valorem', 0.037000, DATE '2026-01-01', 'EU TARIC 8207 50 10', NULL, false, '铣削工具用硬质合金工作部件'),
    ('8207501000', 'DE', 'VAT', 'ad_valorem', 0.190000, DATE '2026-01-01', 'DE UStG §12', NULL, false, '进口环节增值税，报价按 DDP 需含'),
    ('8209001000', 'DE', 'MFN', 'ad_valorem', 0.025000, DATE '2026-01-01', 'EU TARIC 8209 00 10', NULL, false, NULL),
    ('8209001000', 'DE', 'VAT', 'ad_valorem', 0.190000, DATE '2026-01-01', 'DE UStG §12', NULL, false, NULL),
    ('8113001000', 'DE', 'MFN', 'ad_valorem', 0.050000, DATE '2026-01-01', 'EU TARIC 8113 00 10', NULL, false, '受管制品，另需两用物项许可'),
    ('8113001000', 'DE', 'VAT', 'ad_valorem', 0.190000, DATE '2026-01-01', 'DE UStG §12', NULL, false, NULL),

    -- 美国：MFN + 301 附加税（票价按 301 清单 3 预置，须按最新 USTR 核对）
    ('8207501000', 'US', 'MFN', 'ad_valorem', 0.038000, DATE '2026-01-01', 'HTSUS 8207.50.10', NULL, false, NULL),
    ('8207501000', 'US', '301', 'ad_valorem', 0.250000, DATE '2026-01-01', 'USTR Section 301 List 3', NULL, false, '对华附加税，按到岸价计征'),
    ('8209001000', 'US', 'MFN', 'ad_valorem', 0.023000, DATE '2026-01-01', 'HTSUS 8209.00.10', NULL, false, NULL),
    ('8209001000', 'US', '301', 'ad_valorem', 0.250000, DATE '2026-01-01', 'USTR Section 301 List 3', NULL, false, NULL),
    ('8113001000', 'US', 'MFN', 'ad_valorem', 0.044000, DATE '2026-01-01', 'HTSUS 8113.00.10', NULL, false, NULL),

    -- 俄罗斯：MFN + 20% VAT
    ('8207501000', 'RU', 'MFN', 'ad_valorem', 0.050000, DATE '2026-01-01', 'EAEU ETT 8207 50 100 0', NULL, false, NULL),
    ('8207501000', 'RU', 'VAT', 'ad_valorem', 0.200000, DATE '2026-01-01', 'RU Tax Code Art.164', NULL, false, NULL),
    ('8209001000', 'RU', 'MFN', 'ad_valorem', 0.050000, DATE '2026-01-01', 'EAEU ETT 8209 00 100 0', NULL, false, NULL),
    ('8209001000', 'RU', 'VAT', 'ad_valorem', 0.200000, DATE '2026-01-01', 'RU Tax Code Art.164', NULL, false, NULL),

    -- 印度：MFN 7.5% + 18% GST
    ('8207501000', 'IN', 'MFN', 'ad_valorem', 0.075000, DATE '2026-01-01', 'India Customs Tariff Ch.82', NULL, false, NULL),
    ('8207501000', 'IN', 'GST', 'ad_valorem', 0.180000, DATE '2026-01-01', 'CGST Act 2017', NULL, false, NULL),
    ('8209001000', 'IN', 'MFN', 'ad_valorem', 0.075000, DATE '2026-01-01', 'India Customs Tariff Ch.82', NULL, false, NULL),
    ('8209001000', 'IN', 'GST', 'ad_valorem', 0.180000, DATE '2026-01-01', 'CGST Act 2017', NULL, false, NULL);

-- 8) 出口管制规则：钨含量 ≥97% 的钨粉/碳化钨粉需两用物项出口许可证
INSERT INTO export_control_rule (list_version, item_code, ref_hs_code, condition_expr, announcement_no, effective_from, requires_license, remark) VALUES
    ('CN-DUAL-2026', '1C117.d', '8113001000',
     '{"tungsten_content_pct":{"gte":97},"form":["powder","carbide_powder"]}'::jsonb,
     '商务部 海关总署公告 2025 年第 46 号', DATE '2026-01-01', true,
     '钨含量≥97% 的钨粉及碳化钨粉，出口须申请两用物项和技术出口许可证'),
    ('CN-DUAL-2026', '1C226', '8113001000',
     '{"tungsten_content_pct":{"gte":90},"weight_kg":{"gte":1000}}'::jsonb,
     '商务部 海关总署公告 2025 年第 46 号', DATE '2026-01-01', true,
     '整批钨制品净重≥1000kg 且钨含量≥90%，触发许可审查'),
    ('CN-DUAL-2026', '3A002', '8209001000',
     '{"destination_sensitive":true,"weight_kg":{"gte":500}}'::jsonb,
     '商务部 海关总署公告 2025 年第 46 号', DATE '2026-01-01', false,
     '敏感目的地 + 大批量时仅提示复核，不强制许可');

-- 9) 原料基准价（单位 CNY/吨，演示值）
INSERT INTO raw_material_price (material_type, spec, purity, price, currency_code, price_date, source, change_pct) VALUES
    ('tungsten_powder', 'FSSS 2.0-3.0um', 'min 99.9%', 320000.0000, 'CNY', DATE '2026-11-07', '赣州钨业协会',  0.012500),
    ('wc_powder',       'FSSS 1.5-2.5um', 'min 99.9%', 312000.0000, 'CNY', DATE '2026-11-07', '赣州钨业协会',  0.008000),
    ('cobalt_powder',   'FSSS 1.0-2.0um', 'min 99.8%', 285000.0000, 'CNY', DATE '2026-11-07', '上海有色网',   -0.005000),
    ('titanium',        'Ti sponge',      'min 99.7%', 62000.0000,  'CNY', DATE '2026-11-07', '上海有色网',    0.003000),
    ('tantalum',        'Ta powder',      'min 99.9%', 1450000.0000,'CNY', DATE '2026-11-07', '上海有色网',    0.021000);