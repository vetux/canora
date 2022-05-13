package com.phaseshifter.canora.model.iap.implementation;

import com.phaseshifter.canora.model.iap.Purchasable;
import com.phaseshifter.canora.model.iap.PurchaseType;

public class SamplePurchasable implements Purchasable {
    private final String SKU;
    private final PurchaseType ptype;
    private String purchaseTitle;
    private String purchaseDescription;
    private String price;
    private Boolean isPurchased;

    public SamplePurchasable(String SKU, PurchaseType ptype, String purchaseTitle, String purchaseDescription, String price, Boolean isPurchased) {
        this.SKU = SKU;
        this.ptype = ptype;
        this.purchaseTitle = purchaseTitle;
        this.purchaseDescription = purchaseDescription;
        this.price = price;
        this.isPurchased = isPurchased;
    }

    public SamplePurchasable(SamplePurchasable copy) {
        this.SKU = copy.SKU;
        this.ptype = copy.ptype;
        this.purchaseTitle = copy.purchaseTitle;
        this.purchaseDescription = copy.purchaseDescription;
        this.price = copy.price;
        this.isPurchased = copy.isPurchased;
    }

    @Override
    public String getSKU() {
        return SKU;
    }

    @Override
    public PurchaseType getPurchaseType() {
        return ptype;
    }

    @Override
    public String getPurchaseTitle() {
        return purchaseTitle;
    }

    @Override
    public void setPurchaseTitle(String title) {
        purchaseTitle = title;
    }

    @Override
    public String getPurchaseDescription() {
        return purchaseDescription;
    }

    @Override
    public void setPurchaseDescription(String description) {
        purchaseDescription = description;
    }

    @Override
    public String getPrice() {
        return price;
    }

    @Override
    public void setPrice(String price) {
        this.price = price;
    }

    @Override
    public Boolean isPurchased() {
        return isPurchased;
    }

    @Override
    public void setIsPurchased(Boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    @Override
    public Purchasable clonePurchasable() {
        return new SamplePurchasable(this);
    }
}