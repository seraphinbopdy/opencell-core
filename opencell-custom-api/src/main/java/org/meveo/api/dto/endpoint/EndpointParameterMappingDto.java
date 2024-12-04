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
package org.meveo.api.dto.endpoint;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configration of how a GET or POST parameters are mapped to the script properties.
 * 
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.10
 */
public class EndpointParameterMappingDto implements Serializable {

    private static final long serialVersionUID = -8011884518495291872L;

    /**
     * Script's parameter to map
     */
    @JsonProperty(required = true)
    private String scriptParameter;

    /**
     * Whether the parameter is multivalued
     */
    @JsonProperty(required = true)
    private Boolean multivalued;

    /**
     * Name of the parameter as exposed by the endpoint
     */
    @JsonProperty
    private String parameterName;

    /**
     * Default value to give to the parameter
     */
    @JsonProperty(required = true)
    private String defaultValue;

    /**
     * When this value is set to true, the min length of string and array objects must be set to 1 by default or they should not be null.
     */
    @JsonProperty
    protected Boolean valueRequired = false;

    /**
     * Parameter description
     */
    @JsonProperty
    private String description;

    /**
     * Parameter value examples
     */
    @JsonProperty
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

    public Boolean getMultivalued() {
        return multivalued == null ? false : multivalued;
    }

    public void setMultivalued(Boolean multivalued) {
        this.multivalued = multivalued;
    }

    /**
     * Retrieves the boolean value of whether this parameter is required.
     * 
     * @return true if this parameter is required
     */
    public Boolean getValueRequired() {
        return valueRequired;
    }

    /**
     * Sets whether this parameter is required or not.
     * 
     * @param valueRequired boolean value
     */
    public void setValueRequired(Boolean valueRequired) {
        this.valueRequired = valueRequired;
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