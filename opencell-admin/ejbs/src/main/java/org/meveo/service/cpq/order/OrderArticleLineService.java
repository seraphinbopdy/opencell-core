package org.meveo.service.cpq.order;

import java.util.List;

import org.meveo.model.cpq.commercial.OrderArticleLine;
import org.meveo.service.base.BusinessService;

import jakarta.ejb.Stateless;

/**
 * @author Tarik FAKHOURI
 * @version 11.0
 * @dateCreation 19/01/2021
 *
 */
@Stateless
public class OrderArticleLineService extends BusinessService<OrderArticleLine> {

	@SuppressWarnings("unchecked")
	public List<OrderArticleLine> findByOrderId(Long orderId) {
		return getEntityManager().createNamedQuery("OrderArticleLine.findByOrderId").setParameter("commercialOrderId", orderId).getResultList();
	}

}
