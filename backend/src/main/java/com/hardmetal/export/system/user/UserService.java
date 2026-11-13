package com.hardmetal.export.system.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import com.hardmetal.export.shared.web.ItemsResult;
import com.hardmetal.export.system.role.SysRole;
import com.hardmetal.export.system.role.SysRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 用户只读查询。当前只提供“归属人选项”，不提供用户增删改。
 * <p>角色 code 保持 ADMIN / SALES；业务中文叫法（老板 / 员工）由前端映射。
 */
@Service
public class UserService {

    private static final Set<String> ROLE_CODES = Set.of("ADMIN", "SALES");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public UserService(SysUserMapper userMapper, SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * 启用状态的用户选项，按 username 升序；role 缺省 SALES。
     */
    public ItemsResult<UserOptionView> options(String role) {
        String code = StringUtils.hasText(role) ? role.trim().toUpperCase() : "SALES";
        if (!ROLE_CODES.contains(code)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "role 只允许 SALES / ADMIN，当前：" + role);
        }
        SysRole roleRow = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, code)
                .last("limit 1"));
        if (roleRow == null) {
            return ItemsResult.of(List.of());
        }
        List<UserOptionView> items = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getRoleId, roleRow.getId())
                        .eq(SysUser::getStatus, 1)
                        .orderByAsc(SysUser::getUsername))
                .stream()
                .map(u -> new UserOptionView(u.getId(), u.getUsername(),
                        StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(), code))
                .toList();
        return ItemsResult.of(items);
    }
}