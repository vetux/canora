package com.phaseshifter.canora.model.iap;

public enum TransactionResult {
    OK, //User has payed for the product
    CANCELED, //User has canceled the product payment
    DENIED //Other factors such as too low account balance etc.
}