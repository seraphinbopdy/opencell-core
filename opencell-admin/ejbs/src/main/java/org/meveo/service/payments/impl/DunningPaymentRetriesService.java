package org.meveo.service.payments.impl;
import org.meveo.model.dunning.DunningPaymentRetry;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

/**
 * Service implementation to manage DunningPaymentRetries entity.
 * It extends {@link PersistenceService} class
 *
 * @author Mbarek-Ay
 * @version 11.0
 *
 */
@Stateless
public class DunningPaymentRetriesService extends PersistenceService<DunningPaymentRetry> {

    public DunningPaymentRetry findByPaymentMethodAndPsp(DunningPaymentRetry dunningPaymentRetry) {
        try {
            return getEntityManager().createNamedQuery("DunningPaymentRetry.findByPaymentMethodAndPsp", DunningPaymentRetry.class)
                    .setParameter("paymentMethod", dunningPaymentRetry.getPaymentMethod()).setParameter("psp", dunningPaymentRetry.getPsp()).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
