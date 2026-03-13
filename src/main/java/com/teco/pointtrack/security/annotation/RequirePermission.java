package com.teco.pointtrack.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu một endpoint yêu cầu permission cụ thể.
 *
 * <p>Ví dụ sử dụng:</p>
 * <pre>
 *   {@code @RequirePermission("USER_MANAGE")}
 *   {@code @GetMapping("/users")}
 *   public ResponseEntity<?> getUsers(...) { ... }
 * </pre>
 *
 * <p>Nhiều permission (OR logic — có một trong số là đủ):</p>
 * <pre>
 *   {@code @RequirePermission({"USER_READ", "USER_MANAGE"})}
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();
}
