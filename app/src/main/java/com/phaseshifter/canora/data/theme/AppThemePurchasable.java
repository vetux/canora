package com.phaseshifter.canora.data.theme;

import com.phaseshifter.canora.model.iap.Purchasable;
import com.phaseshifter.canora.model.iap.PurchaseType;

import java.util.Objects;

public class AppThemePurchasable extends AppTheme implements Purchasable {
    private final String SKU;

    public AppThemePurchasable(int id, int displayNameResID, int styleResourceID, int previewResourceID, String SKU) {
        super(id, displayNameResID, styleResourceID, previewResourceID);
        this.SKU = SKU;
    }

    @Override
    public String getSKU() {
        return SKU;
    }

    @Override
    public PurchaseType getPurchaseType() {
        return null;
    }

    @Override
    public String getPurchaseTitle() {
        return null;
    }

    @Override
    public void setPurchaseTitle(String description) {

    }

    @Override
    public String getPurchaseDescription() {
        return null;
    }

    @Override
    public void setPurchaseDescription(String description) {

    }

    @Override
    public String getPrice() {
        return null;
    }

    @Override
    public void setPrice(String price) {

    }

    @Override
    public Boolean isPurchased() {
        return null;
    }

    @Override
    public void setIsPurchased(Boolean isPurchased) {

    }

    @Override
    public Purchasable clonePurchasable() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AppThemePurchasable that = (AppThemePurchasable) o;
        return Objects.equals(SKU, that.SKU);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), SKU);
    }
}