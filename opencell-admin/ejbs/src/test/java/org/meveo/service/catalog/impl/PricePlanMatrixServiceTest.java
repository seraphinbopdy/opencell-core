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

package org.meveo.service.catalog.impl;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class PricePlanMatrixServiceTest {

    @Spy
    @InjectMocks
    private PricePlanMatrixService pricePlanMatrixService;

    @Test
    public void create_test() throws ParseException {
        RecurringChargeTemplate recurringChargeTemplate = new RecurringChargeTemplate();
        recurringChargeTemplate.setCode("REC_CODE");

        recurringChargeTemplate.setProrataOnPriceChange(true);

        PricePlanMatrix pricePlanMatrix1 = new PricePlanMatrix();
        pricePlanMatrix1.setId(1L);
        pricePlanMatrix1.setValidityDate(new SimpleDateFormat("yyyy-MM-dd").parse("2018-01-01"));
        pricePlanMatrix1.setValidityFrom(new SimpleDateFormat("yyyy-MM-dd").parse("2018-01-01"));

        PricePlanMatrix pricePlanMatrix2 = new PricePlanMatrix();
        pricePlanMatrix2.setId(2L);
        pricePlanMatrix2.setValidityFrom(new SimpleDateFormat("yyyy-MM-dd").parse("2018-01-01"));
        pricePlanMatrix2.setValidityDate(new SimpleDateFormat("yyyy-MM-dd").parse("2019-01-01"));

        PricePlanMatrix pricePlanMatrix3 = spy(new PricePlanMatrix());
        pricePlanMatrix3.setId(3L);
        pricePlanMatrix3.setValidityFrom(new SimpleDateFormat("yyyy-MM-dd").parse("2019-01-01"));
        pricePlanMatrix3.setValidityDate(new SimpleDateFormat("yyyy-MM-dd").parse("2019-01-01"));
        pricePlanMatrix3.setChargeTemplates(Set.of(recurringChargeTemplate));

        doNothing().when(pricePlanMatrixService).create(pricePlanMatrix3);
        doReturn(Arrays.asList(pricePlanMatrix1, pricePlanMatrix2)).when(pricePlanMatrixService).listByChargeCode("REC_CODE");

        // Use the form "doAnswer().when().method" instead of "when().thenAnswer()" because on spied object the later will call a real method at the setup time, which will fail because of null values being passed.
        doAnswer(new Answer<PricePlanMatrix>() {
            public PricePlanMatrix answer(InvocationOnMock invocation) throws Throwable {
                return pricePlanMatrix3;
            }
        }).when(pricePlanMatrixService).findById(eq(3L));

        pricePlanMatrixService.createPP(pricePlanMatrix3);
    }

    @Test(expected = BusinessException.class)
    public void create_price_plan_overlapped_validity_date_test() throws ParseException {
        RecurringChargeTemplate recurringChargeTemplate = new RecurringChargeTemplate();
        recurringChargeTemplate.setCode("REC_CODE");

        recurringChargeTemplate.setProrataOnPriceChange(true);

        PricePlanMatrix pricePlanMatrix1 = new PricePlanMatrix();
        pricePlanMatrix1.setId(1L);
        pricePlanMatrix1.setValidityFrom(new SimpleDateFormat("yyyy-MM-dd").parse("2017-01-01"));
        pricePlanMatrix1.setValidityDate(new SimpleDateFormat("yyyy-MM-dd").parse("2018-01-01"));

        PricePlanMatrix pricePlanMatrix2 = new PricePlanMatrix();
        pricePlanMatrix2.setId(2L);
        pricePlanMatrix2.setValidityFrom(new SimpleDateFormat("yyyy-MM-dd").parse("2018-01-01"));
        pricePlanMatrix2.setValidityDate(new SimpleDateFormat("yyyy-MM-dd").parse("2019-01-01"));

        PricePlanMatrix pricePlanMatrix3 = new PricePlanMatrix();
        pricePlanMatrix3.setId(3L);
        pricePlanMatrix3.setValidityFrom(new SimpleDateFormat("yyyy-MM-dd").parse("2018-07-01"));
        pricePlanMatrix3.setValidityDate(new SimpleDateFormat("yyyy-MM-dd").parse("2019-01-01"));
        pricePlanMatrix3.setChargeTemplates(Set.of(recurringChargeTemplate));

        doReturn(Arrays.asList(pricePlanMatrix1, pricePlanMatrix2)).when(pricePlanMatrixService).listByChargeCode("REC_CODE");

        // Use the form "doAnswer().when().method" instead of "when().thenAnswer()" because on spied object the later will call a real method at the setup time, which will fail because of null values being passed.
        doAnswer(new Answer<PricePlanMatrix>() {
            public PricePlanMatrix answer(InvocationOnMock invocation) throws Throwable {
                return pricePlanMatrix3;
            }
        }).when(pricePlanMatrixService).findById(eq(3L));

        pricePlanMatrixService.createPP(pricePlanMatrix3);
    }
}