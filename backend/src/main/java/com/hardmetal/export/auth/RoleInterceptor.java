package com.hardmetal.export.auth;

import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 角色拦截：读取 {@link RequireRole}，管理员 ADMIN 天然放行。
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireRole require = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireRole.class);
        if (require == null) {
            require = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireRole.class);
        }
        if (require == null) {
            return true;
        }
        CurrentUser user = CurrentUserHolder.require();
        if ("ADMIN".equals(user.roleCode())) {
            return true;
        }
        boolean allowed = Arrays.asList(require.value()).contains(user.roleCode());
        if (!allowed) {
            throw new BizException(ErrorCode.FORBIDDEN, "角色 " + user.roleCode() + " 无权访问该接口");
        }
        return true;
    }
}