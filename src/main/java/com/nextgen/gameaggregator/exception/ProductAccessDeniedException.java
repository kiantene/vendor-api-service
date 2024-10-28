package com.nextgen.gameaggregator.exception;

public class ProductAccessDeniedException extends Exception {
    public ProductAccessDeniedException() {
        super();
    }

    public ProductAccessDeniedException(String message) {
        super(message);
    }
}
