/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. This program is
 * not suitable for any direct or indirect application in MILITARY industry See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.meveo.cache.endpoint;

import java.io.Serializable;

/**
 * Endpoint execution result
 * 
 * @author Andrius Karpavicius
 */
public class EndpointResult implements Serializable {

    private static final long serialVersionUID = 1656541199515241533L;

    public enum EndpointResponseStatusEnum {

        /**
         * Execution was canceled
         */
        CANCELED,

        /**
         * Execution has not finished
         */
        IN_PROGRESS,

        /**
         * Timed out while waiting for execution to finish
         */
        TIMED_OUT;
    }

    /**
     * Endpoint execution result serialized to text
     */
    private String result;

    /**
     * MIME content type
     */
    private String contentType;

    /**
     * Or an exception that occurred
     */
    private Throwable error;

    /**
     * Or another response reason - like in progress, canceled, etc..
     */
    private EndpointResponseStatusEnum executionResponseStatus;

    public EndpointResult() {
    }

    /**
     * Constructor
     * 
     * @param result Endpoint execution result serialized to text
     * @param contentType MIME content type
     */
    public EndpointResult(String result, String contentType) {
        this.result = result;
        this.contentType = contentType;
    }

    /**
     * Constructor
     * 
     * @param error An exception occurred
     */
    public EndpointResult(Throwable error) {
        this.error = error;
    }

    /**
     * Constructor
     * 
     * @param executionResponseStatus another response reason - like in progress, canceled, etc..
     */
    public EndpointResult(EndpointResponseStatusEnum executionResponseStatus) {
        this.executionResponseStatus = executionResponseStatus;
    }

    /**
     * @return Endpoint execution result serialized to text
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result Endpoint execution result serialized to text
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * @return MIME content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType MIME content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return Exception that occurred
     */
    public Throwable getError() {
        return error;
    }

    /**
     * @param error Exception that occurred
     */
    public void setError(Throwable error) {
        this.error = error;
    }

    public EndpointResponseStatusEnum getExecutionResponseStatus() {
        return executionResponseStatus;
    }

    public void setExecutionResponseStatus(EndpointResponseStatusEnum executionResponseStatus) {
        this.executionResponseStatus = executionResponseStatus;
    }

    @Override
    public String toString() {
        if (result != null) {
            return "Result=" + result + ", contentType=" + contentType;
        } else if (error != null) {
            return "Error=" + error.getClass().getSimpleName() + (!(error instanceof NullPointerException) ? error.getMessage() : "");
        } else {
            return "Status=" + executionResponseStatus;
        }
    }
}