package com.opspilot.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspilot.ai")
public class AiProperties {

    private String provider = "stub";
    private OpenAi openai = new OpenAi();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public static class OpenAi {

        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-5.4-mini";
        private String reasoningEffort = "low";
        private int maxOutputTokens = 1200;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getReasoningEffort() {
            return reasoningEffort;
        }

        public void setReasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }
    }
}
