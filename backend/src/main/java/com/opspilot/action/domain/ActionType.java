package com.opspilot.action.domain;

import java.util.Locale;

public enum ActionType {
    RESTART_DEPLOYMENT,
    SCALE_DEPLOYMENT,
    ROLLOUT_UNDO,
    DELETE_POD;

    public static ActionType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Action type is required.");
        }

        return ActionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
