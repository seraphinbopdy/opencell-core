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
package org.meveo.model.payments;

/**
 * Payment Enum for Payment Status.
 * 
 * @author anasseh
 * @lastModifiedVersion 5.0
 */
public enum PaymentStatusEnum {

    ACCEPTED(1, "PaymentStatusEnum.ACCEPTED"), 
    PENDING(2, "PaymentStatusEnum.PENDING"), 
    REJECTED(3, "PaymentStatusEnum.REJECTED"), 
    ERROR(4, "PaymentStatusEnum.ERROR"),  
    NOT_PROCESSED(5, "PaymentStatusEnum.NOT_PROCESSED");
	
	 private Integer id;
	    private String label;

	    PaymentStatusEnum(Integer id, String label) {
	        this.label = label;
	        this.id = id;
	    }

	    public String getLabel() {
	        return this.label;
	    }

	    public Integer getId() {
	        return this.id;
	    }

	    public static PaymentStatusEnum getValue(Integer id) {
	        if (id != null) {
	            for (PaymentStatusEnum status : values()) {
	                if (id.equals(status.getId())) {
	                    return status;
	                }
	            }
	        }
	        return null;
	    }
}