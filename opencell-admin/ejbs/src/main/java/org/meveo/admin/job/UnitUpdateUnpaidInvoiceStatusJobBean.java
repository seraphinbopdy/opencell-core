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

package org.meveo.admin.job;

import static org.meveo.model.billing.InvoicePaymentStatusEnum.UNPAID;
import static org.meveo.model.billing.InvoicePaymentStatusEnum.UNREFUNDED;

import java.util.Date;

import org.meveo.event.qualifier.Updated;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.BaseEntity;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoicePaymentStatusEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.service.billing.impl.InvoiceService;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * @author Mounir BOUKAYOUA
 * @lastModifiedVersion 7.0
 */
@Stateless
public class UnitUpdateUnpaidInvoiceStatusJobBean {

    @Inject
    private Logger log;

    @Inject
    private InvoiceService invoiceService;

    @Inject
    @Updated
    private Event<BaseEntity> entityUpdatedEventProducer;

    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl result, Long unpaidInvoiceId) {
        log.debug("update Invoice[id={}] status to unpaid or unrefunded", unpaidInvoiceId);
        try {
            Invoice invoice = invoiceService.findById(unpaidInvoiceId);
            InvoicePaymentStatusEnum newPaymentStatus = (invoice.getInvoiceType().getCode().contains("ADJ")) ? UNREFUNDED : UNPAID;
            log.info("[Inv.id : " + invoice.getId() + " - oldPaymentStatus : " + invoice.getPaymentStatus() + " - newPaymentStatus : " + newPaymentStatus + "]");
            invoiceService.checkAndUpdatePaymentStatus(invoice, invoice.getPaymentStatus(), newPaymentStatus);
            invoice.setPaymentStatusDate(new Date());
            invoice = invoiceService.updateNoCheck(invoice);
            entityUpdatedEventProducer.fire(invoice);

            result.registerSucces();

        } catch (Exception e) {
            log.error("Failed to update Invoice[id={}] status to unpaid or unrefunded", unpaidInvoiceId, e);
            result.registerError(e.getMessage());
        }
    }
}
