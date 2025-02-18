package org.meveo.api.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

public class TradingPriceEL {

    @Schema(description = "Currency code")
    private String currencyCode;

    @Schema(description = "Trading price EL")
    private String priceEL;

    public TradingPriceEL() {
    }

    public TradingPriceEL(String currencyCode, String priceEL) {
        this.currencyCode = currencyCode;
        this.priceEL = priceEL;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getPriceEL() {
        return priceEL;
    }

    public void setPriceEL(String priceEL) {
        this.priceEL = priceEL;
    }
}
