/**
 * 
 */
package org.meveo.service.cpq;

import org.meveo.model.cpq.OfferTemplateAttribute;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

/**
 * @author Rachid.AITYAAZZA
 *
 */

@Stateless
public class OfferTemplateAttributeService extends PersistenceService<OfferTemplateAttribute>{
	

	
	public OfferTemplateAttribute findByOfferTemplateAndAttribute(Long offerTemplateId, Long attributeVersion) {
		try{
			return  this.getEntityManager().createNamedQuery("OfferTemplateAttribute.findByAttributeAndOfferTemplate", OfferTemplateAttribute.class)
																	.setParameter("attributeId", attributeVersion)
																	.setParameter("offerTemplateId", offerTemplateId)
																	.getSingleResult();
		}catch(NoResultException e) {
			return null;
		}
	}
}