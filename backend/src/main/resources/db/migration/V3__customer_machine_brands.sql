-- 客户机床品牌改为多值，对齐《接口文档》3.3 的 machineBrands 数组。
ALTER TABLE customer ADD COLUMN machine_brands text[];
COMMENT ON COLUMN customer.machine_brands IS '客户主力机床品牌列表，如 DMG Mori/Hermle';