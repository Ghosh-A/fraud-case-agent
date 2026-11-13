package com.capstone.fraudcase.agent.common;

/** Thrown when the configured processor cannot run (no model, missing credentials, outage). */
public class ProcessorUnavailableException extends RuntimeException {

    public ProcessorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessorUnavailableException(String message) {
        super(message);
    }
}
