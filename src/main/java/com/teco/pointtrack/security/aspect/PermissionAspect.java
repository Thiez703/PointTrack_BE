package com.teco.pointtrack.security.aspect;

import com.teco.pointtrack.exception.Forbidden;
import com.teco.pointtrack.exception.SignInRequiredException;
import com.teco.pointtrack.security.annotation.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class PermissionAspect {

    @Before("@annotation(com.teco.pointtrack.security.annotation.RequirePermission)" +
            " || @within(com.teco.pointtrack.security.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SignInRequiredException("Vui lòng đăng nhập để thực hiện thao tác này");
        }

        RequirePermission annotation = resolveAnnotation(joinPoint);
        if (annotation == null) return;

        String[] requiredCodes = annotation.value();
        if (requiredCodes.length == 0) return;

        Set<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (userAuthorities.contains("ROLE_ADMIN")) return;

        boolean hasPermission = Arrays.stream(requiredCodes).anyMatch(userAuthorities::contains);

        if (!hasPermission) {
            log.warn("Từ chối truy cập: user không có permission [{}]", Arrays.toString(requiredCodes));
            throw new Forbidden("Bạn không có quyền thực hiện thao tác này");
        }
    }

    private RequirePermission resolveAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequirePermission methodAnnotation = method.getAnnotation(RequirePermission.class);
        if (methodAnnotation != null) return methodAnnotation;

        return joinPoint.getTarget().getClass().getAnnotation(RequirePermission.class);
    }
}
