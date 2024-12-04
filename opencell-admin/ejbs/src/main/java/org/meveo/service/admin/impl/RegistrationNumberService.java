package org.meveo.service.admin.impl;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.meveo.model.RegistrationNumber;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

@Stateless
public class RegistrationNumberService extends PersistenceService<RegistrationNumber> {

	public RegistrationNumber findByRegistrationNo(String registrationNo) {
		try{
			List<RegistrationNumber> result =  getEntityManager().createQuery("from RegistrationNumber r where lower(r.registrationNo)= lower(:registrationNo) ORDER BY r.registrationNo ASC").setMaxResults(1)
					.setParameter("registrationNo", registrationNo).getResultList();
			return CollectionUtils.isNotEmpty(result) ? result.get(0) : null;
		}catch (NoResultException e) {
			log.info("No class found for registration number : {}", registrationNo);
			return null;
		}
	}
}
