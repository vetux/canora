package com.phaseshifter.canora.model.iap;

public interface IAPConnectionListener {
    /**
     * Connection has been established and the Handler is ready to process IAPs
     */
    void onConnectionReady();

    /**
     * Connection Error occured while trying to establish connection
     */
    void onConnectionError();
}