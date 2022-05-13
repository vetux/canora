package com.phaseshifter.canora.model.iap;

public interface IAPTransactionListener {
    /**
     * Called as the asynchronous Billing Flow is started.
     */
    void onTransactionStarted();

    /**
     * Called when an Error at any stage occurs.
     */
    void onTransactionError();

    /**
     * Called when the transaction was completed successfully, including user cancellation of the transaction.
     *
     * @param result
     */
    void onTransactionSuccess(TransactionResult result);
}