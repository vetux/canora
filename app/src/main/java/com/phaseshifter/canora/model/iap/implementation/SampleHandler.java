package com.phaseshifter.canora.model.iap.implementation;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.model.iap.*;
import com.phaseshifter.canora.model.iap.exceptions.IAPConnectionInterruptException;
import com.phaseshifter.canora.model.iap.exceptions.IAPConnectionPendingException;
import com.phaseshifter.canora.model.iap.exceptions.IAPTransactionOngoingException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SampleHandler implements IAPHandler {
    private final int FAKETIMEOUTSECONDS = 0;

    private final Boolean iapServiceInstalled = true;
    private final Boolean networkConnectivity = true;

    private final Context C;

    private final List<String> onlinePurchaseSKUs = new ArrayList<String>() {{
        add("1");
    }};

    private final List<String> cachedPurchaseSKUs = new ArrayList<String>() {{
        add("1");
    }};

    public SampleHandler(Context context) {
        C = context;
    }

    @Override
    public void openConnection(IAPConnectionListener listener) throws IAPConnectionPendingException {
        new Thread() {
            @Override
            public void run() {
                if (!iapServiceInstalled) {
                    listener.onConnectionError();
                    return;
                }
                try {
                    TimeUnit.SECONDS.sleep(FAKETIMEOUTSECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                listener.onConnectionReady();
            }
        }.start();
    }

    @Override
    public void queryNetworkData(IAPQueryListener listener, List<Purchasable> purchases) throws IAPConnectionPendingException, IAPConnectionInterruptException {
        new Thread() {
            @Override
            public void run() {
                if (!networkConnectivity) {
                    listener.onNetworkError();
                    return;
                }
                for (Purchasable p : purchases) {
                    if (onlinePurchaseSKUs.contains(p.getSKU())) {
                        //Purchased
                        p.setIsPurchased(true);
                        p.setPrice("999.00 $");
                        p.setPurchaseDescription("DUMMY PRODUCT");
                        p.setPurchaseTitle("DUMMY PRODUCT");
                    } else {
                        //Not Purchased
                        p.setIsPurchased(false);
                        p.setPrice("999.00 $");
                        p.setPurchaseDescription("DUMMY PRODUCT");
                        p.setPurchaseTitle("DUMMY PRODUCT");
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(FAKETIMEOUTSECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                listener.onQuerySuccess(purchases);
            }
        }.start();
    }

    @Override
    public void closeConnection() {

    }

    @Override
    public List<Purchasable> queryCachedData() throws IAPConnectionPendingException, IAPConnectionInterruptException {
        List<Purchasable> ret = new ArrayList<>();
        for (String s : cachedPurchaseSKUs) {
            Purchasable tmp = new SamplePurchasable(s, PurchaseType.MANAGED, "DUMMYTITLE", "DUMMYDESCRIPTION", "99$", true);
            ret.add(tmp);
        }
        return ret;
    }

    @Override
    public void requestPurchase(Activity hostActivity, IAPTransaction transaction) throws IAPConnectionPendingException, IAPConnectionInterruptException, IAPTransactionOngoingException {
        new Thread() {
            @Override
            public void run() {
                AlertDialog.Builder ab = new AlertDialog.Builder(hostActivity);
                ab.setTitle("Purchase Verification");
                ab.setMessage("Do you want to purchase " + transaction.getPurchasable().getPurchaseTitle() + " for " + transaction.getPurchasable().getPrice());

                //Needed because onDismiss is called even when the user presses a button
                final AtomicReference<Boolean> isQuitted = new AtomicReference<>(false);

                ab.setPositiveButton(hostActivity.getString(R.string.iap_dialog_requestPurchase_button0yes), (dialog, which) -> {
                    Log.v("BLA", "PRESS YES");
                    if (!isQuitted.get()) {
                        isQuitted.set(true);
                        transaction.getPurchasable().setIsPurchased(true);
                        transaction.getListener().onTransactionSuccess(TransactionResult.OK);
                    }
                });
                ab.setNegativeButton(hostActivity.getString(R.string.iap_dialog_requestPurchase_button0no), (dialog, which) -> {
                    Log.v("BLA", "PRESS NO");
                    if (!isQuitted.get()) {
                        isQuitted.set(true);
                        transaction.getPurchasable().setIsPurchased(false);
                        transaction.getListener().onTransactionSuccess(TransactionResult.CANCELED);
                    }
                });
                ab.setOnDismissListener(dialog -> {
                    Log.v("BLA", "ONDISMISS");
                    if (!isQuitted.get()) {
                        isQuitted.set(true);
                        transaction.getPurchasable().setIsPurchased(false);
                        transaction.getListener().onTransactionSuccess(TransactionResult.CANCELED);
                    }
                });
                hostActivity.runOnUiThread(() -> {
                    AlertDialog ad = ab.create();
                    ad.show();
                });
            }
        }.start();
    }
}