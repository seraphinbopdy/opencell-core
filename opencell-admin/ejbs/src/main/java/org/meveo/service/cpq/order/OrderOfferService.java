package org.meveo.service.cpq.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.logging.log4j.util.Strings;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.model.cpq.commercial.OfferLineTypeEnum;
import org.meveo.model.cpq.commercial.OrderOffer;
import org.meveo.model.cpq.commercial.OrderProduct;
import org.meveo.service.admin.impl.CustomGenericEntityCodeService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.settings.impl.AdvancedSettingsService;

/**
 * @author Tarik FAKHOURI
 * @version 11.0
 * @dateCreation 19/01/2021
 */
@Stateless
public class OrderOfferService extends PersistenceService<OrderOffer> {


	@Inject
	private CustomGenericEntityCodeService customGenericEntityCodeService;

	@Inject
	private OrderProductService orderProductService;

	@Inject
	private AdvancedSettingsService advancedSettingsService;
	

	public OrderOffer findByCodeAndQuoteVersion(String code, String orderCode) {
		if(Strings.isEmpty(code) || Strings.isEmpty(orderCode))
			throw new BusinessException("code and order code must not be empty");
		Query query=getEntityManager().createNamedQuery("OrderOffer.findByCodeAndOrderCode");
		query.setParameter("orderCode", orderCode)
			  .setParameter("code", code);
		try {
			return (OrderOffer) query.getSingleResult();
		}catch(NoResultException e ) {
			return null;
		}
	}
	
	@Override
	public void create(OrderOffer entity) throws BusinessException {
		if(Strings.isEmpty(entity.getCode())) {
			entity.setCode(customGenericEntityCodeService.getGenericEntityCode(entity));
		}
		var orderOfferExist = findByCodeAndQuoteVersion(entity.getCode(), entity.getOrder().getCode());
		if(orderOfferExist != null)
			throw new EntityAlreadyExistsException("Quote offer already exist with code : " + entity.getCode() + " and Order code : " + entity.getOrder().getCode());
		updateMrr(entity);
		super.create(entity);
	}

	@Override
	public OrderOffer update(OrderOffer entity) {
		updateMrr(entity);
		return super.update(entity);
	}

	private void updateMrr(OrderOffer entity) {
		Boolean updateMrr = (Boolean) advancedSettingsService.getParameter(AdvancedSettingsService.DISABLE_SYNC_MRR_UPDATE);
		if(updateMrr != null && updateMrr) {
			entity.getProducts().forEach(product ->product.setMrr(orderProductService.calculateMRR(product)));
			entity.getOrder().setMrr(entity.getProducts().stream().map(OrderProduct::getMrr).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
		}
	}

	public List<OrderOffer> findBySubscriptionAndStatus(String subscriptionCode, OfferLineTypeEnum offerLineType) {
		Query query=getEntityManager().createNamedQuery("OrderOffer.findByStatusAndSubscription");
		query.setParameter("subscriptionCode", subscriptionCode)
			  .setParameter("status", offerLineType);
		try {
			return (List<OrderOffer>) query.getResultList();
		}catch(NoResultException e ) {
			return null;
		}
	}
}
