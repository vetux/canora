package com.phaseshifter.canora.model.iap;

public class IAPTransaction {
    private final Purchasable purchase;
    private final IAPTransactionListener listener;

    public IAPTransaction(Purchasable purchase, IAPTransactionListener listener) {
        this.purchase = purchase;
        this.listener = listener;
    }

    public Purchasable getPurchasable() {
        return purchase;
    }

    public IAPTransactionListener getListener() {
        return listener;
    }
}