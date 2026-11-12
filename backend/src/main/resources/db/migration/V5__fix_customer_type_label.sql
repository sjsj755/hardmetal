-- =====================================================================
-- V5__fix_customer_type_label.sql
-- 统一 end_user 的中文标签为“终端工厂”（《设计清单》C 段、《需求与设计》4 取值表）。
-- 原 V2__seed.sql 写成“终端用户”，该文件已执行且不能修改（Flyway checksum）。
-- 注意：DictIntegrityChecker 只比对 dict_key 与 DDL CHECK，改 label 不影响启动自检。
-- =====================================================================

UPDATE sys_dict
SET dict_label = '终端工厂'
WHERE dict_type = 'customer_type'
  AND dict_key = 'end_user';