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
package org.meveo.model.billing;

/**
 * Invoice Payment status.
 */
public enum InvoicePaymentStatusEnum {

    /**
     * invoice has no payment status, no AO created.
     */
    NONE(1, "invoicePaymentStatusEnum.none"),

    /**
     * AO created, due date is still in the future
     */
    PENDING(2, "invoicePaymentStatusEnum.pending"),

    /**
     * PaymentPlan created for DEBUT AO
     */
    PENDING_PLAN(3, "invoicePaymentStatusEnum.pending_plan"),

    /**
     * invoice fully paid
     */
    PAID(4, "invoicePaymentStatusEnum.paid"),

    /**
     * invoice partially paid
     */
    PPAID(5, "invoicePaymentStatusEnum.pPaid"),

    /**
     * invoice has not yet paid, due date is in the past
     */
    UNPAID(6, "invoicePaymentStatusEnum.unPaid"),

    /**
     * invoice abandoned
     */
    ABANDONED(7, "invoicePaymentStatusEnum.abandoned"),

    /**
     * invoice disputed, litigation case
     */
    DISPUTED(8, "invoicePaymentStatusEnum.disputed"),

    /**
     * credit note partially completely refunded.
     */
    REFUNDED(9, "invoicePaymentStatusEnum.refunded"),

    /**
     * credit note partially refunded.
     */
    PREFUNDED(10, "invoicePaymentStatusEnum.prefunded"),

    /**
     * credit note not yet refunded. due date is still in the future.
     */
    UNREFUNDED(11, "invoicePaymentStatusEnum.unrefunded");

    private Integer id;
    private String label;

    InvoicePaymentStatusEnum(Integer id, String label) {
        this.id = id;
        this.label = label;

    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Gets enum by its id.
     * 
     * @param id of invoice payment status
     * @return invoice payment status enum
     */
    public static InvoicePaymentStatusEnum getValue(Integer id) {
        if (id != null) {
            for (InvoicePaymentStatusEnum status : values()) {
                if (id.equals(status.getId())) {
                    return status;
                }
            }
        }
        return null;
    }
}
