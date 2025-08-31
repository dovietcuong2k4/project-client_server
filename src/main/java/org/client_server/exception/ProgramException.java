package org.client_server.exception;

public class ProgramException extends RuntimeException{
    public ProgramException(String message, Throwable cause) {
        super(message, cause);
    }
}
