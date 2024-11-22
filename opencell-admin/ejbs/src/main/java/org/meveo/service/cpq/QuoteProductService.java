package org.meveo.service.cpq;

import java.math.BigDecimal;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.meveo.model.quote.QuoteProduct;
import org.meveo.service.base.PersistenceService;


/**
 * @author Khairi
 * @version 10.0
 */
@Stateless
public class QuoteProductService extends PersistenceService<QuoteProduct> { 
	
	public QuoteProduct addNewQuoteProduct(QuoteProduct quoteProduct){
		this.create(quoteProduct);
		return quoteProduct;
	}
	
	public QuoteProduct findByQuoteAndOfferAndProduct(Long quoteVersionId, String quoteOfferCode,String productCode) {
		try {
			return (QuoteProduct) this.getEntityManager().createNamedQuery("QuoteProduct.findByQuoteVersionAndQuoteOffer")
														.setParameter("quoteVersionId", quoteVersionId)
														.setParameter("quoteOfferCode", quoteOfferCode)
														.setParameter("productCode",productCode)
															.getSingleResult();
		}catch(NoResultException e ) {
			log.warn("cant find QuoteProduct with  quote version: {} and product version : {}", quoteVersionId, quoteOfferCode);
			return null;
		}
	}
	public QuoteProduct findByQuoteAndOfferAndProductAndQuantity(Long quoteVersionId, String quoteOfferCode,String productCode, BigDecimal quantity) {
		try {
			return (QuoteProduct) this.getEntityManager().createNamedQuery("QuoteProduct.findByQuoteVersionAndQuoteOfferAndQuantity")
					.setParameter("quoteVersionId", quoteVersionId)
					.setParameter("quoteOfferCode", quoteOfferCode)
					.setParameter("productCode",productCode)
					.setParameter("quantity", quantity)
					.getSingleResult();
		}catch(NoResultException e ) {
			log.warn("cant find QuoteProduct with  quote version: {} and product version : {}", quoteVersionId, quoteOfferCode);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<QuoteProduct> findByQuoteVersion(Long quoteVersionId) {
		Query query = this.getEntityManager().createNamedQuery("QuoteProduct.findByQuoteVersionId").setParameter("id", quoteVersionId);
		return query.getResultList();
	}

	public QuoteProduct update(QuoteProduct quoteProduct) {
		BigDecimal qpMRR = calculateMRR(quoteProduct);
		log.info("QuoteProduct {} MRR: {}", quoteProduct.getId(), qpMRR);
		// quoteProduct.setMrr(qpMRR);
		return super.update(quoteProduct);
	}

	public BigDecimal calculateMRR(QuoteProduct quoteProduct) {
        try {
			TypedQuery<Object> query = getEntityManager().createNamedQuery("QuoteProduct.calculateMrr", Object.class);
			return (BigDecimal) query.setParameter("quoteProductId", quoteProduct.getId()).getSingleResult();
        } catch (NoResultException | NonUniqueResultException e) {
			log.debug("No MRR calculation found for quoteProduct id: #" + quoteProduct.getId(), e);
            return null;
        }
    }

}
