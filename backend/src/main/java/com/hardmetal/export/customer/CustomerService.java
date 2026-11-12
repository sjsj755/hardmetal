package com.hardmetal.export.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hardmetal.export.auth.CurrentUser;
import com.hardmetal.export.auth.CurrentUserHolder;
import com.hardmetal.export.customer.dto.ContactUpsertRequest;
import com.hardmetal.export.customer.dto.ContactView;
import com.hardmetal.export.customer.dto.CreatedCustomer;
import com.hardmetal.export.customer.dto.CustomerDetailView;
import com.hardmetal.export.customer.dto.CustomerUpsertRequest;
import com.hardmetal.export.customer.dto.CustomerView;
import com.hardmetal.export.customer.dto.PriceSummary;
import com.hardmetal.export.customer.dto.RecentQuote;
import com.hardmetal.export.customer.dto.TimelineItem;
import com.hardmetal.export.meta.dict.DictService;
import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import com.hardmetal.export.shared.web.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 客户主档与联系人。《接口文档》3.1–3.6。
 * <p>数据权限：SALES 只能看/改自己名下的客户，其余角色不受限。
 */
@Service
public class CustomerService {

    private static final Set<String> LEVELS = Set.of("A", "B", "C", "D");
    private static final int TIMELINE_SIZE = 50;

    private final CustomerMapper customerMapper;
    private final CustomerContactMapper contactMapper;
    private final JdbcTemplate jdbcTemplate;
    private final DictService dictService;

    public CustomerService(CustomerMapper customerMapper,
                           CustomerContactMapper contactMapper,
                           JdbcTemplate jdbcTemplate,
                           DictService dictService) {
        this.customerMapper = customerMapper;
        this.contactMapper = contactMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.dictService = dictService;
    }

    public PageResult<CustomerView> page(int page, int size, String type, String country, String level,
                                         Long ownerId, String keyword) {
        int p = PageResult.normalizePage(page);
        int s = PageResult.normalizeSize(size);
        CurrentUser me = CurrentUserHolder.require();
        var wrapper = Wrappers.<Customer>lambdaQuery()
                .eq(StringUtils.hasText(type), Customer::getCustomerType, type)
                .eq(StringUtils.hasText(country), Customer::getCountryCode, country == null ? null : country.toUpperCase())
                .eq(StringUtils.hasText(level), Customer::getGrade, level)
                .orderByDesc(Customer::getUpdatedAt);
        if (isSales(me)) {
            wrapper.eq(Customer::getOwnerUserId, me.id());
        } else if (ownerId != null) {
            wrapper.eq(Customer::getOwnerUserId, ownerId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Customer::getCustomerCode, kw)
                    .or().like(Customer::getNameCn, kw)
                    .or().like(Customer::getNameEn, kw)
                    .or().like(Customer::getShortName, kw));
        }
        IPage<Customer> result = customerMapper.selectPage(new Page<>(p, s), wrapper);
        Map<Long, OrderStat> stats = orderStats(result.getRecords().stream().map(Customer::getId).toList());
        List<CustomerView> items = result.getRecords().stream()
                .map(c -> new CustomerView(c.getId(), c.getCustomerCode(), c.getNameCn(), c.getNameEn(),
                        c.getCustomerType(), c.getGrade(), c.getCountryCode(),
                        listOf(c.getProcessingMaterials()), c.getBenchmarkBrand(),
                        stats.getOrDefault(c.getId(), OrderStat.EMPTY).count(),
                        stats.getOrDefault(c.getId(), OrderStat.EMPTY).lastAt()))
                .toList();
        return PageResult.of(items, result.getTotal(), p, s);
    }

    public CustomerDetailView get(long id) {
        Customer c = requireCustomer(id);
        List<ContactView> contacts = contactMapper.selectList(Wrappers.<CustomerContact>lambdaQuery()
                        .eq(CustomerContact::getCustomerId, id)
                        .orderByDesc(CustomerContact::getIsDecisionMaker)
                        .orderByAsc(CustomerContact::getId))
                .stream().map(this::toView).toList();
        OrderStat stat = orderStats(List.of(id)).getOrDefault(id, OrderStat.EMPTY);
        return new CustomerDetailView(c.getId(), c.getCustomerCode(), c.getNameCn(), c.getNameEn(),
                c.getCustomerType(), c.getGrade(), c.getCountryCode(), c.getCurrencyCode(),
                listOf(c.getProcessingMaterials()), listOf(c.getProcesses()), listOf(c.getMachineBrands()),
                c.getBenchmarkBrand(), c.getPaymentTerms(), c.getCreditDays(), c.getCreditLimit(),
                c.getSanctionsStatus(), contacts, priceSummary(id), recentQuotes(id),
                stat.count(), stat.lastAt());
    }

    @Transactional
    public CreatedCustomer create(CustomerUpsertRequest request) {
        requireText(request.code(), "客户编码 code 不能为空");
        requireText(request.nameCn(), "中文名称 nameCn 不能为空");
        requireText(request.customerType(), "客户类型 customerType 不能为空");
        requireText(request.countryCode(), "国家码 countryCode 不能为空");
        validate(request, true);

        String code = request.code().trim();
        if (codeExists(code, null)) {
            throw new BizException(ErrorCode.DUPLICATE_KEY, "客户编码已存在：" + code);
        }
        Customer entity = new Customer();
        entity.setCustomerCode(code);
        apply(entity, request);
        if (entity.getOwnerUserId() == null) {
            // 未指定归属业务员时落到当前登录用户，否则 SALES 创建后按归属过滤会看不到自己的客户
            entity.setOwnerUserId(CurrentUserHolder.require().id());
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setSanctionsStatus("clear");
        entity.setSensitiveCountry(false);
        entity.setIsDeleted(false);
        if (entity.getLanguage() == null) {
            entity.setLanguage("zh");
        }
        if (entity.getTimezone() == null) {
            entity.setTimezone("Asia/Shanghai");
        }
        if (entity.getCurrencyCode() == null) {
            entity.setCurrencyCode("USD");
        }
        if (entity.getGrade() == null) {
            entity.setGrade("C");
        }
        customerMapper.insert(entity);
        return new CreatedCustomer(entity.getId(), entity.getCustomerCode());
    }

    @Transactional
    public void update(long id, CustomerUpsertRequest request) {
        Customer entity = requireCustomer(id);
        validate(request, false);
        if (StringUtils.hasText(request.code())) {
            String code = request.code().trim();
            if (!code.equals(entity.getCustomerCode()) && codeExists(code, id)) {
                throw new BizException(ErrorCode.DUPLICATE_KEY, "客户编码已存在：" + code);
            }
            entity.setCustomerCode(code);
        }
        apply(entity, request);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        customerMapper.updateById(entity);
    }

    @Transactional
    public void delete(long id) {
        requireCustomer(id);
        Integer used = jdbcTemplate.queryForObject(
                "select (select count(*) from quotation where customer_id = ? and is_deleted = false)"
                        + " + (select count(*) from sales_order where customer_id = ? and is_deleted = false)",
                Integer.class, id, id);
        if (used != null && used > 0) {
            throw new BizException(ErrorCode.UNCLOSED_BUSINESS, "该客户存在 " + used + " 条报价或订单，不可删除");
        }
        // 软删除同时刷新 updated_at，保留“何时被删”的痕迹；逻辑删除条件由 MyBatis-Plus 自动附加
        customerMapper.update(null, Wrappers.<Customer>lambdaUpdate()
                .eq(Customer::getId, id)
                .set(Customer::getIsDeleted, true)
                .set(Customer::getUpdatedAt, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    public List<ContactView> contacts(long customerId) {
        requireCustomer(customerId);
        return contactMapper.selectList(Wrappers.<CustomerContact>lambdaQuery()
                        .eq(CustomerContact::getCustomerId, customerId)
                        .orderByDesc(CustomerContact::getIsDecisionMaker)
                        .orderByAsc(CustomerContact::getId))
                .stream().map(this::toView).toList();
    }

    @Transactional
    public ContactView addContact(long customerId, ContactUpsertRequest request) {
        requireCustomer(customerId);
        CustomerContact contact = new CustomerContact();
        contact.setCustomerId(customerId);
        contact.setName(request.name().trim());
        contact.setPosition(request.position());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setImType(request.imType());
        contact.setImAccount(request.imAccount());
        contact.setLanguage(request.language());
        contact.setIsDecisionMaker(Boolean.TRUE.equals(request.isDecisionMaker()));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        contact.setCreatedAt(now);
        contact.setUpdatedAt(now);
        contact.setIsDeleted(false);
        contactMapper.insert(contact);
        return toView(contact);
    }

    @Transactional
    public void deleteContact(long customerId, long contactId) {
        CustomerContact contact = contactMapper.selectById(contactId);
        if (contact == null || !Long.valueOf(customerId).equals(contact.getCustomerId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "联系人不存在：" + contactId);
        }
        contactMapper.update(null, Wrappers.<CustomerContact>lambdaUpdate()
                .eq(CustomerContact::getId, contactId)
                .set(CustomerContact::getIsDeleted, true)
                .set(CustomerContact::getUpdatedAt, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    public List<TimelineItem> timeline(long customerId, String type, LocalDate from, LocalDate to, int page) {
        requireCustomer(customerId);
        int p = PageResult.normalizePage(page);
        StringBuilder sql = new StringBuilder("""
                select 'quote' as typ, q.created_at as at, q.id as ref_id, q.quote_no as ref_no,
                       q.incoterm as incoterm, q.dest_port_code as port_code, q.currency_code as currency,
                       q.total_amount as amount, q.status as status, u.real_name as actor
                from quotation q left join sys_user u on u.id = q.created_by
                where q.customer_id = ? and q.is_deleted = false
                """);
        List<Object> args = new ArrayList<>();
        args.add(customerId);
        boolean wantQuote = !StringUtils.hasText(type) || "quote".equals(type);
        boolean wantOrder = !StringUtils.hasText(type) || "order".equals(type);
        if (!wantQuote) {
            sql.setLength(0);
            sql.append("select 'order' as typ, o.created_at as at, o.id as ref_id, o.order_no as ref_no,"
                    + " o.incoterm, null as port_code, o.currency_code as currency, o.amount, o.status,"
                    + " u.real_name as actor from sales_order o"
                    + " left join sys_user u on u.id = o.created_by"
                    + " where o.customer_id = ? and o.is_deleted = false");
        } else if (wantOrder) {
            sql.append(" union all select 'order', o.created_at, o.id, o.order_no, o.incoterm, null,"
                    + " o.currency_code, o.amount, o.status, u.real_name from sales_order o"
                    + " left join sys_user u on u.id = o.created_by"
                    + " where o.customer_id = ? and o.is_deleted = false");
            args.add(customerId);
        }
        if (!wantQuote && !wantOrder) {
            return List.of();
        }
        String head = "select * from (" + sql + ") t where 1 = 1";
        StringBuilder finalSql = new StringBuilder(head);
        List<Object> finalArgs = new ArrayList<>(args);
        if (from != null) {
            finalSql.append(" and t.at >= ?::date");
            finalArgs.add(from.toString());
        }
        if (to != null) {
            finalSql.append(" and t.at < (?::date + interval '1 day')");
            finalArgs.add(to.toString());
        }
        finalSql.append(" order by t.at desc limit ? offset ?");
        finalArgs.add(TIMELINE_SIZE);
        finalArgs.add((long) (p - 1) * TIMELINE_SIZE);

        return jdbcTemplate.query(finalSql.toString(), (rs, rowNum) -> {
            String typ = rs.getString("typ");
            String refNo = rs.getString("ref_no");
            String incoterm = rs.getString("incoterm");
            String portCode = rs.getString("port_code");
            String currency = rs.getString("currency");
            BigDecimal amount = rs.getBigDecimal("amount");
            String title;
            if ("quote".equals(typ)) {
                title = "报价 " + refNo + "（" + incoterm + (portCode == null ? "" : " " + portCode)
                        + "，" + currency + " " + plain(amount) + "）";
            } else {
                title = "订单 " + refNo + "（" + currency + " " + plain(amount) + "）";
            }
            return new TimelineItem(rs.getObject("at", OffsetDateTime.class).toString(), typ, title,
                    rs.getLong("ref_id"), "quote".equals(typ) ? "quotation" : "sales_order",
                    rs.getString("actor"));
        }, finalArgs.toArray());
    }

    // ---------- 内部 ----------

    private boolean isSales(CurrentUser user) {
        return "SALES".equals(user.roleCode());
    }

    private Customer requireCustomer(long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户不存在或已删除：" + id);
        }
        CurrentUser me = CurrentUserHolder.require();
        if (isSales(me) && !me.id().equals(customer.getOwnerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看他人客户");
        }
        return customer;
    }

    private void validate(CustomerUpsertRequest r, boolean creating) {
        if (creating || r.customerType() != null) {
            // 取值以 sys_dict 为准，不再维护 Java 常量副本
            dictService.assertAllowed("customer_type", r.customerType(), "customerType");
        }
        if (r.level() != null && !LEVELS.contains(r.level())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "level 只允许 A/B/C/D，当前：" + r.level());
        }
        if (r.countryCode() != null && !r.countryCode().matches("^[A-Za-z]{2}$")) {
            throw new BizException(ErrorCode.PARAM_INVALID, "countryCode 必须是 ISO 两位国家码");
        }
        if (r.currency() != null && !r.currency().matches("^[A-Za-z]{3}$")) {
            throw new BizException(ErrorCode.PARAM_INVALID, "currency 必须是 ISO 三位货币码");
        }
        if (r.creditDays() != null && (r.creditDays() < 0 || r.creditDays() > 365)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "creditDays 必须在 0–365 之间");
        }
        if (r.machiningMaterials() != null && r.machiningMaterials().size() > 16) {
            throw new BizException(ErrorCode.PARAM_INVALID, "machiningMaterials 最多 16 项");
        }
        if (r.machiningOps() != null && r.machiningOps().size() > 16) {
            throw new BizException(ErrorCode.PARAM_INVALID, "machiningOps 最多 16 项");
        }
    }

    /** null 字段表示“不改”。 */
    private void apply(Customer e, CustomerUpsertRequest r) {
        if (r.nameCn() != null) {
            e.setNameCn(r.nameCn().trim());
        }
        if (r.nameEn() != null) {
            e.setNameEn(r.nameEn());
        }
        if (r.customerType() != null) {
            e.setCustomerType(r.customerType());
        }
        if (r.level() != null) {
            e.setGrade(r.level());
        }
        if (r.countryCode() != null) {
            e.setCountryCode(r.countryCode().toUpperCase());
        }
        if (r.currency() != null) {
            e.setCurrencyCode(r.currency().toUpperCase());
        }
        if (r.machiningMaterials() != null) {
            e.setProcessingMaterials(r.machiningMaterials().toArray(new String[0]));
        }
        if (r.machiningOps() != null) {
            e.setProcesses(r.machiningOps().toArray(new String[0]));
        }
        if (r.machineBrands() != null) {
            e.setMachineBrands(r.machineBrands().toArray(new String[0]));
        }
        if (r.benchmarkBrand() != null) {
            e.setBenchmarkBrand(r.benchmarkBrand());
        }
        if (r.paymentTerms() != null) {
            e.setPaymentTerms(r.paymentTerms());
        }
        if (r.creditDays() != null) {
            e.setCreditDays(r.creditDays());
        }
        if (r.creditLimit() != null) {
            e.setCreditLimit(r.creditLimit());
        }
        if (r.ownerId() != null) {
            e.setOwnerUserId(r.ownerId());
        }
        if (r.remark() != null) {
            e.setRemark(r.remark());
        }
    }

    private PriceSummary priceSummary(long customerId) {
        Integer active = jdbcTemplate.queryForObject(
                "select count(*) from price_list where customer_id = ? and is_deleted = false"
                        + " and effective_from <= current_date"
                        + " and (effective_to is null or effective_to >= current_date)",
                Integer.class, customerId);
        Integer expiring = jdbcTemplate.queryForObject(
                "select count(*) from price_list where customer_id = ? and is_deleted = false"
                        + " and effective_to between current_date and current_date + 30",
                Integer.class, customerId);
        return new PriceSummary(active == null ? 0 : active, expiring == null ? 0 : expiring);
    }

    private List<RecentQuote> recentQuotes(long customerId) {
        return jdbcTemplate.query("""
                select id, quote_no, status, total_amount, currency_code
                from quotation where customer_id = ? and is_deleted = false
                order by created_at desc limit 5
                """, (rs, i) -> new RecentQuote(rs.getLong("id"), rs.getString("quote_no"),
                rs.getString("status"), rs.getBigDecimal("total_amount"), rs.getString("currency_code")),
                customerId);
    }

    private Map<Long, OrderStat> orderStats(List<Long> customerIds) {
        Map<Long, OrderStat> map = new HashMap<>();
        if (customerIds.isEmpty()) {
            return map;
        }
        String placeholders = String.join(",", customerIds.stream().map(x -> "?").toList());
        jdbcTemplate.query(
                "select customer_id, count(*) as cnt, max(created_at)::date as last_at"
                        + " from sales_order where is_deleted = false and customer_id in (" + placeholders + ")"
                        + " group by customer_id",
                rs -> {
                    long id = rs.getLong("customer_id");
                    map.put(id, new OrderStat(rs.getLong("cnt"),
                            rs.getObject("last_at", LocalDate.class)));
                },
                customerIds.toArray());
        return map;
    }

    private ContactView toView(CustomerContact c) {
        return new ContactView(c.getId(), c.getName(), c.getPosition(), c.getEmail(), c.getPhone(),
                c.getImType(), c.getImAccount(), c.getLanguage(), Boolean.TRUE.equals(c.getIsDecisionMaker()));
    }

    private static List<String> listOf(String[] values) {
        return values == null ? List.of() : Arrays.asList(values);
    }

    /** 编码唯一性：软删记录同样占用编码，避免历史单据引用的客户编码被复用。 */
    private boolean codeExists(String code, Long excludeId) {
        String sql = excludeId == null
                ? "select count(*) from customer where customer_code = ?"
                : "select count(*) from customer where customer_code = ? and id <> ?";
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, code)
                : jdbcTemplate.queryForObject(sql, Integer.class, code, excludeId);
        return count != null && count > 0;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0.00"
                : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.PARAM_INVALID, message);
        }
    }

    private record OrderStat(long count, LocalDate lastAt) {
        private static final OrderStat EMPTY = new OrderStat(0, null);
    }
}