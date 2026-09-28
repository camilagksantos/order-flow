package com.camilagksantos.orderflow.infrastructure.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SecurityUtils {

    private SecurityUtils() {}

    public static void requireCustomerAccess(Long pathCustomerId) {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Object customerId = request.getAttribute("customerId");

        if (customerId == null || !customerId.equals(pathCustomerId)) {
            throw new AccessDeniedException("Cannot access another customer's resource");
        }
    }
}