package com.phaseshifter.canora.model.iap;

import java.util.List;

public interface IAPQueryListener {
    /**
     * The IAPHandler has successfully and as accurately as possible determined the purchasable states.
     *
     * @param p The list of purchasables with their iap data set.
     */
    void onQuerySuccess(List<Purchasable> p);

    /**
     * Network Error occured.
     */
    void onNetworkError();

    /**
     * IAP Service Connection error.
     */
    void onServiceConnectionError();
}