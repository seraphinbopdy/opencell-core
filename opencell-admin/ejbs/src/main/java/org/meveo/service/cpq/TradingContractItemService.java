package org.meveo.service.cpq;

import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.cpq.contract.ContractItem;
import org.meveo.model.cpq.contract.TradingContractItem;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Query;

/**
 * Persistence service for entity TradingContractItem
 */
@Stateless
public class TradingContractItemService extends PersistenceService<TradingContractItem> {

	public TradingContractItem findByContractItemAndCurrency(ContractItem contractItem, TradingCurrency tradingCurrency) {
		Query query = getEntityManager().createNamedQuery("TradingContractItem.getByContractItemAndCurrency");
		query.setParameter("contractItem", contractItem);
		query.setParameter("tradingCurrency", tradingCurrency);
		
		try {
			return (TradingContractItem) query.getSingleResult();
		} catch (NoResultException | NonUniqueResultException e) {
            return null;
        }
	}
    
}
