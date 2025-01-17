package org.meveo.service.billing.impl;

import static java.util.Arrays.asList;
import static org.meveo.model.billing.BillingEntityTypeEnum.BILLINGACCOUNT;
import static org.meveo.model.billing.BillingEntityTypeEnum.ORDER;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.AggregationConfiguration;
import org.meveo.admin.util.pagination.FilterOperatorEnum;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.response.PagingAndFiltering.SortOrder;
import org.meveo.api.generics.PersistenceServiceHelper;
import org.meveo.api.generics.filter.FactoryFilterMapper;
import org.meveo.api.generics.filter.FilterMapper;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.IEntity;
import org.meveo.model.billing.BillingCycle;
import org.meveo.model.billing.BillingEntityTypeEnum;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.DateAggregationOption;
import org.meveo.model.billing.DiscountAggregationModeEnum;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.crm.Provider;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.base.PersistenceService;
import org.meveo.util.ApplicationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;

public class InvoiceLineAggregationService implements Serializable {

    private static final long serialVersionUID = 4394465445595777997L;

    private static final String QUERY_FILTER = "a.status = 'OPEN' AND (a.invoicingDate is NULL or a.invoicingDate < :invoiceUpToDate)";

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(InvoiceLineAggregationService.class);

    @Inject
    @Named
    private NativePersistenceService nativePersistenceService;

    @Inject
    @ApplicationProvider
    protected Provider appProvider;

    @Inject
    @MeveoJpa
    private EntityManagerWrapper emWrapper;

    /**
     * Create a query object for IL aggregation lookup
     * 
     * @param billingRun Billing run
     * @param statelessSession Stateless session for query creation
     * @param incrementalInvoiceLines Shall Invoice lines be created in incremental mode
     * @param aggregationConfiguration
     * @param nbToRetrieve Number of items to retrieve for processing
     * @return Aggregation summary - number of ILs, BAs and a hibernate query
     */
    @SuppressWarnings("rawtypes")
    public RTtoILAggregationQuery getAggregationSummaryAndILDetailsQuery(BillingRun billingRun, AggregationConfiguration aggregationConfiguration,
                                                                         StatelessSession statelessSession, boolean incrementalInvoiceLines, int nbToRetrieve) {

        // Get a basic RT to IL aggregation query

        BillingCycle billingCycle = billingRun.getBillingCycle();
        Map<String, Object> bcFilter = billingCycle != null ? billingCycle.getFilters() : billingRun.getFilters();
        if (bcFilter == null && billingRun.getBillingCycle() != null) {
            bcFilter = new HashMap<>();
            bcFilter.put("billingAccount.billingCycle.id", billingRun.getBillingCycle().getId());
        }
        if (bcFilter == null) {
            throw new BusinessException("No filter found for billingRun " + billingRun.getId());
        }

        String aggregationQuery = getAggregationJPAQuery(aggregationConfiguration, billingRun, bcFilter);
        List<String> aggregationFields = parseQueryFieldNames(aggregationQuery);

        EntityManager em = emWrapper.getEntityManager();

        // Pass parameters to the aggregation query
        Map<String, Object> params = new HashMap<>();
        // params.put("firstTransactionDate", new Date(0));
        if (billingRun.getLastTransactionDate() != null) {
            params.put("lastTransactionDate", billingRun.getLastTransactionDate());
        }
        params.put("invoiceUpToDate", billingRun.getInvoiceDate());

        // Aggregated RT information is written to a materialized view. In case of incremental processing, materialized view is then joined with a IL table

        String sql = PersistenceService.getNativeQueryFromJPA(em.createQuery(aggregationQuery), params);

        Session hibernateSession = em.unwrap(Session.class);

        String viewName = InvoiceLineAggregationService.getMaterializedAggregationViewName(billingRun.getId());
        String materializedViewFields = StringUtils.concatenate(",", aggregationFields);
        hibernateSession.doWork(new org.hibernate.jdbc.Work() {

            @Override
            public void execute(Connection connection) throws SQLException {

                try (Statement statement = connection.createStatement()) {
                    log.info("Dropping and rereating materialized view {} with fields {} and request {}: ", viewName, materializedViewFields, (nbToRetrieve > 0 ? sql + " limit " + nbToRetrieve : sql));
                    statement.execute("drop materialized view if exists " + viewName);
                    statement.execute("create materialized view " + viewName + "(" + materializedViewFields + ") as " + (nbToRetrieve > 0 ? sql + " limit " + nbToRetrieve : sql));
                    statement.execute("create index idx__" + viewName + " ON " + viewName + " USING btree (billing_account__id, offer_id, article_id, tax_id) ");
                } catch (Exception e) {
                    log.error("Failed to drop/create the materialized view " + viewName, e.getMessage());
                    throw new BusinessException(e);
                }
            }
        });

        Long ilCount =  (Long) em.createNativeQuery("select count(*) from " + viewName).getSingleResult();
        Long baCount = 0L;
        org.hibernate.query.Query query = null;
        if (ilCount > 0) {
            baCount = (Long) em.createNativeQuery("select count(distinct billing_account__id) from " + viewName).getSingleResult();

            if (!aggregationConfiguration.isDisableAggregation() && incrementalInvoiceLines) {
                aggregationQuery = buildJoinWithILQuery(billingRun.getId(), aggregationFields, aggregationConfiguration);
                aggregationFields = parseQueryFieldNames(aggregationQuery);
            } else {
                aggregationQuery = "select " + materializedViewFields + " from {h-schema}" + viewName + " order by billing_account__id";
            }
            query = statelessSession.createNativeQuery(aggregationQuery);
        }

        return new RTtoILAggregationQuery(query, aggregationFields, ilCount, baCount);

    }

    /**
     * Get Rated transaction to Invoice line aggregation JPA query
     * 
     * @param aggregationConfiguration Aggregation configuration
     * @param billingRun Billing run
     * @param bcFilter Additional filter of Billing cycle
     * @return A JPA query to aggregate Rated transactions
     */
    private String getAggregationJPAQuery(AggregationConfiguration aggregationConfiguration, BillingRun billingRun, Map<String, Object> bcFilter) {

        String query = buildAggregationQuery(billingRun, aggregationConfiguration, bcFilter);

        query = "select " + billingRun.getId() + " as billing_run_id, " + query.substring(query.toLowerCase().indexOf("select") + 6);

        return query;
    }

    private String getUsageDateAggregationFunction(DateAggregationOption dateAggregationOption, String usageDateColumn, String alias) {
        switch (dateAggregationOption) {
        case MONTH_OF_USAGE_DATE:
            return " TO_CHAR(" + alias + "." + usageDateColumn + ", 'YYYY-MM') ";
        case DAY_OF_USAGE_DATE:
            return " TO_CHAR(" + alias + "." + usageDateColumn + ", 'YYYY-MM-DD') ";
        case WEEK_OF_USAGE_DATE:
            return " TO_CHAR(" + alias + "." + usageDateColumn + ", 'YYYY-WW') ";
        case NO_DATE_AGGREGATION:
            return usageDateColumn;
        }
        return usageDateColumn;
    }

    /**
     * Get a list of fields to retrieve
     * 
     * @param aggregationConfiguration aggregation configuration
     * @return A list of fields
     */
    private List<String> buildAggregationFieldList(AggregationConfiguration aggregationConfiguration) {

        String usageDateAggregationFunction = getUsageDateAggregationFunction(aggregationConfiguration.getDateAggregationOption(), "usageDate", "a");
        String unitAmount = appProvider.isEntreprise() ? "unitAmountWithoutTax" : "unitAmountWithTax";
        String unitAmountAggregationFunction = aggregationConfiguration.isAggregationPerUnitAmount() ? "SUM(a.unitAmountWithoutTax)" : unitAmount;

        List<String> fieldToFetch;
        if (aggregationConfiguration.isDisableAggregation()) {
            fieldToFetch = new ArrayList<>(
                asList("id as rated_transaction_ids", "billingAccount.id as billing_account__id", "description as label", "quantity as quantity", unitAmount + " as unit_amount_without_tax",
                    "amountWithoutTax as sum_without_tax", "amountWithTax as sum_with_tax", "offerTemplate.id as offer_id", "serviceInstance.id as service_instance_id", "usageDate as usage_date",
                    "startDate as start_date", "endDate as end_date", "orderNumber as order_number", "orderInfo.order.id as commercial_order_id", "taxPercent as tax_percent",
                    "tax.id as tax_id", "orderInfo.productVersion.id as product_version_id", "orderInfo.orderLot.id as order_lot_id", "chargeInstance.id as charge_instance_id", "accountingArticle.id as article_id",
                    "discountedRatedTransaction as discounted_ratedtransaction_id", "discountPlanType as discount_plan_type", "discountValue as discount_value", "subscription.id as subscription_id", "userAccount.id as user_account_id", "seller.id as seller_id"));

        } else {
            fieldToFetch = new ArrayList<>(
                asList("string_agg_long(a.id) as rated_transaction_ids", "billingAccount.id as billing_account__id", "SUM(a.quantity) as quantity", unitAmountAggregationFunction + " as unit_amount_without_tax",
                    "SUM(a.amountWithoutTax) as sum_without_tax", "SUM(a.amountWithTax) as sum_with_tax", "offerTemplate.id as offer_id", usageDateAggregationFunction + " as usage_date", "min(a.startDate) as start_date",
                    "max(a.endDate) as end_date", "taxPercent as tax_percent", "tax.id as tax_id", "accountingArticle.id as article_id", "count(a.id) as rt_count", "seller.id as seller_id"));
            if (aggregationConfiguration.getDiscountAggregation() == DiscountAggregationModeEnum.NO_AGGREGATION) {
	            fieldToFetch.add("discountedRatedTransaction as discounted_ratedtransaction_id");
	            fieldToFetch.add("discountPlanType as discount_plan_type");
	            fieldToFetch.add("discountValue as discount_value");
            }
            if (ORDER == aggregationConfiguration.getType() || !aggregationConfiguration.isIgnoreOrders()) {
                fieldToFetch.add("orderInfo.order.id as commercial_order_id");
                fieldToFetch.add("orderNumber as order_number");
            }
            
            if(!aggregationConfiguration.isIgnoreUserAccounts()) {
                fieldToFetch.add("userAccount.id as user_account_id");
            }
            
            if (aggregationConfiguration.isUseAccountingArticleLabel()) {
                fieldToFetch.add("accountingArticle.description as label");
            } else {
                fieldToFetch.add("description as label");
            }

            if (BILLINGACCOUNT != aggregationConfiguration.getType() || !aggregationConfiguration.isIgnoreSubscriptions()) {
                fieldToFetch.add("subscription.id as subscription_id");
                fieldToFetch.add("serviceInstance.id as service_instance_id");
            } else {
                fieldToFetch.add("string_agg_long(a.subscription.id) as subscription_ids");
            }
            if (aggregationConfiguration.getAdditionalAggregation() != null
                    && !aggregationConfiguration.getAdditionalAggregation().isEmpty()) {
                aggregationConfiguration.getAdditionalAggregation()
                        .forEach(additionalField -> fieldToFetch.add(additionalField + " as " + additionalField));
            }
        }

        return fieldToFetch;
    }

    /**
     * Construct RT to IL aggregation query
     * 
     * @param billingRun Billing run
     * @param aggregationConfiguration
     * @param bcFilter Additional filters of billing cycle
     * @return JPA query
     */
    private String buildAggregationQuery(BillingRun billingRun, AggregationConfiguration aggregationConfiguration, Map<String, Object> bcFilter) {

        List<String> fieldToFetch = buildAggregationFieldList(aggregationConfiguration);

        Set<String> groupBy = getAggregationQueryGroupBy(aggregationConfiguration);

        PaginationConfiguration searchConfig = new PaginationConfiguration(null, null, evaluateFilters(bcFilter, RatedTransaction.class), null, fieldToFetch, groupBy, (Set<String>) null, "billingAccount.id", SortOrder.ASCENDING);

        String extraCondition = (billingRun.getLastTransactionDate() != null ? " a.usageDate < :lastTransactionDate and " : " ") + QUERY_FILTER;
        if(billingRun.getBillingCycle() != null && ORDER.equals(billingRun.getBillingCycle().getType())) {
            extraCondition += " and a.orderInfo.order is not null";
        }

        QueryBuilder queryBuilder = nativePersistenceService.getAggregateQuery("RatedTransaction", searchConfig, null, extraCondition, null);
        return queryBuilder.getQueryAsString();
    }



    private Map<String, Object> evaluateFilters(Map<String, Object> filters, Class<? extends IEntity> entity) {
        return Stream.of(filters.keySet().toArray())
                     .map(key -> {
                         String keyObject = (String) key;
                         if(keyObject.matches("\\$filter[0-9]+$")) {
                             return Collections.singletonMap(keyObject, evaluateFilters((Map<String, Object>)filters.get(key), entity));
                         } else if(!keyObject.startsWith("SQL") && !"$FILTER".equalsIgnoreCase(keyObject) && !"$OPERATOR".equalsIgnoreCase(keyObject)){

                             String fieldName = keyObject.contains(" ") ? keyObject.substring(keyObject.indexOf(" ")).trim() : keyObject;
                             String[] fields=fieldName.split(" ");
                             FilterMapper filterMapper=null;
                             for(String field:fields) {
                                 filterMapper=new FactoryFilterMapper().create(field, filters.get(key), (String) filters.get("cetCode"), PersistenceServiceHelper.getPersistenceService(), entity);
                             }
                             return Collections.singletonMap(keyObject, filterMapper.map());
                         } else if ("$OPERATOR".equalsIgnoreCase(keyObject)) {
                             String filterOperator = (String) filters.get(keyObject);
                             try {
                                 FilterOperatorEnum enumValue = FilterOperatorEnum.valueOf(filterOperator);
                                 return Collections.singletonMap(keyObject, enumValue);
                             } catch (IllegalArgumentException e) {
                                 throw new IllegalArgumentException("Invalid $operator value. Accepted value : 'OR', 'AND'", e);
                             }
                         }
                         return Collections.singletonMap(keyObject, filters.get(key));
                     })
                     .flatMap (map -> map.entrySet().stream())
                     .filter(stringObjectEntry -> Objects.nonNull(stringObjectEntry.getValue()))
                     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String buildJoinWithILQuery(Long billingRunId, List<String> aggregationFields, AggregationConfiguration aggregationConfiguration) {

        StringBuilder sql = new StringBuilder("select ");
        String viewName = getMaterializedAggregationViewName(billingRunId);
        for (String field : aggregationFields) {
            sql.append("agr.").append(field).append(" as ").append(field).append(',');
        }
        sql.append(
            "ivl.id as invoice_line_id, ivl.amount_without_tax as amount_without_tax, ivl.amount_with_tax as amount_with_tax, ivl.tax_rate as tax_rate,ivl.quantity as accumulated_quantity,ivl.begin_date as begin_date,ivl.end_date as finish_date,ivl.unit_price as unit_price ")
            .append(" from ").append(viewName).append(" agr LEFT JOIN billing_invoice_line ivl ON ivl.billing_run_id=").append(billingRunId);

        Map<String, String> joinCriteria = getIncrementalJoinCriteria(aggregationConfiguration);

        for (String joinField : joinCriteria.keySet()) {
            if (aggregationFields.contains(joinField)) {
                sql.append(" and ").append(joinCriteria.get(joinField));
            }
        }

        if (aggregationConfiguration.getDiscountAggregation() == DiscountAggregationModeEnum.NO_AGGREGATION) {
            sql.append(" and agr.discounted_ratedtransaction_id is null and agr.discount_value is null and ivl.discount_value is null and ivl.discounted_invoice_line is null");
        }
        sql.append(" and ivl.status='OPEN'");

        sql.append(" ORDER BY agr.billing_account__id");
        return sql.toString();

    }

    private Set<String> getAggregationQueryGroupBy(AggregationConfiguration aggregationConfiguration) {

        if (aggregationConfiguration.isDisableAggregation()) {
            return null;
        }

        Set<String> groupBy = new LinkedHashSet<>();

        groupBy.add("billingAccount.id");
        groupBy.add("offerTemplate");
        groupBy.add("accountingArticle.id");
        groupBy.add("tax.id");
        groupBy.add("taxPercent");
        groupBy.add("seller.id");
        if (aggregationConfiguration.getDiscountAggregation() == DiscountAggregationModeEnum.NO_AGGREGATION) {
	        groupBy.add("discountedRatedTransaction");
	        groupBy.add("discountValue");
	        groupBy.add("discountPlanType");
        }
        if (aggregationConfiguration.getType() == BillingEntityTypeEnum.ORDER) {
            groupBy.add("orderNumber");
        } else if (aggregationConfiguration.getType() == BillingEntityTypeEnum.SUBSCRIPTION) {
            groupBy.add("subscription.id");
        }

        String usageDateAggregationFunction = getUsageDateAggregationFunction(aggregationConfiguration.getDateAggregationOption(), "usageDate", "a");
        groupBy.add(usageDateAggregationFunction);

        if (!(BILLINGACCOUNT == aggregationConfiguration.getType() && aggregationConfiguration.isIgnoreSubscriptions())) {
            groupBy.add("subscription.id");
            groupBy.add("serviceInstance");
        }

        if (ORDER == aggregationConfiguration.getType() || !aggregationConfiguration.isIgnoreOrders()) {
            groupBy.add("orderInfo.order.id");
            groupBy.add("orderNumber");
        }
        
        if (!aggregationConfiguration.isIgnoreUserAccounts()) {
            groupBy.add("userAccount.id");
        }

        if (!aggregationConfiguration.isAggregationPerUnitAmount()) {
            if (appProvider.isEntreprise()) {
                groupBy.add("unitAmountWithoutTax");
            } else {
                groupBy.add("unitAmountWithTax");
            }
        }

        if (aggregationConfiguration.isUseAccountingArticleLabel()) {
            groupBy.add("accountingArticle.description");
        } else {
            groupBy.add("description");
        }
        if (aggregationConfiguration.getAdditionalAggregation() != null
                && !aggregationConfiguration.getAdditionalAggregation().isEmpty()) {
            aggregationConfiguration.getAdditionalAggregation()
                    .forEach(additionalField -> groupBy.add(additionalField));
        }

        return groupBy;
    }

    /**
     * Get a list of join fields between RT aggregation materialized view and IL table
     * 
     * @param aggregationConfiguration Aggregation configuration
     * @return
     */
    private Map<String, String> getIncrementalJoinCriteria(AggregationConfiguration aggregationConfiguration) {

        // DO NOT Change the order - indexes are build according to the order
        Map<String, String> mapToInvoiceLineTable = new LinkedHashMap<>();

        mapToInvoiceLineTable.put("billing_account__id", "agr.billing_account__id = ivl.billing_account_id");
        mapToInvoiceLineTable.put("offer_id", "agr.offer_id = ivl.offer_template_id");
        mapToInvoiceLineTable.put("article_id", "agr.article_id = ivl.accounting_article_id");
        mapToInvoiceLineTable.put("tax_id", "agr.tax_id = ivl.tax_id");
        mapToInvoiceLineTable.put("tax_percent", "agr.tax_percent =  tax_rate");
        mapToInvoiceLineTable.put("product_version_id", "((agr.product_version_id is null and ivl.product_version_id is null) or agr.product_version_id = ivl.product_version_id)");
        if(!aggregationConfiguration.isDisableAggregation()) {
            String usageDateAggregation = getUsageDateAggregationFunction(aggregationConfiguration.getDateAggregationOption(), "value_date", "ivl");
            mapToInvoiceLineTable.put("usage_date", "((agr.usage_date is null and ivl.value_date is null) or  agr.usage_date =" + usageDateAggregation + ")");
        }

		if(aggregationConfiguration.isDisableAggregation() || !aggregationConfiguration.isIgnoreSubscriptions()) {
		    mapToInvoiceLineTable.put("subscription_id", "((agr.subscription_id is null and  ivl.subscription_id is null) or agr.subscription_id = ivl.subscription_id)");
		    mapToInvoiceLineTable.put("service_instance_id", "((agr.service_instance_id is null and ivl.service_instance_id is null) or agr.service_instance_id = ivl.service_instance_id)");
		}
		if(aggregationConfiguration.isDisableAggregation() || ORDER == aggregationConfiguration.getType() || !aggregationConfiguration.isIgnoreOrders()) {
	        mapToInvoiceLineTable.put("order_id", "((agr.order_id is null and ivl.commercial_order_id is null) or agr.order_id =  ivl.commercial_order_id)");
	        mapToInvoiceLineTable.put("order_number", "((agr.order_number is null and ivl.order_number is null) or agr.order_number = ivl.order_number)");
		}
		if(aggregationConfiguration.isDisableAggregation() || !aggregationConfiguration.isIgnoreUserAccounts()) {
	        mapToInvoiceLineTable.put("user_account_id", "((agr.user_account_id is null and ivl.user_account is null) or agr.user_account_id =  ivl.user_account_id)");
		}
		if(aggregationConfiguration.isDisableAggregation() || !aggregationConfiguration.isAggregationPerUnitAmount()) {
			if (appProvider.isEntreprise()) {
	            mapToInvoiceLineTable.put("unit_amount_without_tax", "((agr.unit_amount_without_tax is null or ivl.unit_price is null) or agr.unit_amount_without_tax = ivl.unit_price)");
	        } else {
	            mapToInvoiceLineTable.put("unit_amount_with_tax", "((agr.unit_amount_with_tax is null or ivl.unit_price is null) or agr.unit_amount_with_tax = ivl.unit_price)");
	        }
		}
        if(aggregationConfiguration.isDisableAggregation() || !aggregationConfiguration.isUseAccountingArticleLabel()) {
        	mapToInvoiceLineTable.put("label", "((agr.label is null and ivl.label is null) or agr.label = ivl.label)");
        }
        
        return mapToInvoiceLineTable;
    }

    /**
     * Get a Invoice line aggregation data materialized view name
     * 
     * @param billingRunId Billing run id
     * @return A materialized view name in format: "billing_il_job_&lt;billingRun Id&gt;"
     */
    public static String getMaterializedAggregationViewName(long billingRunId) {
        return "billing_il_job_" + billingRunId;
    }

    /**
     * Get a list of fieldnames from a query
     * 
     * @param query Sql query
     * @return
     */
    private static List<String> parseQueryFieldNames(String query) {

        List<String> fieldNames = new ArrayList<String>();

        // Get only field part
        query = query.substring(0, query.toLowerCase().indexOf(" from"));

        Pattern pattern = Pattern.compile(" as (\\w*)[ ,]?");
        Matcher matcher = pattern.matcher(query);

        while (matcher.find()) {
            fieldNames.add(matcher.group(1));
        }

        return fieldNames;
    }

    /**
     * A query for aggregate RTs to IL
     */
    public class RTtoILAggregationQuery {

        @SuppressWarnings("rawtypes")
        private org.hibernate.query.Query query;
        private List<String> fieldNames;
        private Long numberOfIL;
        private Long numberOfBA;

        /**
         * Constructor
         * 
         * @param query Aggregation query
         * @param fieldNames Query fieldnames
         * @param numberOfIL Number of Invoices lines that will result in aggregation
         */
        @SuppressWarnings("rawtypes")
        public RTtoILAggregationQuery(Query query, List<String> fieldNames, Long numberOfIL, Long numberOfBA) {
            this.query = query;
            this.fieldNames = fieldNames;
            this.numberOfIL = numberOfIL;
            this.numberOfBA = numberOfBA;
        }

        /**
         * 
         * @return
         */
        @SuppressWarnings("rawtypes")
        public org.hibernate.query.Query getQuery() {
            return query;
        }

        /**
         * @return Query fieldnames
         */
        public List<String> getFieldNames() {
            return fieldNames;
        }

        /**
         * @return Number of Invoices lines that will result in aggregation
         */
        public Long getNumberOfIL() {
            return numberOfIL;
        }

        /**
         * @return Number of distinct Billing accounts
         */
        public Long getNumberOfBA() {
            return numberOfBA;
        }
    }
}