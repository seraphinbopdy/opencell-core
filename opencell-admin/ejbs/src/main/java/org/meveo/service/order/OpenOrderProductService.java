package org.meveo.service.order;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.model.ordering.OpenOrderProduct;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;

@Stateless
public class OpenOrderProductService extends PersistenceService<OpenOrderProduct> {

    public OpenOrderProduct findByProductCodeAndTemplate(String code, Long idTemplate) {
        List<OpenOrderProduct> oops = getEntityManager().createNamedQuery("OpenOrderProduct.findByCodeAndTemplate")
                .setParameter("TEMPLATE_ID", idTemplate)
                .setParameter("PRODUCT_CODE", code)
                .getResultList();

        return CollectionUtils.isEmpty(oops) ? null : oops.get(0);
    }

}