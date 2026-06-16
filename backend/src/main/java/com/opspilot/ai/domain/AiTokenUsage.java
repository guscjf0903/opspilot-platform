package com.opspilot.ai.domain;

public record AiTokenUsage(
        int inputTokens,
        int cachedInputTokens,
        int outputTokens,
        int reasoningOutputTokens,
        int totalTokens
) {

    public static AiTokenUsage zero() {
        return new AiTokenUsage(0, 0, 0, 0, 0);
    }
}
