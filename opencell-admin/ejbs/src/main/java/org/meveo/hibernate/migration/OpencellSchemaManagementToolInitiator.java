package org.meveo.hibernate.migration;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * Database schema management tool for Opencell setup - initiator</br>
 * 
 * Implementation based on https://docs.jboss.org/hibernate/orm/5.3/integrationguide/html_single/Hibernate_Integration_Guide.html
 * 
 * @author Andrius Karpavicius
 *
 */
public class OpencellSchemaManagementToolInitiator implements StandardServiceInitiator<SchemaManagementTool> {
    public static final OpencellSchemaManagementToolInitiator INSTANCE = new OpencellSchemaManagementToolInitiator();

    public SchemaManagementTool initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        final Object setting = configurationValues.get(AvailableSettings.SCHEMA_MANAGEMENT_TOOL);
        OpencellSchemaManagementTool tool = registry.getService(StrategySelector.class).resolveStrategy(OpencellSchemaManagementTool.class, setting);
        if (tool == null) {
            tool = new OpencellSchemaManagementTool();
        }

        return tool;
    }

    @Override
    public Class<SchemaManagementTool> getServiceInitiated() {
        return SchemaManagementTool.class;
    }
}