package org.meveo.hibernate.migration;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Database schema management tool for Opencell setup - controller</br>
 * 
 * Implementation based on https://docs.jboss.org/hibernate/orm/5.3/integrationguide/html_single/Hibernate_Integration_Guide.html
 * 
 * @author Andrius Karpavicius
 *
 */
public class OpencellSchemaManagementToolContributor implements ServiceContributor {

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addInitiator(OpencellSchemaManagementToolInitiator.INSTANCE);

    }
}