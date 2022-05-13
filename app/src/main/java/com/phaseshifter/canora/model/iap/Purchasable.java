package com.phaseshifter.canora.model.iap;

public interface Purchasable {
    String getSKU();

    PurchaseType getPurchaseType();

    String getPurchaseTitle();

    void setPurchaseTitle(String description);

    String getPurchaseDescription();

    void setPurchaseDescription(String description);

    String getPrice();

    void setPrice(String price);

    Boolean isPurchased();

    void setIsPurchased(Boolean isPurchased);

    Purchasable clonePurchasable();
}