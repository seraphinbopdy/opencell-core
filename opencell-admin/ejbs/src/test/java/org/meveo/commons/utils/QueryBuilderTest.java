package org.meveo.commons.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueryBuilderTest {
    private QueryBuilder queryBuilder = new QueryBuilder("select * from invoice");

    @Test
    public void addValueIsGreaterThanFieldDoubleTest() {
        QueryBuilder queryBuilder = this.queryBuilder.addValueIsGreaterThanField("amount", 1., false);
        assertEquals("select * from invoice where amount > :amount", queryBuilder.getSqlString());
    }
    @Test
    public void addValueIsGreaterThanFieldIntegerTest() {
        QueryBuilder queryBuilder = this.queryBuilder.addValueIsGreaterThanField("rank", 3, false);
        assertEquals("select * from invoice where rank > :rank", queryBuilder.getSqlString());
    }

    @Test
    public void addValueIsGreaterThanFieldDateTest() {
        Date value = new Date();
        QueryBuilder queryBuilder = this.queryBuilder.addValueIsGreaterThanField("invoiceDate", value, false);
        assertEquals("select * from invoice where invoiceDate>=:startinvoiceDate", queryBuilder.getSqlString());
        assertTrue(value.before((Date) queryBuilder.getParams().get("startinvoiceDate")));
    }

    @Test
    public void addValueIsGreaterThanOrEqualFieldDoubleTest() {
        QueryBuilder queryBuilder = this.queryBuilder.addValueIsGreaterThanOrEqualField("amount", 1., false);
        assertEquals("select * from invoice where amount >= :amount", queryBuilder.getSqlString());
    }
    @Test
    public void addValueIsGreaterThanOrEqualFieldIntegerTest() {
        QueryBuilder queryBuilder = this.queryBuilder.addValueIsGreaterThanOrEqualField("rank", 3, false);
        assertEquals("select * from invoice where rank >= :rank", queryBuilder.getSqlString());
    }
    @Test
    public void addValueIsGreaterThanOrEqualFieldDateTest() {
        Date value = new Date();
        QueryBuilder queryBuilder = this.queryBuilder.addValueIsGreaterThanOrEqualField("invoiceDate", value, false);
        assertEquals("select * from invoice where invoiceDate>=:startinvoiceDate", queryBuilder.getSqlString());
        LocalDate localDate = LocalDate.ofEpochDay(value.getDate());
        LocalDate startinvoiceDate = LocalDate.ofEpochDay(((Date) queryBuilder.getParams().get("startinvoiceDate")).getDate());
        assertEquals(localDate.compareTo(startinvoiceDate), 0);
    }
}
