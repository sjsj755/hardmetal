package com.hardmetal.export.customer;

import com.hardmetal.export.auth.RequireRole;
import com.hardmetal.export.customer.dto.ContactUpsertRequest;
import com.hardmetal.export.customer.dto.ContactView;
import com.hardmetal.export.customer.dto.CreatedCustomer;
import com.hardmetal.export.customer.dto.CustomerDetailView;
import com.hardmetal.export.customer.dto.CustomerUpsertRequest;
import com.hardmetal.export.customer.dto.CustomerView;
import com.hardmetal.export.customer.dto.TimelineItem;
import com.hardmetal.export.shared.web.ApiResponse;
import com.hardmetal.export.shared.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户接口，《接口文档》3.1–3.6。
 * <p>联系人维护（/{id}/contacts）为契约外的必要补充，详情接口已聚合联系人读取。
 */
@Tag(name = "客户", description = "客户档案、联系人、时间轴")
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "客户列表", description = "分页查询客户。SALES 角色只返回自己名下客户；grossMarginAvg 受 cost:read 权限控制，P0 暂不返回。")
    @GetMapping
    public ApiResponse<PageResult<CustomerView>> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，最大 200") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "客户类型，字典 customer_type") @RequestParam(required = false) String type,
            @Parameter(description = "国家代码，如 DE") @RequestParam(required = false) String country,
            @Parameter(description = "等级 A/B/C/D") @RequestParam(required = false) String level,
            @Parameter(description = "归属业务员用户 id") @RequestParam(required = false) Long ownerId,
            @Parameter(description = "按编码 / 中英文名模糊搜索") @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(customerService.page(page, size, type, country, level, ownerId, keyword));
    }

    @Operation(summary = "客户详情", description = "返回档案、联系人、价格摘要、最近 5 条报价与订单统计。")
    @GetMapping("/{id}")
    public ApiResponse<CustomerDetailView> detail(
            @Parameter(description = "客户 id") @PathVariable long id) {
        return ApiResponse.ok(customerService.get(id));
    }

    @Operation(summary = "新建客户", description = "创建成功返回新客户 id 与最终编码；编码重复返回 40903。SALES 只能把 ownerId 设为自己；制裁状态固定为 unreviewed（未实现筛查）。")
    @PostMapping
    @RequireRole({"ADMIN", "SALES"})
    public ApiResponse<CreatedCustomer> create(@Valid @RequestBody CustomerUpsertRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @Operation(summary = "更新客户", description = "部分更新：只传要改的字段，未传的字段保持不变。ownerId 仅 ADMIN 可改给任意合法用户，SALES 只能保持/设为自己。")
    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SALES"})
    public ApiResponse<Void> update(
            @Parameter(description = "客户 id") @PathVariable long id,
            @Valid @RequestBody CustomerUpsertRequest request) {
        customerService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除客户", description = "软删除。存在未结报价或订单时返回 40904。")
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public ApiResponse<Void> delete(
            @Parameter(description = "客户 id") @PathVariable long id) {
        customerService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "客户时间轴", description = "按时间倒序返回报价、订单等事件；契约未定义 total。")
    @GetMapping("/{id}/timeline")
    public ApiResponse<Map<String, Object>> timeline(
            @Parameter(description = "客户 id") @PathVariable long id,
            @Parameter(description = "事件类型：quote / order") @RequestParam(required = false) String type,
            @Parameter(description = "起始日期，ISO 日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "截止日期，ISO 日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page) {
        List<TimelineItem> items = customerService.timeline(id, type, from, to, page);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        return ApiResponse.ok(data);
    }

    @Operation(summary = "联系人列表", description = "契约外补充接口，供详情页维护联系人。")
    @GetMapping("/{id}/contacts")
    public ApiResponse<List<ContactView>> contacts(
            @Parameter(description = "客户 id") @PathVariable long id) {
        return ApiResponse.ok(customerService.contacts(id));
    }

    @Operation(summary = "新增联系人", description = "契约外补充接口。")
    @PostMapping("/{id}/contacts")
    @RequireRole({"ADMIN", "SALES"})
    public ApiResponse<ContactView> addContact(
            @Parameter(description = "客户 id") @PathVariable long id,
            @Valid @RequestBody ContactUpsertRequest request) {
        return ApiResponse.ok(customerService.addContact(id, request));
    }

    @Operation(summary = "删除联系人", description = "契约外补充接口。")
    @DeleteMapping("/{id}/contacts/{contactId}")
    @RequireRole({"ADMIN", "SALES"})
    public ApiResponse<Void> deleteContact(
            @Parameter(description = "客户 id") @PathVariable long id,
            @Parameter(description = "联系人 id") @PathVariable long contactId) {
        customerService.deleteContact(id, contactId);
        return ApiResponse.ok();
    }
}