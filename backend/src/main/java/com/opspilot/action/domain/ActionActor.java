package com.opspilot.action.domain;

public record ActionActor(
        String email,
        UserRole role
) {
    public static ActionActor localDefault() {
        return new ActionActor("local-user@example.com", UserRole.OPERATOR);
    }
}
