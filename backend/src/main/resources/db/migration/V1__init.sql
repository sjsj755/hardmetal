-- =====================================================================
-- 硬质合金刀具出口系统 · 初始化结构（V1）
-- 约定：金额 numeric(18,4)，税率/费率 numeric(9,6)，时间 timestamptz 存 UTC
--       单据软删除 is_deleted，业务表不允许物理删除
-- =====================================================================

-- ---------- 用户与角色 ----------
CREATE TABLE sys_role (
    id          bigserial PRIMARY KEY,
    code        varchar(32)  NOT NULL UNIQUE,
    name        varchar(64)  NOT NULL,
    remark      varchar(255),
    created_at  timestamptz  NOT NULL DEFAULT now()
);
COMMENT ON TABLE sys_role IS '角色：SALES 业务员 / SALES_MANAGER 业务主管 / DOC 单证 / FINANCE 财务 / BOSS 老板 / ADMIN 管理员';

CREATE TABLE sys_user (
    id                 bigserial PRIMARY KEY,
    username           varchar(64)  NOT NULL UNIQUE,
    password_hash      varchar(100) NOT NULL,
    real_name          varchar(64)  NOT NULL,
    role_id            bigint       NOT NULL REFERENCES sys_role (id),
    department         varchar(64),
    phone              varchar(32),
    email              varchar(128),
    status             smallint     NOT NULL DEFAULT 1, -- 1 启用 / 0 停用
    failed_login_count smallint     NOT NULL DEFAULT 0,
    locked_until       timestamptz,
    last_login_at      timestamptz,
    is_deleted         boolean      NOT NULL DEFAULT false,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now()
);

-- ---------- 主数据：客户 ----------
CREATE TABLE customer (
    id                   bigserial PRIMARY KEY,
    customer_code        varchar(32)  NOT NULL UNIQUE,
    name_cn              varchar(128) NOT NULL,
    name_en              varchar(128),
    short_name           varchar(64),
    country_code         char(2)      NOT NULL,
    region               varchar(64),
    currency_code        char(3)      NOT NULL DEFAULT 'USD',
    language             varchar(16)  NOT NULL DEFAULT 'zh',
    timezone             varchar(64)  NOT NULL DEFAULT 'Asia/Shanghai',
    customer_type        varchar(16)  NOT NULL, -- distributor / end_user / trader / agent
    grade                char(1)      NOT NULL DEFAULT 'C', -- A/B/C/D
    processing_materials text[],      -- steel/stainless/cast_iron/aluminum/superalloy/composite/hardened_steel
    processes            text[],      -- turning/milling/drilling/boring/threading/parting/grooving/mold
    machine_brand        varchar(64),
    machine_model        varchar(64),
    tool_holder_interface varchar(32), -- BT/HSK/Capto...
    benchmark_brand      varchar(64),  -- 山特维克/伊斯卡/肯纳/三菱/其他
    payment_terms        varchar(32),
    credit_days          integer,
    credit_limit         numeric(18,4),
    sinosure_limit       numeric(18,4),
    tax_no               varchar(64),
    sanctions_status     varchar(16) NOT NULL DEFAULT 'clear', -- P0 只存 clear/review
    sensitive_country    boolean     NOT NULL DEFAULT false,
    owner_user_id        bigint REFERENCES sys_user (id),
    region_code          varchar(32),
    source_channel       varchar(64),
    remark               text,
    is_deleted           boolean     NOT NULL DEFAULT false,
    created_by           bigint,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_by           bigint,
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_customer_type CHECK (customer_type IN ('distributor', 'end_user', 'trader', 'agent')),
    CONSTRAINT ck_customer_grade CHECK (grade IN ('A', 'B', 'C', 'D')),
    CONSTRAINT ck_customer_sanctions CHECK (sanctions_status IN ('clear', 'review', 'blocked'))
);
CREATE INDEX idx_customer_owner ON customer (owner_user_id) WHERE is_deleted = false;
CREATE INDEX idx_customer_country ON customer (country_code, grade) WHERE is_deleted = false;

CREATE TABLE customer_contact (
    id                bigserial PRIMARY KEY,
    customer_id       bigint      NOT NULL REFERENCES customer (id),
    name              varchar(64) NOT NULL,
    position          varchar(32), -- purchase/technical/quality/finance/boss
    email             varchar(128),
    phone             varchar(32),
    im_type           varchar(16), -- whatsapp/wechat/skype
    im_account        varchar(64),
    language          varchar(16),
    is_decision_maker boolean     NOT NULL DEFAULT false,
    is_deleted        boolean     NOT NULL DEFAULT false,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_contact_customer ON customer_contact (customer_id) WHERE is_deleted = false;

-- ---------- 主数据：产品 ----------
CREATE TABLE product (
    id                    bigserial PRIMARY KEY,
    product_code          varchar(64)   NOT NULL UNIQUE,
    name_cn               varchar(128)  NOT NULL,
    name_en               varchar(128),
    category              varchar(24)   NOT NULL, -- indexable_insert/solid_tool/brazed_tip/die_mold/blank/custom_part
    -- 刀片矩阵 7 维
    insert_shape          varchar(8),   -- C/D/V/S/T/R/W
    clearance_angle       varchar(8),   -- N/P/C/D/E
    tolerance_class       varchar(8),   -- M/G/K
    groove                varchar(16),  -- MM/MR/FN...
    size_code             varchar(8),   -- 09/12/16/19
    corner_radius         varchar(8),   -- 04/08/12
    hand                  varchar(4),   -- L/R/N
    -- 整体刀具 5 维
    tool_type             varchar(24),  -- end_mill/drill/reamer/tap/form
    diameter_mm           numeric(9,3),
    flute_length_mm       numeric(9,3),
    shank_type            varchar(16),
    flute_count           smallint,
    helix_angle           numeric(6,2),
    -- 材质
    grade_code            varchar(16),
    grade_system          varchar(8),   -- P/M/K/N/S/H
    coating               varchar(16),  -- none/TiN/TiCN/TiAlN/AlCrN/CVD/PVD/diamond
    grain_size_um         numeric(8,3),
    -- 适用
    applicable_materials  text[],
    recommended_vc        numeric(9,2),
    recommended_feed      numeric(9,3),
    max_depth_mm          numeric(9,3),
    -- 物理
    net_weight_g          numeric(18,4),
    gross_weight_g        numeric(18,4),
    volume_cm3            numeric(18,4),
    material_usage_g      numeric(18,4),
    -- 税务与管制
    hs_code               varchar(10),
    declaration_elements  text,
    export_rebate_rate    numeric(9,6),
    supervision_condition varchar(64),
    is_controlled         boolean       NOT NULL DEFAULT false,
    tungsten_content_pct  numeric(9,6), -- 管制筛查参数
    -- 成本
    material_cost         numeric(18,4),
    machining_cost        numeric(18,4),
    coating_cost          numeric(18,4),
    packaging_cost        numeric(18,4),
    -- 状态
    status                varchar(16)   NOT NULL DEFAULT 'active', -- active/discontinued/trial
    launch_date           date,
    moq                   integer,
    lead_time_days        integer,
    attrs                 jsonb         NOT NULL DEFAULT '{}'::jsonb,
    is_deleted            boolean       NOT NULL DEFAULT false,
    created_by            bigint,
    created_at            timestamptz   NOT NULL DEFAULT now(),
    updated_by            bigint,
    updated_at            timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_product_category CHECK (category IN ('indexable_insert', 'solid_tool', 'brazed_tip', 'die_mold', 'blank', 'custom_part')),
    CONSTRAINT ck_product_status CHECK (status IN ('active', 'discontinued', 'trial')),
    CONSTRAINT ck_product_grade_system CHECK (grade_system IS NULL OR grade_system IN ('P', 'M', 'K', 'N', 'S', 'H'))
);
CREATE INDEX idx_product_matrix ON product (insert_shape, clearance_angle, tolerance_class, groove, size_code, corner_radius, hand) WHERE is_deleted = false;
CREATE INDEX idx_product_hs ON product (hs_code) WHERE is_deleted = false;
CREATE INDEX idx_product_attrs ON product USING gin (attrs);

CREATE TABLE product_alias (
    id          bigserial PRIMARY KEY,
    product_id  bigint      NOT NULL REFERENCES product (id),
    alias_type  varchar(16) NOT NULL, -- customer_code/competitor_code/legacy_code/supplier_code
    alias_value varchar(64) NOT NULL,
    brand       varchar(64),
    remark      varchar(255),
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_alias_type CHECK (alias_type IN ('customer_code', 'competitor_code', 'legacy_code', 'supplier_code'))
);
-- 规则：一个别名只能映射到一个我方型号
CREATE UNIQUE INDEX ux_alias_value ON product_alias (alias_type, alias_value, coalesce(brand, ''));
CREATE INDEX idx_alias_value ON product_alias (lower(alias_value));

-- ---------- 主数据：HS / 港口 / 币种 / 价格 ----------
CREATE TABLE hs_code (
    code                 varchar(10) PRIMARY KEY,
    name_cn              varchar(128) NOT NULL,
    name_en              varchar(128),
    unit                 varchar(16)  NOT NULL DEFAULT '千克',
    legal_unit1          varchar(16),
    legal_unit2          varchar(16),
    export_duty_rate     numeric(9,6) NOT NULL DEFAULT 0,
    export_rebate_rate   numeric(9,6) NOT NULL DEFAULT 0,
    supervision_condition varchar(64),
    inspection_category  varchar(64),
    declaration_elements text,
    classification_basis text,
    remark               text,
    is_deleted           boolean      NOT NULL DEFAULT false,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE port (
    code               varchar(10) PRIMARY KEY,
    name_cn            varchar(128) NOT NULL,
    name_en            varchar(128),
    country_code       char(2)      NOT NULL,
    port_type          varchar(16)  NOT NULL, -- seaport/airport/landport
    route              varchar(32),
    surcharge_template jsonb        NOT NULL DEFAULT '{}'::jsonb,
    is_deleted         boolean      NOT NULL DEFAULT false,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_port_type CHECK (port_type IN ('seaport', 'airport', 'landport'))
);

CREATE TABLE currency (
    id            bigserial PRIMARY KEY,
    currency_code char(3)      NOT NULL,
    mid_rate      numeric(18,6) NOT NULL,
    buy_rate      numeric(18,6),
    sell_rate     numeric(18,6),
    rate_date     date         NOT NULL,
    source        varchar(32)  NOT NULL DEFAULT 'BOC',
    is_locked     boolean      NOT NULL DEFAULT false,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ux_currency_date UNIQUE (currency_code, rate_date)
);

CREATE TABLE price_list (
    id              bigserial PRIMARY KEY,
    scope_type      varchar(16)   NOT NULL, -- customer/grade/region/standard
    customer_id     bigint REFERENCES customer (id),
    grade           char(1),
    region_code     varchar(32),
    product_id      bigint        NOT NULL REFERENCES product (id),
    currency_code   char(3)       NOT NULL DEFAULT 'USD',
    min_qty         integer       NOT NULL DEFAULT 1,
    max_qty         integer,
    unit_price      numeric(18,4) NOT NULL,
    effective_from  date          NOT NULL,
    effective_to    date,
    approval_user_id bigint,
    is_deleted      boolean       NOT NULL DEFAULT false,
    created_by      bigint,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_scope_type CHECK (scope_type IN ('customer', 'grade', 'region', 'standard')),
    CONSTRAINT ck_price_qty CHECK (min_qty >= 1 AND (max_qty IS NULL OR max_qty >= min_qty)),
    CONSTRAINT ck_price_period CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX idx_price_lookup ON price_list (product_id, scope_type, effective_from DESC) WHERE is_deleted = false;
CREATE INDEX idx_price_customer ON price_list (customer_id, product_id) WHERE is_deleted = false;

-- ---------- 规则数据（版本化） ----------
CREATE TABLE tariff_rule (
    id                  bigserial PRIMARY KEY,
    hs_code             varchar(10)   NOT NULL,
    country_code        char(2)       NOT NULL,
    rule_type           varchar(12)   NOT NULL, -- MFN/FTA/301/AD/CVD/SG/VAT/GST
    rate_type           varchar(12)   NOT NULL DEFAULT 'ad_valorem', -- ad_valorem/specific
    rate_value          numeric(9,6)  NOT NULL,
    currency_code       char(3),
    effective_from      date          NOT NULL,
    effective_to        date,
    legal_basis         varchar(255),
    fta_name            varchar(64),
    requires_origin_cert boolean      NOT NULL DEFAULT false,
    remark              text,
    is_deleted          boolean       NOT NULL DEFAULT false,
    created_at          timestamptz   NOT NULL DEFAULT now(),
    updated_at          timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_rule_type CHECK (rule_type IN ('MFN', 'FTA', '301', 'AD', 'CVD', 'SG', 'VAT', 'GST')),
    CONSTRAINT ck_rate_type CHECK (rate_type IN ('ad_valorem', 'specific')),
    CONSTRAINT ux_tariff_version UNIQUE (hs_code, country_code, rule_type, effective_from)
);
CREATE INDEX idx_tariff_lookup ON tariff_rule (hs_code, country_code, effective_from DESC) WHERE is_deleted = false;

CREATE TABLE export_control_rule (
    id             bigserial PRIMARY KEY,
    list_version   varchar(32)  NOT NULL,
    item_code      varchar(32)  NOT NULL, -- 1C117.d
    ref_hs_code    varchar(10),
    condition_expr jsonb        NOT NULL DEFAULT '{}'::jsonb, -- 声明式条件：{"tungsten_content_pct":{"gte":97},"form":["powder"]}
    announcement_no varchar(64),
    effective_from date         NOT NULL,
    effective_to   date,
    requires_license boolean    NOT NULL DEFAULT true,
    remark         text,
    is_deleted     boolean      NOT NULL DEFAULT false,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_ctrl_rule ON export_control_rule (ref_hs_code, effective_from DESC) WHERE is_deleted = false;

CREATE TABLE compliance_screen (
    id            bigserial PRIMARY KEY,
    screen_type   varchar(16) NOT NULL, -- product/customer
    target_id     bigint      NOT NULL,
    target_code   varchar(64),
    result        varchar(16) NOT NULL, -- pass/blocked/released
    matched_rules jsonb       NOT NULL DEFAULT '[]'::jsonb,
    release_no    varchar(64),          -- 许可证编号
    handled_action varchar(32),
    handled_by    bigint,
    screened_at   timestamptz NOT NULL DEFAULT now(),
    remark        text,
    CONSTRAINT ck_screen_type CHECK (screen_type IN ('product', 'customer')),
    CONSTRAINT ck_screen_result CHECK (result IN ('pass', 'blocked', 'released'))
);
CREATE INDEX idx_screen_target ON compliance_screen (screen_type, target_id, screened_at DESC);

-- ---------- 运价与原料价 ----------
CREATE TABLE freight_rate (
    id              bigserial PRIMARY KEY,
    transport_mode  varchar(12)   NOT NULL, -- express/air/lcl/fcl/rail/road
    carrier         varchar(64)   NOT NULL,
    origin_code     varchar(10)   NOT NULL,
    dest_code       varchar(10)   NOT NULL,
    charge_method   varchar(24)   NOT NULL, -- first_next/weight/cbm/container
    base_price      numeric(18,4) NOT NULL,
    currency_code   char(3)       NOT NULL DEFAULT 'CNY',
    surcharges      jsonb         NOT NULL DEFAULT '[]'::jsonb,
    min_charge      numeric(18,4),
    transit_days    integer,
    effective_from  date          NOT NULL,
    effective_to    date,
    source_level    varchar(4)    NOT NULL DEFAULT 'L2', -- L1历史/L2协议/L3指数/L4API
    remark          text,
    is_deleted      boolean       NOT NULL DEFAULT false,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_transport_mode CHECK (transport_mode IN ('express', 'air', 'lcl', 'fcl', 'rail', 'road')),
    CONSTRAINT ck_charge_method CHECK (charge_method IN ('first_next', 'weight', 'cbm', 'container')),
    CONSTRAINT ck_source_level CHECK (source_level IN ('L1', 'L2', 'L3', 'L4'))
);
CREATE INDEX idx_freight_lookup ON freight_rate (origin_code, dest_code, transport_mode, effective_from DESC) WHERE is_deleted = false;

CREATE TABLE raw_material_price (
    id            bigserial PRIMARY KEY,
    material_type varchar(32)   NOT NULL, -- tungsten_powder/cobalt_powder/wc_powder/titanium/tantalum
    spec          varchar(64),
    purity        varchar(32),
    price         numeric(18,4) NOT NULL,
    currency_code char(3)       NOT NULL DEFAULT 'CNY',
    price_date    date          NOT NULL,
    source        varchar(64)   NOT NULL,
    change_pct    numeric(9,6),
    created_at    timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ux_material_price UNIQUE (material_type, spec, price_date)
);

-- ---------- 报价 ----------
CREATE TABLE quotation (
    id                    bigserial PRIMARY KEY,
    quote_no              varchar(32)   NOT NULL UNIQUE,
    customer_id           bigint        NOT NULL REFERENCES customer (id),
    contact_id            bigint REFERENCES customer_contact (id),
    currency_code         char(3)       NOT NULL,
    exchange_rate         numeric(18,6) NOT NULL,
    incoterm              varchar(8)    NOT NULL,
    dest_port_code        varchar(10),
    transport_mode        varchar(12),
    -- 五项不可变快照
    exchange_rate_snapshot jsonb        NOT NULL,
    material_price_snapshot jsonb       NOT NULL,
    freight_snapshot      jsonb         NOT NULL DEFAULT '{}'::jsonb,
    tariff_snapshot       jsonb         NOT NULL DEFAULT '{}'::jsonb,
    rebate_snapshot       jsonb         NOT NULL DEFAULT '{}'::jsonb,
    valid_from            timestamptz   NOT NULL,
    valid_to              timestamptz   NOT NULL,
    payment_terms         varchar(64),
    lead_time_days        integer,
    total_amount          numeric(18,4) NOT NULL DEFAULT 0,
    total_cost            numeric(18,4) NOT NULL DEFAULT 0,
    gross_profit          numeric(18,4) NOT NULL DEFAULT 0,
    gross_margin          numeric(9,6)  NOT NULL DEFAULT 0,
    status                varchar(24)   NOT NULL DEFAULT 'draft',
    version_no            integer       NOT NULL DEFAULT 1,
    parent_id             bigint REFERENCES quotation (id),
    approval_user_id      bigint,
    reject_reason         text,
    sent_at               timestamptz,
    send_channel          varchar(32),
    remark                text,
    is_deleted            boolean       NOT NULL DEFAULT false,
    created_by            bigint,
    created_at            timestamptz   NOT NULL DEFAULT now(),
    updated_by            bigint,
    updated_at            timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_quote_status CHECK (status IN ('draft', 'internal_review', 'sent', 'accepted', 'rejected', 'expired', 'converted')),
    CONSTRAINT ck_quote_incoterm CHECK (incoterm IN ('EXW', 'FOB', 'CFR', 'CIF', 'CPT', 'CIP', 'DAP', 'DDP')),
    CONSTRAINT ck_quote_period CHECK (valid_to > valid_from)
);
CREATE INDEX idx_quote_customer ON quotation (customer_id, created_at DESC) WHERE is_deleted = false;
CREATE INDEX idx_quote_status ON quotation (status, valid_to) WHERE is_deleted = false;

CREATE TABLE quote_cost_item (
    id              bigserial PRIMARY KEY,
    quotation_id    bigint        NOT NULL REFERENCES quotation (id),
    line_no         integer       NOT NULL,
    product_id      bigint REFERENCES product (id),
    custom_part_id  bigint,
    qty             numeric(18,4) NOT NULL,
    unit_price      numeric(18,4) NOT NULL,
    amount          numeric(18,4) NOT NULL DEFAULT 0,
    material_cost   numeric(18,4) NOT NULL DEFAULT 0,
    machining_cost  numeric(18,4) NOT NULL DEFAULT 0,
    coating_cost    numeric(18,4) NOT NULL DEFAULT 0,
    packaging_cost  numeric(18,4) NOT NULL DEFAULT 0,
    inland_freight  numeric(18,4) NOT NULL DEFAULT 0,
    customs_fee     numeric(18,4) NOT NULL DEFAULT 0,
    intl_freight    numeric(18,4) NOT NULL DEFAULT 0,
    insurance       numeric(18,4) NOT NULL DEFAULT 0,
    bank_fee        numeric(18,4) NOT NULL DEFAULT 0,
    commission      numeric(18,4) NOT NULL DEFAULT 0,
    rebate_amount   numeric(18,4) NOT NULL DEFAULT 0,
    gross_profit    numeric(18,4) NOT NULL DEFAULT 0,
    gross_margin    numeric(9,6)  NOT NULL DEFAULT 0,
    CONSTRAINT ux_quote_line UNIQUE (quotation_id, line_no)
);

-- ---------- 订单 ----------
CREATE TABLE sales_order (
    id                bigserial PRIMARY KEY,
    order_no          varchar(32)   NOT NULL UNIQUE,
    quotation_id      bigint REFERENCES quotation (id),
    customer_id       bigint        NOT NULL REFERENCES customer (id),
    pi_no             varchar(64),
    contract_no       varchar(64),
    sign_date         date,
    amount            numeric(18,4) NOT NULL DEFAULT 0,
    currency_code     char(3)       NOT NULL,
    incoterm          varchar(8),
    deposit_ratio     numeric(9,6),
    balance_terms     varchar(128),
    delivery_date     date,
    status            varchar(24)   NOT NULL DEFAULT 'created',
    production_status varchar(24),
    shipment_status   varchar(24),
    payment_status    varchar(24),
    waybill_no        varchar(64),
    bl_no             varchar(64),
    completed_at      timestamptz,
    remark            text,
    is_deleted        boolean       NOT NULL DEFAULT false,
    created_by        bigint,
    created_at        timestamptz   NOT NULL DEFAULT now(),
    updated_at        timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_order_status CHECK (status IN ('created', 'confirmed', 'in_production', 'ready', 'shipped', 'delivered', 'settled', 'closed', 'cancelled'))
);
CREATE INDEX idx_order_customer ON sales_order (customer_id, created_at DESC) WHERE is_deleted = false;

-- ---------- 辅助 ----------
CREATE TABLE approval_log (
    id          bigserial PRIMARY KEY,
    biz_type    varchar(32)  NOT NULL,
    biz_id      bigint       NOT NULL,
    action      varchar(24)  NOT NULL, -- submit/approve/reject/release
    from_status varchar(24),
    to_status   varchar(24),
    reason      text,
    operator_id bigint,
    operator_name varchar(64),
    created_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_approval_biz ON approval_log (biz_type, biz_id);

CREATE TABLE audit_log (
    id           bigserial PRIMARY KEY,
    trace_id     varchar(64),
    user_id      bigint,
    username     varchar(64),
    module       varchar(32),
    action       varchar(16), -- create/update/delete/...
    target_type  varchar(64),
    target_id    varchar(64),
    before_value jsonb,
    after_value  jsonb,
    ip           varchar(64),
    created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_target ON audit_log (target_type, target_id, created_at DESC);
CREATE INDEX idx_audit_user ON audit_log (user_id, created_at DESC);

CREATE TABLE attachment (
    id           bigserial PRIMARY KEY,
    biz_type     varchar(32)  NOT NULL,
    biz_id       bigint       NOT NULL,
    file_name    varchar(255) NOT NULL,
    file_path    varchar(512) NOT NULL,
    file_size    bigint,
    content_type varchar(128),
    uploaded_by  bigint,
    created_at   timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachment_biz ON attachment (biz_type, biz_id);

CREATE TABLE sys_dict (
    id         bigserial PRIMARY KEY,
    dict_type  varchar(32)  NOT NULL,
    dict_key   varchar(64)  NOT NULL,
    dict_label varchar(128) NOT NULL,
    sort_no    integer      NOT NULL DEFAULT 0,
    remark     varchar(255),
    is_deleted boolean      NOT NULL DEFAULT false,
    CONSTRAINT ux_dict_key UNIQUE (dict_type, dict_key)
);

CREATE TABLE import_job (
    id           bigserial PRIMARY KEY,
    job_type     varchar(32)  NOT NULL, -- product/price
    file_name    varchar(255),
    status       varchar(16)  NOT NULL DEFAULT 'pending', -- pending/running/success/failed
    total_rows   integer      NOT NULL DEFAULT 0,
    success_rows integer      NOT NULL DEFAULT 0,
    failed_rows  integer      NOT NULL DEFAULT 0,
    errors       jsonb        NOT NULL DEFAULT '[]'::jsonb,
    created_by   bigint,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    finished_at  timestamptz,
    CONSTRAINT ck_job_status CHECK (status IN ('pending', 'running', 'success', 'failed'))
);

-- ---------- 报价快照不可变触发器（R-Q07 / R-T05） ----------
CREATE OR REPLACE FUNCTION trg_quote_snapshot_immutable_fn() RETURNS trigger AS
$$
BEGIN
    IF NEW.exchange_rate_snapshot IS DISTINCT FROM OLD.exchange_rate_snapshot
        OR NEW.material_price_snapshot IS DISTINCT FROM OLD.material_price_snapshot
        OR NEW.freight_snapshot IS DISTINCT FROM OLD.freight_snapshot
        OR NEW.tariff_snapshot IS DISTINCT FROM OLD.tariff_snapshot
        OR NEW.rebate_snapshot IS DISTINCT FROM OLD.rebate_snapshot THEN
        RAISE EXCEPTION 'quote snapshot is immutable (quotation.id=%)', OLD.id
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_quote_snapshot_immutable
    BEFORE UPDATE ON quotation
    FOR EACH ROW EXECUTE FUNCTION trg_quote_snapshot_immutable_fn();