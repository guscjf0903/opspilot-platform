package com.opspilot.kubernetes.domain;

import java.util.List;

public record PodStatusSnapshot(
        String phase,
        List<String> waitingReasons,
        List<String> currentTerminatedReasons,
        List<String> previousTerminatedReasons,
        int restartCount
) {
}
