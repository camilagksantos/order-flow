package com.camilagksantos.orderflow.infrastructure.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SecurityUtils {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private SecurityUtils() {}

    public static void requireCustomerAccess(Long pathCustomerId) {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Object customerId = request.getAttribute("customerId");

        if (customerId == null || !customerId.equals(pathCustomerId)) {
            throw new AccessDeniedException("Cannot access another customer's resource");
        }
    }

    public static void requireOrderAccess(Long orderCustomerId) {
        if (isAdmin()) {
            return;
        }
        requireCustomerAccess(orderCustomerId);
    }

    public static void requireCustomerOrAdminAccess(Long pathCustomerId) {
        if (isAdmin()) {
            return;
        }
        requireCustomerAccess(pathCustomerId);
    }

    private static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }
}