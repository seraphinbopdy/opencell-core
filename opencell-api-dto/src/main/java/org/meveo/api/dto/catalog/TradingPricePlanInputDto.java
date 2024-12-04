package org.meveo.api.dto.catalog;

import org.meveo.api.dto.BaseEntityDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class TradingPricePlanInputDto extends BaseEntityDto {

	private static final long serialVersionUID = -3498847257781821440L;

    @Schema(description = "price plan matrix version id")
	private Long pricePlanMatrixVersionId;

    @Schema(description = "trading currency")
	private TradingCurrencyDto tradingCurrency;

	/**
	 * @return the pricePlanMatrixVersionId
	 */
	public Long getPricePlanMatrixVersionId() {
		return pricePlanMatrixVersionId;
	}

	/**
	 * @param pricePlanMatrixVersionId the pricePlanMatrixVersionId to set
	 */
	public void setPricePlanMatrixVersionId(Long pricePlanMatrixVersionId) {
		this.pricePlanMatrixVersionId = pricePlanMatrixVersionId;
	}

	/**
	 * @return the tradingCurrency
	 */
	public TradingCurrencyDto getTradingCurrency() {
		return tradingCurrency;
	}

	/**
	 * @param tradingCurrency the tradingCurrency to set
	 */
	public void setTradingCurrency(TradingCurrencyDto tradingCurrency) {
		this.tradingCurrency = tradingCurrency;
	}
	
}
