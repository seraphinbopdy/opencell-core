package org.meveo.service.catalog.impl;

import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.catalog.DiscountPlanItem;
import org.meveo.model.catalog.TradingDiscountPlanItem;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Query;

/**
 * Persistence service for entity TradingDiscountPlanItem
 */
@Stateless
public class TradingDiscountPlanItemService extends PersistenceService<TradingDiscountPlanItem> {

	public TradingDiscountPlanItem findByDiscountPlanItemAndCurrency(DiscountPlanItem discountPlanItem, TradingCurrency tradingCurrency) {
		Query query = getEntityManager().createNamedQuery("TradingDiscountPlanItem.getByDiscountPlanItemAndCurrency");
		query.setParameter("discountPlanItem", discountPlanItem);
		query.setParameter("tradingCurrency", tradingCurrency);
		
		try {
			return (TradingDiscountPlanItem) query.getSingleResult();
		} catch (NoResultException | NonUniqueResultException e) {
            return null;
        }
	}
    
}
