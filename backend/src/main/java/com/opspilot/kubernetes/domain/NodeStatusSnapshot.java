package com.opspilot.kubernetes.domain;

import java.util.Map;

public record NodeStatusSnapshot(
        Map<String, String> conditions
) {
}
