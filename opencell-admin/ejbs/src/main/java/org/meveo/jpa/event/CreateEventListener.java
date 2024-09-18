/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.jpa.event;

import org.hibernate.HibernateException;
import org.hibernate.event.internal.DefaultPersistEventListener;
import org.hibernate.event.spi.PersistEvent;
import org.meveo.commons.utils.EjbUtils;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.BusinessEntity;
import org.meveo.service.admin.impl.CustomGenericEntityCodeService;
import org.meveo.service.admin.impl.SequenceService;
import org.meveo.service.base.BusinessService;

/**
 * JPA Persist event listener. Auto generate and customize business entity code.
 *
 * @author Abdellatif BARI.
 * @since 7.0
 */
public class CreateEventListener extends DefaultPersistEventListener {

	private static SequenceService sequenceService = null;
	
	static {
		sequenceService = (SequenceService) EjbUtils.getServiceInterface("SequenceService");
	}
    @Override
    public void onPersist(PersistEvent event) throws HibernateException {
        super.onPersist(event);
        final Object entity = event.getObject();
        if (entity instanceof BusinessEntity) {
            try {
                BusinessEntity businessEntity = (BusinessEntity) entity;
                if (StringUtils.isBlank(businessEntity.getCode())) {
                    CustomGenericEntityCodeService customGenericEntityCodeService = (CustomGenericEntityCodeService) EjbUtils.getServiceInterface("CustomGenericEntityCodeService");
	                businessEntity.setCode(checkExistingCodeonDB(businessEntity, customGenericEntityCodeService, 0));
                }
            } catch (Exception e) {
                throw new HibernateException(e);
            }
        }
    }
	
	/**
	 * create method that take code as parameter and the  business entity
	 * and check if it exists in the database if so increment it
 	 */
	private String checkExistingCodeonDB(BusinessEntity entity, CustomGenericEntityCodeService customGenericEntityCodeService, int incrementCodeValue) {
		BusinessService businessEntityService = (BusinessService) EjbUtils.getServiceInterface(entity.getClass());
		String code = customGenericEntityCodeService.getGenericEntityCode(entity);
		if(incrementCodeValue > 0) {
			code = code + "_" + incrementCodeValue;
		}
		if (businessEntityService.findByCode(code) != null) {
			return checkExistingCodeonDB(entity, customGenericEntityCodeService, ++incrementCodeValue);
		}
		return code;
		
		
	}
	

}