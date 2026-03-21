package com.rkortega94.scanner.utils;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControllerUtils {
    private ControllerUtils() {
    }

    private static final Pattern ROLE_PATTERN = Pattern.compile("hasRole\\(['\"](.*?)['\"]\\)|hasAnyRole\\((.*?)\\)");
    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("hasAuthority\\(['\"](.*?)['\"]\\)|hasAnyAuthority\\((.*?)\\)");

    public static Set<String> extractRoles(Method method) {
        return Optional.ofNullable(method.getAnnotation(PreAuthorize.class)) //
                .map(PreAuthorize::value) //
                .map(expression -> parseValues(expression, ROLE_PATTERN)) //
                .orElse(new HashSet<>());
    }

    public static Set<String> extractAuthorities(Method method) {
        return Optional.ofNullable(method.getAnnotation(PreAuthorize.class)) //
                .map(PreAuthorize::value) //
                .map(expression -> parseValues(expression, AUTHORITY_PATTERN)) //
                .orElse(new HashSet<>());
    }

    private static Set<String> parseValues(String expression, Pattern pattern) {
        Set<String> result = new HashSet<>();
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()) {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (group == null || group.isBlank()) {
                continue;
            }

            Arrays.stream(group.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(value -> value.replaceAll("^['\"]|['\"]$", ""))
                    .map(String::trim)
                    .forEach(result::add);
        }
        return result;
    }
}