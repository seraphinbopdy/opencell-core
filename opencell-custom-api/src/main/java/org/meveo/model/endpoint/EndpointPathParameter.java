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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Endpoint request path parameter mapping to script variables.
 *
 * @author clement.bareth
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 */
@Embeddable
public class EndpointPathParameter {

    @Column(name = "script_parameter", length = 50)
    private String scriptParameter;

    /**
     * Position of the parameter the endpoint's path parameter list. This column is used only for JPA to build list in right order.
     */
    @Column(name = "position", nullable = false, columnDefinition = "int default 0")
    private int position = 0;

    @Override
    public String toString() {
        return scriptParameter;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getScriptParameter() {
        return scriptParameter;
    }

    public void setScriptParameter(String scriptParameter) {
        this.scriptParameter = scriptParameter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointPathParameter that = (EndpointPathParameter) o;
        return scriptParameter.equals(that.getScriptParameter());
    }

    @Override
    public int hashCode() {
        return Objects.hash(scriptParameter, getPosition());
    }
}