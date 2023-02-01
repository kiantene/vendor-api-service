package com.nextgen.gameaggregator.exception;

public class InvalidVendorResponseException extends Exception{

    public InvalidVendorResponseException() {
        super();
    }
    public InvalidVendorResponseException(String message) {
        super(message);
    }
}
