package com.warhammer.towerdefense.data;

/** Raised when a data file is missing, malformed or references unknown content. */
public class DataException extends RuntimeException {
    public DataException(String message) {
        super(message);
    }

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
