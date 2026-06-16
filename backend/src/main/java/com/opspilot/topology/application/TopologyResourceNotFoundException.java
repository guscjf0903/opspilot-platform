package com.opspilot.topology.application;

public class TopologyResourceNotFoundException extends RuntimeException {

    public TopologyResourceNotFoundException(String kind, String name) {
        super("Topology resource not found: %s/%s".formatted(kind, name));
    }
}
