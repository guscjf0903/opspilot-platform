package com.opspilot.action.domain;

public record ActionDiff(
        String field,
        String beforeValue,
        String afterValue
) {
}
