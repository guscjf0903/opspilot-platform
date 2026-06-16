package com.opspilot.action.domain;

import java.util.Locale;

public enum UserRole {
    ADMIN,
    OPERATOR,
    DEVELOPER,
    VIEWER;

    public static UserRole fromValue(String value) {
        if (value == null || value.isBlank()) {
            return OPERATOR;
        }

        return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
