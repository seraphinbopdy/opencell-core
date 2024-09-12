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

import org.meveo.model.billing.BankCoordinates;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * Payment by wire transfer method
 * 
 * @author Andrius Karpavicius
 */
@Entity
@DiscriminatorValue(value = "WIRETRANSFER")
public class WirePaymentMethod extends PaymentMethod {

    private static final long serialVersionUID = 8726571628074346184L;

    /**
     * Bank information
     */
    @Embedded
    private BankCoordinates bankCoordinates = new BankCoordinates();

    /**
     * Order identification
     */
    @Column(name = "mandate_identification", length = 255)
    @Size(max = 255)
    private String mandateIdentification = "";

    /**
     * Order date
     */
    @Column(name = "mandate_date")
    @Temporal(TemporalType.DATE)
    private Date mandateDate;

    public WirePaymentMethod() {
        this.paymentType = PaymentMethodEnum.WIRETRANSFER;
    }

    public WirePaymentMethod(boolean isDisabled, String alias, boolean preferred, CustomerAccount customerAccount) {
        super();
        this.paymentType = PaymentMethodEnum.WIRETRANSFER;
        this.alias = alias;
        this.preferred = preferred;
        this.customerAccount = customerAccount;
        setDisabled(isDisabled);
    }

    public WirePaymentMethod(String alias, boolean preferred) {
        super();
        this.alias = alias;
        this.preferred = preferred;
    }

    public WirePaymentMethod(CustomerAccount customerAccount, boolean isDisabled, String alias, boolean preferred, Date mandateDate, String mandateIdentification, BankCoordinates bankCoordinates) {
        this(isDisabled, alias, preferred, customerAccount);
        this.mandateDate = mandateDate;
        this.mandateIdentification = mandateIdentification;
        setBankCoordinates(bankCoordinates);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public void updateWith(PaymentMethod paymentMethod) {

        setAlias(paymentMethod.getAlias());
        setPreferred(paymentMethod.isPreferred());
    }

    @Override
    public String toString() {
        return "WirePaymentMethod [alias= " + getAlias() + ", preferred=" + isPreferred() + "]";
    }

    public BankCoordinates getBankCoordinates() {
        return bankCoordinates;
    }

    public void setBankCoordinates(BankCoordinates bankCoordinates) {
        this.bankCoordinates = bankCoordinates;
    }

    public @Size(max = 255) String getMandateIdentification() {
        return mandateIdentification;
    }

    public void setMandateIdentification(@Size(max = 255) String mandateIdentification) {
        this.mandateIdentification = mandateIdentification;
    }

    public Date getMandateDate() {
        return mandateDate;
    }

    public void setMandateDate(Date mandateDate) {
        this.mandateDate = mandateDate;
    }
}