package org.meveo.service.catalog.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.catalog.PricePlanMatrixVersion;
import org.meveo.model.catalog.TradingPricePlanMatrixLine;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

@Stateless
public class TradingPricePlanMatrixLineService extends PersistenceService<TradingPricePlanMatrixLine> {

	public void disableOrEnableAllTradingPricePlanMatrixLine(Set<Long> ids, boolean enable) {
		if(CollectionUtils.isNotEmpty(ids)) {
			this.getEntityManager().createNamedQuery("TradingPricePlanMatrixLine.enableOrDisable")
									.setParameter("enable", enable)
									.setParameter("ids", ids).executeUpdate();
		}
	}
	
	public List<TradingPricePlanMatrixLine> getByPricePlanMatrixVersionAndCurrency(PricePlanMatrixVersion ppmv,	TradingCurrency tradingCurrency) {
		try {
			return getEntityManager()
					.createNamedQuery("TradingPricePlanMatrixLine.getByPricePlanMatrixVersionAndCurrency", TradingPricePlanMatrixLine.class)
					.setParameter("ppmv", ppmv)
					.setParameter("tradingCurrency", tradingCurrency)
					.getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}

}
