package com.nextgen.gameaggregator.exception;

public class InvalidGameCategoryException extends Exception {
    public InvalidGameCategoryException() {
        super();
    }
    public InvalidGameCategoryException(String message) {
        super(message);
    }
}