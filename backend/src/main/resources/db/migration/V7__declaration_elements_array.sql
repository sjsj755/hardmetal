-- V7: 申报要素统一为 text[]（与 customer.processing_materials / processes / machine_brands 一致）。
-- 原列是竖线拼接的 text（如 "品名|材质|用途"），接口契约要的是数组，这里一次性转换。
ALTER TABLE hs_code
    ALTER COLUMN declaration_elements TYPE text[]
        USING CASE
                  WHEN declaration_elements IS NULL THEN NULL
                  WHEN btrim(declaration_elements) = '' THEN '{}'::text[]
                  ELSE string_to_array(declaration_elements, '|')
              END;

-- 原料价：spec 为空的行按 (material_type, price_date) 唯一。
-- PG 的唯一约束认为 NULL 互不相等，原 UNIQUE (material_type, spec, price_date)
-- 对“不填规格”的录入形同虚设，故补一个部分唯一索引兜底（应用层仍会先给出友好提示）。
CREATE UNIQUE INDEX ux_raw_material_price_nospec
    ON raw_material_price (material_type, price_date)
    WHERE spec IS NULL;