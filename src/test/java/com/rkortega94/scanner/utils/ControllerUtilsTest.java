package com.rkortega94.scanner.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControllerUtilsTest {

    private static class SecuredMethods {

        @PreAuthorize("hasAnyRole('ADMIN', ' USER ') and hasRole(\"MANAGER\")")
        public void withRoles() {
        }

        @PreAuthorize("hasAnyAuthority('read', \" write \" ) or hasAuthority('delete')")
        public void withAuthorities() {
        }
    }

    @Test
    void extractRoles_shouldTrimAndNormalizeQuotedValues() throws Exception {
        Method method = SecuredMethods.class.getDeclaredMethod("withRoles");

        Set<String> roles = ControllerUtils.extractRoles(method);

        assertEquals(Set.of("ADMIN", "USER", "MANAGER"), roles);
    }

    @Test
    void extractAuthorities_shouldTrimAndNormalizeQuotedValues() throws Exception {
        Method method = SecuredMethods.class.getDeclaredMethod("withAuthorities");

        Set<String> authorities = ControllerUtils.extractAuthorities(method);

        assertEquals(Set.of("read", "write", "delete"), authorities);
    }
}
