package org.meveo.commons.utils;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.response.PagingAndFiltering.SortOrder;
import org.meveo.model.billing.Invoice;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.persistence.criteria.JoinType;

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

    @Test
    public void join_has_name_and_alias() {
        InnerJoin innerJoin = new InnerJoin("ab", 0);
        assertThat(innerJoin.getName()).isEqualTo("ab");
        assertThat(innerJoin.getAlias()).startsWith("ab_");
    }

    @Test
    public void join_may_point_to_list_of_joins() {
        InnerJoin innerJoin = new InnerJoin("ab", 0);
        InnerJoin acInnerJoin = new InnerJoin("ac", 1);
        innerJoin.next(acInnerJoin);
        InnerJoin aeInnerJoin = new InnerJoin("ae", 2);
        innerJoin.next(aeInnerJoin);

        assertThat(innerJoin.getNextInnerJoins()).containsExactly(acInnerJoin, aeInnerJoin);
    }

    @Test
    public void can_format_one_join() {
        InnerJoin innerJoin = new InnerJoin("ab", 1);
        QueryBuilder queryBuilder = new QueryBuilder(Invoice.class, "I", List.of());
        String joinString = queryBuilder.format("", innerJoin, false);

        assertThat(joinString).isEqualTo(format("left join ab %s ", innerJoin.getAlias()));
    }

    @Test
    public void query_builder_can_format_joins() {

        InnerJoin abInnerJoin = new InnerJoin("ab", 0);
        InnerJoin bcInnerJoin = new InnerJoin("bc", 1);
        abInnerJoin.next(bcInnerJoin);

        QueryBuilder queryBuilder = new QueryBuilder(Invoice.class, "I", List.of());
        String joinString = queryBuilder.format("", abInnerJoin, false);

        assertThat(joinString).isEqualTo(format("left join ab %s left join %s.bc %s", abInnerJoin.getAlias(), abInnerJoin.getAlias(), bcInnerJoin.getAlias()));
    }

    @Test
    public void joins_may_have_n_deep() {
        InnerJoin abInnerJoin = new InnerJoin("ab", 0);
        InnerJoin acInnerJoin = new InnerJoin("ac", 1);
        InnerJoin adInnerJoin = new InnerJoin("ad", 2);
        abInnerJoin.next(acInnerJoin);
        acInnerJoin.next(adInnerJoin);

        QueryBuilder queryBuilder = new QueryBuilder(Invoice.class, "I", List.of());
        String joinString = queryBuilder.format("", abInnerJoin, false);

        assertThat(joinString)
            .isEqualTo(format("left join ab %s left join %s.ac %s left join %s.ad %s", abInnerJoin.getAlias(), abInnerJoin.getAlias(), acInnerJoin.getAlias(), acInnerJoin.getAlias(), adInnerJoin.getAlias()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_could_not_parse_one_field() {

        QueryBuilder queryBuilder = new QueryBuilder(Invoice.class, "I", List.of());
        JoinWrapper joinWrapper = queryBuilder.parse("a");
        InnerJoin rootJoin = joinWrapper.getRootInnerJoin();

        assertThat(rootJoin.getName()).isEqualTo("a");
        assertThat(rootJoin.getNextInnerJoins()).isEmpty();

        assertThat(joinWrapper.getJoinAlias()).isEqualTo(rootJoin.getAlias());

    }

    @Test
    public void builder_can_parse_two_fields() {

        QueryBuilder queryBuilder = new QueryBuilder(Invoice.class, "I", List.of());
        JoinWrapper joinWrapper = queryBuilder.parse("a.b");
        InnerJoin rootJoin = joinWrapper.getRootInnerJoin();

        assertThat(rootJoin.getName()).isEqualTo("a");
        assertThat(rootJoin.getNextInnerJoins()).isEmpty();

        assertThat(joinWrapper.getJoinAlias()).isEqualTo(rootJoin.getAlias() + ".b");

    }

    @Test
    public void builder_can_parse_fields() {

        QueryBuilder queryBuilder = new QueryBuilder(Invoice.class, "I", List.of());
        JoinWrapper joinWrapper = queryBuilder.parse("a.b.c");
        InnerJoin rootJoin = joinWrapper.getRootInnerJoin();

        assertThat(rootJoin.getName()).isEqualTo("a");
        assertThat(rootJoin.getNextInnerJoins()).hasSize(1);
        assertThat(rootJoin.getNextInnerJoins().get(0).getName()).isEqualTo("b");
        assertThat(rootJoin.getNextInnerJoins().get(0).getNextInnerJoins()).isEmpty();

        assertThat(joinWrapper.getJoinAlias()).isEqualTo(rootJoin.getNextInnerJoins().get(0).getAlias() + ".c");

    }

    @Test
    public void field_name_migration_in_pagination_config() {

        List<String> fetchFields = List.of("field10", "cfValues", "varcharFromJson('crm_customer.cfValues','customPrices20','string','String','0')",
            "longFromJson('crm_customer_account.cfValuesAsJson','customNumbers20')", "customerSubCategory.cfValues", "billingAccount.customerAccount.cfValuesAsJson", "field20");

        Set<String> groupBy = Set.of("field1", "cfValues", "varcharFromJson('crm_customer.cfValues','customPrices','string','String','0')", "longFromJson('crm_customer_account.cfValuesAsJson','customNumbers')",
            "customerCategory.cfValues", "billingAccount.cfValuesAsJson", "field2");

        Set<String> having = Set.of("field5", "cfValuesAsJson", "varcharFromJson('crm_customer.cfValues','customPrices','string','String','0')");

        Map<String, Object> filters = Map.of("field1", "value1", "cfValues", "value2", "varcharFromJson('crm_customer.cfValues','customPrices','string','String','0')", "value3",
            "longFromJson('crm_customer_account.cfValuesAsJson','customNumbers')", "value4", "customerCategory.cfValues", "value5", "billingAccount.cfValuesAsJson", "value6", "field2", "value7");

        PaginationConfiguration paginationConfiguration = new PaginationConfiguration(filters);
        String[] filterMigratedFields = paginationConfiguration.getFilters().keySet().toArray(new String[] {});
        Arrays.sort(filterMigratedFields);

        assertThat(StringUtils.concatenate(",", filterMigratedFields)).isEqualTo(
            "billingAccount.cfValuesAsJson,cfValuesAsJson,customerCategory.cfValuesAsJson,field1,field2,longFromJson('crm_customer_account.cfValuesAsJson','customNumbers'),varcharFromJson('crm_customer.cfValuesAsJson','customPrices','string','String','0')");

        paginationConfiguration = new PaginationConfiguration(1, 15, filters, null, fetchFields, groupBy, having, JoinType.INNER, true, false, "id", SortOrder.ASCENDING, "cfValues", SortOrder.DESCENDING,
            "left(cfValues,5)", SortOrder.ASCENDING, "cfValuesAsJson", SortOrder.ASCENDING);

        filterMigratedFields = paginationConfiguration.getFilters().keySet().toArray(new String[] {});
        Arrays.sort(filterMigratedFields);

        assertThat(StringUtils.concatenate(",", filterMigratedFields)).isEqualTo(
            "billingAccount.cfValuesAsJson,cfValuesAsJson,customerCategory.cfValuesAsJson,field1,field2,longFromJson('crm_customer_account.cfValuesAsJson','customNumbers'),varcharFromJson('crm_customer.cfValuesAsJson','customPrices','string','String','0')");

        String[] fetchMigratedFields = paginationConfiguration.getFetchFields().toArray(new String[] {});
        Arrays.sort(fetchMigratedFields);
        assertThat(StringUtils.concatenate(",", fetchMigratedFields)).isEqualTo(
            "billingAccount.customerAccount.cfValuesAsJson,cfValuesAsJson,customerSubCategory.cfValuesAsJson,field10,field20,longFromJson('crm_customer_account.cfValuesAsJson','customNumbers20'),varcharFromJson('crm_customer.cfValuesAsJson','customPrices20','string','String','0')");

        String[] groupByMigratedFields = paginationConfiguration.getGroupBy().toArray(new String[] {});
        Arrays.sort(groupByMigratedFields);
        assertThat(StringUtils.concatenate(",", groupByMigratedFields)).isEqualTo(
            "billingAccount.cfValuesAsJson,cfValuesAsJson,customerCategory.cfValuesAsJson,field1,field2,longFromJson('crm_customer_account.cfValuesAsJson','customNumbers'),varcharFromJson('crm_customer.cfValuesAsJson','customPrices','string','String','0')");

        String[] havingMigratedFields = paginationConfiguration.getHaving().toArray(new String[] {});
        Arrays.sort(havingMigratedFields);
        assertThat(StringUtils.concatenate(",", havingMigratedFields)).isEqualTo("cfValuesAsJson,field5,varcharFromJson('crm_customer.cfValuesAsJson','customPrices','string','String','0')");

        assertThat(StringUtils.concatenate(",", List.of(paginationConfiguration.getOrderings()))).isEqualTo("id,ASCENDING,cfValuesAsJson,DESCENDING,left(cfValuesAsJson,5),ASCENDING,cfValuesAsJson,ASCENDING");

    }
}
