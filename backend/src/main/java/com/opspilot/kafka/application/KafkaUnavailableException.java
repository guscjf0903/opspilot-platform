package com.opspilot.kafka.application;

public class KafkaUnavailableException extends RuntimeException {

    public KafkaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
