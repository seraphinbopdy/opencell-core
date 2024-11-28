package org.meveo.commons.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map.Entry;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueryBuilderTest {

    @Test
    public void addValueIsGreaterThanFieldDoubleTest_alias() {
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice a", "a");
        queryBuilder.addValueIsGreaterThanField("amount", 1., false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice a where a.amount > :a_amount");
    }

    @Test
    public void addValueIsGreaterThanFieldIntegerTest_alias() {
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice a", "a");
        queryBuilder.addValueIsGreaterThanField("rank", 3, false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice a where a.rank > :a_rank");
    }

    @Test
    public void addValueIsGreaterThanFieldDateTest_alias() {
        Date value = new Date();
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice a", "a");
        queryBuilder.addValueIsGreaterThanField("invoiceDate", value, false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice a where a.invoiceDate>=:starta_invoiceDate");

        for (Entry<String, Object> paramInfo : queryBuilder.getParams().entrySet()) {
            if (paramInfo.getKey().startsWith("starta_invoiceDate")) {
                assertTrue(value.before((Date) paramInfo.getValue()));
            }
        }
    }

    @Test
    public void addValueIsGreaterThanOrEqualFieldDoubleTest_alias() {
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice a", "a");
        queryBuilder.addValueIsGreaterThanOrEqualField("amount", 1., false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice a where a.amount >= :a_amount");
    }

    @Test
    public void addValueIsGreaterThanOrEqualFieldIntegerTest_alias() {
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice a", "a");
        queryBuilder.addValueIsGreaterThanOrEqualField("rank", 3, false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice a where a.rank >= :a_rank");
    }

    @Test
    public void addValueIsGreaterThanOrEqualFieldDateTest_alias() {
        Date value = new Date();
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice a", "a");
        queryBuilder.addValueIsGreaterThanOrEqualField("invoiceDate", value, false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice a where a.invoiceDate>=:starta_invoiceDate");
        LocalDate localDate = LocalDate.ofEpochDay(value.getDate());

        for (Entry<String, Object> paramInfo : queryBuilder.getParams().entrySet()) {
            if (paramInfo.getKey().startsWith("starta_invoiceDate")) {
                LocalDate startinvoiceDate = LocalDate.ofEpochDay(((Date) paramInfo.getValue()).getDate());
                assertEquals(localDate.compareTo(startinvoiceDate), 0);
            }
        }
    }

    @Test
    public void addValueIsGreaterThanFieldDoubleTest() {
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice");
        queryBuilder.addValueIsGreaterThanField("amount", 1., false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice where amount > :amount");
    }

    @Test
    public void addValueIsGreaterThanFieldIntegerTest() {
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice");
        queryBuilder.addValueIsGreaterThanField("rank", 3, false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice where rank > :rank");
    }

    @Test
    public void addValueIsGreaterThanFieldDateTest() {
        Date value = new Date();
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice");
        queryBuilder.addValueIsGreaterThanField("invoiceDate", value, false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice where invoiceDate>=:startinvoiceDate");

        for (Entry<String, Object> paramInfo : queryBuilder.getParams().entrySet()) {
            if (paramInfo.getKey().startsWith("startinvoiceDate")) {
                assertTrue(value.before((Date) paramInfo.getValue()));
            }
        }
    }

    @Test
    public void addValueIsGreaterThanOrEqualFieldDoubleTest() {
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice");
        queryBuilder.addValueIsGreaterThanOrEqualField("amount", 1., false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice where amount >= :amount");
    }

    @Test
    public void addValueIsGreaterThanOrEqualFieldIntegerTest() {
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice");
        queryBuilder.addValueIsGreaterThanOrEqualField("rank", 3, false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice where rank >= :rank");
    }

    @Test
    public void addValueIsGreaterThanOrEqualFieldDateTest() {
        Date value = new Date();
        QueryBuilder queryBuilder = new QueryBuilder("select * from invoice");
        queryBuilder.addValueIsGreaterThanOrEqualField("invoiceDate", value, false);
        Assertions.assertThat(queryBuilder.getSqlString()).isEqualTo("select * from invoice where invoiceDate>=:startinvoiceDate");
        LocalDate localDate = LocalDate.ofEpochDay(value.getDate());

        for (Entry<String, Object> paramInfo : queryBuilder.getParams().entrySet()) {
            if (paramInfo.getKey().startsWith("startinvoiceDate")) {
                LocalDate startinvoiceDate = LocalDate.ofEpochDay(((Date) paramInfo.getValue()).getDate());
                assertEquals(localDate.compareTo(startinvoiceDate), 0);
            }
        }
    }
}
