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

package org.meveo.model.audit;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Configuration for data auditing
 * 
 * @author Andrius Karpavicius
 **/
@Entity
@Table(name = "audit_data_config")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "audit_data_config_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "AuditDataConfiguration.findByEntityClass", query = "SELECT adc FROM AuditDataConfiguration adc where adc.entityClass=:entityClass"),
        @NamedQuery(name = "AuditDataConfiguration.getEntityClasses", query = "SELECT adc.entityClass FROM AuditDataConfiguration adc", hints = { @QueryHint(name = "org.hibernate.cacheable", value = "TRUE") }) })
@NamedStoredProcedureQueries({
        @NamedStoredProcedureQuery(name = "AuditDataConfiguration.recreateDataAuditTrigger", procedureName = "proc_recreate_audit_data_trigger", parameters = {
                @StoredProcedureParameter(name = "tableName", type = String.class), @StoredProcedureParameter(name = "fields", type = String.class), @StoredProcedureParameter(name = "actions", type = String.class),
                @StoredProcedureParameter(name = "preserveField", type = String.class), @StoredProcedureParameter(name = "saveEvenDiffIsEmpty", type = boolean.class) }),
        @NamedStoredProcedureQuery(name = "AuditDataConfiguration.deleteDataAuditTrigger", procedureName = "proc_delete_audit_data_trigger", parameters = {
                @StoredProcedureParameter(name = "tableName", type = String.class) }) })
public class AuditDataConfiguration extends AuditableEntity {

    private static final long serialVersionUID = -8920671560100707762L;

    /**
     * Entity type/class to track. A full class name.
     */
    @Column(name = "entity_class", length = 255)
    @Size(max = 255)
    @NotNull
    private String entityClass;

    /**
     * Entity fields to track - a comma separated list of field names
     */
    @Column(name = "entity_fields", length = 2000)
    private String fields;

    /**
     * Actions to track - A comma separated list of actions: INSERT, UPDATE, DELETE - {@link AuditCrudActionEnum}
     */
    @Column(name = "actions", length = 15)
    private String actions;

    /**
     * Constructor
     */
    public AuditDataConfiguration() {
        super();
    }

    /**
     * Constructor
     * 
     * @param entityClass Entity type/class to track
     * @param fields Entity fields to track
     * @param actions Actions to track - A comma separated list of actions: INSERT, UPDATE, DELETE - {@link AuditCrudActionEnum}
     */
    public AuditDataConfiguration(String entityClass, String fields, String actions) {
        super();
        this.entityClass = entityClass;
        this.fields = fields;
        this.actions = actions;
    }

    /**
     * @return Entity type/class to track. A full class name.
     */
    public String getEntityClass() {
        return entityClass;
    }

    /**
     * @param entityClass Entity type/class to track. A full class name.
     */
    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * @return Entity fields to track - a comma separated list of field names
     */
    public String getFields() {
        return fields;
    }

    /**
     * @param fields Entity fields to track - a comma separated list of field names
     */
    public void setFields(String fields) {
        this.fields = fields;
    }

    /**
     * @return Actions to track - A comma separated list of actions: INSERT, UPDATE, DELETE
     */
    public String getActions() {
        return actions;
    }

    /**
     * @param actions Actions to track - A comma separated list of actions: INSERT, UPDATE, DELETE
     */
    public void setActions(String actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "AuditDataConfiguration [entityClass=" + entityClass + ", fields=" + fields + ", actions=" + actions + "]";
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof AuditDataConfiguration)) { // Fails with proxed objects: getClass() != obj.getClass()){
            return false;
        }

        AuditDataConfiguration other = (AuditDataConfiguration) obj;

        if (id != null && other.getId() != null && id.equals(other.getId())) {
            return true;
        }

        if (entityClass == null) {
            if (other.getEntityClass() != null) {
                return false;
            }
        } else if (!entityClass.equals(other.getEntityClass())) {
            return false;
        }
        return true;
    }
}