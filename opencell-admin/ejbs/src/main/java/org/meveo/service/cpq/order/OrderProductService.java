package org.meveo.service.cpq.order;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import org.meveo.model.cpq.commercial.OrderProduct;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;

/**
 * @author Tarik FAKHOURI
 * @version 11.0
 * @dateCreation 19/01/2021
 *
 */
@Stateless
public class OrderProductService extends PersistenceService<OrderProduct> {

	@SuppressWarnings("unchecked")
	public List<OrderProduct> findOrderProductsByOrder(Long orderId) {
		return getEntityManager().createNamedQuery("OrderProduct.findOrderProductByOrder").setParameter("commercialOrderId", orderId).getResultList();
	}
	
	public OrderProduct update(OrderProduct orderProduct) {
		// Calculate MRR
		BigDecimal mrr = calculateMRR(orderProduct);
		if (mrr != null) {
			orderProduct.setMrr(mrr);
		}
		return super.update(orderProduct);
	}

	public BigDecimal calculateMRR(OrderProduct orderProduct) {
		try {
			TypedQuery<Object> query = getEntityManager().createNamedQuery("OrderProduct.calculateMrr", Object.class);
			return (BigDecimal) query.setParameter("orderProductId", orderProduct.getId()).getSingleResult();
		} catch (NoResultException | NonUniqueResultException e) {
			log.debug("No MRR calculation found for orderProduct id: #" + orderProduct.getId(), e);
			return null;
		}
	}

}
