/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.endpoint;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

/**
 * Endpoint request query or body parameter mapping to script variables.
 *
 * @author clement.bareth
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 */
@Embeddable
public class EndpointParameterMapping {

    @Column(name = "script_parameter", length = 50)
    private String scriptParameter;

    /**
     * Exposed name of the parameter
     */
    @Column(name = "parameter_name", length = 50)
    private String parameterName;

    @Column(name = "multivalued", nullable = false, columnDefinition = "int default 0")
    private int multivalued;

    /**
     * Default value of the parameter
     */
    @Column(name = "default_value", length = 255)
    private String defaultValue;

    /**
     * When this value is set to true, the min length of string and array objects must be set to 1 by default or they should not be null.
     */
    @Column(name = "value_required", nullable = false, columnDefinition = "int default 0")
    private int valueRequired;

    /**
     * Parameter description
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Parameter value example
     */
    @Column(name = "example", length = 500)
    private String example;

    public String getScriptParameter() {
        return scriptParameter;
    }

    public void setScriptParameter(String scriptParameter) {
        this.scriptParameter = scriptParameter;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getMultivalued() {
        return multivalued;
    }

    public void setMultivalued(int multivalued) {
        this.multivalued = multivalued;
    }

    /**
     * Retrieves the boolean value of whether this parameter is multivalued.
     * 
     * @return True if this parameter is multivalued
     */
    @Transient
    public boolean isMultivaluedAsBoolean() {
        return multivalued == 1;
    }

    /**
     * Sets whether this parameter is multivalued or not.
     * 
     * @param multivalued True if parameter is multivalued
     */
    @Transient
    public void setMultivaluedAsBoolean(boolean multivalued) {
        this.multivalued = multivalued ? 1 : 0;
    }

    /**
     * Retrieves the boolean value of whether this parameter is required.
     * 
     * @return true if this parameter is required
     */
    public int getValueRequired() {
        return valueRequired;
    }

    /**
     * Sets whether this parameter is required or not.
     * 
     * @param valueRequired boolean value
     */
    public void setValueRequired(int valueRequired) {
        this.valueRequired = valueRequired;
    }

    /**
     * Retrieves the boolean value of whether this parameter is required.
     * 
     * @return True if this parameter is required
     */
    @Transient
    public boolean isValueRequiredAsBoolean() {
        return this.valueRequired == 1;
    }

    /**
     * Sets whether this parameter is required or not.
     * 
     * @param value Boolean value
     */
    @Transient
    public void setValueRequiredAsBoolean(boolean value) {
        this.valueRequired = value ? 1 : 0;
    }

    /**
     * @return Parameter description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description Parameter description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Parameter value example
     */
    public String getExample() {
        return example;
    }

    /**
     * @param example Parameter value example
     */
    public void setExample(String example) {
        this.example = example;
    }
}