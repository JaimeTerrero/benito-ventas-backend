package com.benitoventas.exceptions;

public class PurchaseRejectedException extends RuntimeException {

    public PurchaseRejectedException(String message) {
        super(message);
    }
}
