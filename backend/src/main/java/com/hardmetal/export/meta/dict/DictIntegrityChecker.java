package com.hardmetal.export.meta.dict;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 启动自检：customer_type 的 sys_dict 与 DDL CHECK 必须完全一致。
 * <p>DDL 属于不可变的历史迁移，字典可在运行期维护；两者一旦漂移，
 * 这里会在启动阶段直接失败，避免等到某个值提交后才暴露。
 * 修改枚举时应同时提交新的迁移（改 CHECK）与字典数据，然后重启。</p>
 */
@Component
public class DictIntegrityChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DictIntegrityChecker.class);
    private static final Pattern LITERAL = Pattern.compile("'([^']+)'");
    private static final String TABLE = "customer";
    private static final String CONSTRAINT = "ck_customer_type";
    private static final String DICT_TYPE = "customer_type";

    private final JdbcTemplate jdbcTemplate;
    private final DictService dictService;

    public DictIntegrityChecker(JdbcTemplate jdbcTemplate, DictService dictService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dictService = dictService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> defs = jdbcTemplate.queryForList(
                "select pg_get_constraintdef(c.oid) from pg_constraint c "
                        + "join pg_class t on t.oid = c.conrelid "
                        + "join pg_namespace n on n.oid = t.relnamespace "
                        + "where t.relname = ? and n.nspname = current_schema() and c.conname = ?",
                String.class, TABLE, CONSTRAINT);
        if (defs.isEmpty()) {
            throw new IllegalStateException("启动自检失败：找不到约束 " + TABLE + "." + CONSTRAINT
                    + "，无法校验 " + DICT_TYPE + " 字典一致性");
        }

        // pg_get_constraintdef 形如 CHECK (((customer_type)::text = ANY ((ARRAY['a'::character varying, ...])::text[])))
        Set<String> checkValues = new LinkedHashSet<>();
        Matcher m = LITERAL.matcher(defs.get(0));
        while (m.find()) {
            checkValues.add(m.group(1));
        }

        List<String> dictValues = dictService.values(DICT_TYPE);
        if (!new TreeSet<>(checkValues).equals(new TreeSet<>(dictValues))) {
            throw new IllegalStateException("启动自检失败：" + DICT_TYPE + " 字典与 DDL CHECK 不一致。"
                    + " 字典=" + dictValues + "，CHECK=" + checkValues
                    + "；请修改 sys_dict 与迁移脚本，保持一致后重启");
        }
        log.info("启动自检通过：{} 字典与 DDL CHECK 一致 {}", DICT_TYPE, dictValues);
    }
}