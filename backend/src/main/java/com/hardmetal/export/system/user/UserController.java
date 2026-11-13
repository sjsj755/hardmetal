package com.hardmetal.export.system.user;

import com.hardmetal.export.auth.RequireRole;
import com.hardmetal.export.shared.web.ApiResponse;
import com.hardmetal.export.shared.web.ItemsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户只读接口：客户归属选项。仅 ADMIN 可调。
 */
@Tag(name = "系统", description = "字典、基础资料与只读用户选项")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "归属用户选项",
            description = "仅 ADMIN 可调用。返回启用状态用户，按 username 升序，不含分页与敏感字段。"
                    + "role 缺省 SALES，仅允许 SALES / ADMIN（技术标识，中文叫法由前端映射）。")
    @GetMapping("/options")
    @RequireRole({"ADMIN"})
    public ApiResponse<ItemsResult<UserOptionView>> options(
            @Parameter(description = "角色 code，默认 SALES；可选 SALES / ADMIN") @RequestParam(defaultValue = "SALES") String role) {
        return ApiResponse.ok(userService.options(role));
    }
}