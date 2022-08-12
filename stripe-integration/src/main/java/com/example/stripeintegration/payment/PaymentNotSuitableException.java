package com.example.stripeintegration.payment;

public class PaymentNotSuitableException extends PaymentException {
    public PaymentNotSuitableException(String message) {
        super(message);
    }
}
