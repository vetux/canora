package com.phaseshifter.canora.model.iap;

import android.app.Activity;
import com.phaseshifter.canora.model.iap.exceptions.IAPConnectionInterruptException;
import com.phaseshifter.canora.model.iap.exceptions.IAPConnectionPendingException;
import com.phaseshifter.canora.model.iap.exceptions.IAPTransactionOngoingException;

import java.util.List;

public interface IAPHandler {
    /**
     * Initialize Connection to IAP Service, run the respective callback in the listener on completion.
     * If there is already an Connection Attempt ongoing the function will throw an IAPConnectionPendingException.
     * If any function on the handler is called while the connection to the IAP Service is not yet established, the function will throw a IAPConnectionPendingException.
     *
     * @param listener
     * @throws IAPConnectionPendingException
     */
    void openConnection(IAPConnectionListener listener) throws IAPConnectionPendingException;

    /**
     * Close connection to IAP Service.
     */
    void closeConnection();

    /**
     * This Function will try to make a network request to gather purchase information for the supplied Purchasables.
     * If a network connection cannot be established the function will call the onNetworkError in the listener.
     * <p>
     * Populates the dynamic purchase data for each of the supplied purchases(Price, Description, Bought State, etc.) derived from the supplied SKU.
     * If the Connection to the IAP Service is lost while/before this function is executing it will throw a IAPConnectionInterruptException.
     *
     * @param listener
     * @param purchases
     * @throws IAPConnectionPendingException
     * @throws IAPConnectionInterruptException
     */
    void queryNetworkData(IAPQueryListener listener, List<Purchasable> purchases) throws IAPConnectionPendingException, IAPConnectionInterruptException;

    /**
     * This Function will query the IAP Service for cached purchases in case there is no network connection.
     * If the Connection to the IAP Service is lost while/before this function is executing it will throw a IAPConnectionInterruptException.
     *
     * @return A list of verified Purchasables with their SKU and purchase flag set.
     * @throws IAPConnectionPendingException
     * @throws IAPConnectionInterruptException
     */
    List<Purchasable> queryCachedData() throws IAPConnectionPendingException, IAPConnectionInterruptException;

    /**
     * Initiates an Transaction.
     * If the Connection to the IAP Service is lost while/before this function is executing it will throw a IAPConnectionInterruptException.
     * If there is already an ongoing Purchase process the function will throw an IAPTransactionOngoingException exception.
     *
     * @param hostActivity
     * @param transaction
     * @throws IAPConnectionPendingException
     * @throws IAPConnectionInterruptException
     * @throws IAPTransactionOngoingException
     */
    void requestPurchase(Activity hostActivity, IAPTransaction transaction) throws IAPConnectionPendingException, IAPConnectionInterruptException, IAPTransactionOngoingException;
}