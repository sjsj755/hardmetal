package com.hardmetal.export.system.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户选项：供前端“客户归属”下拉使用，只暴露最小字段。
 * <p>不返回密码哈希、锁定信息、部门、联系方式等敏感或无关字段。
 */
@Schema(description = "用户选项（客户归属下拉）")
public record UserOptionView(
        @Schema(description = "用户 id", example = "2")
        long id,

        @Schema(description = "登录名", example = "sales1")
        String username,

        @Schema(description = "显示名：优先真实姓名，为空则用户名", example = "张三")
        String realName,

        @Schema(description = "角色技术标识，固定为 ADMIN / SALES", example = "SALES")
        String roleCode) {
}