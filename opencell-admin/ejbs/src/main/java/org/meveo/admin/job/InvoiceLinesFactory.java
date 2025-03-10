package org.meveo.admin.job;

import static java.math.BigDecimal.ZERO;
import static java.util.Optional.ofNullable;
import static org.meveo.commons.utils.EjbUtils.getServiceInterface;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.NumberUtils;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.DatePeriod;
import org.meveo.model.admin.Seller;
import org.meveo.model.article.AccountingArticle;
import org.meveo.model.billing.*;
import org.meveo.model.catalog.DiscountPlanItemTypeEnum;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.cpq.ProductVersion;
import org.meveo.model.cpq.commercial.CommercialOrder;
import org.meveo.model.cpq.commercial.OrderLot;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.billing.impl.InvoiceLineService;
import org.meveo.service.billing.impl.RatedTransactionService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;

public class InvoiceLinesFactory {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private InvoiceLineService invoiceLineService = (InvoiceLineService) getServiceInterface(InvoiceLineService.class.getSimpleName());
    private RatedTransactionService ratedTransactionService = (RatedTransactionService) getServiceInterface(RatedTransactionService.class.getSimpleName());
    private SubscriptionService subscriptionService = (SubscriptionService) getServiceInterface(SubscriptionService.class.getSimpleName());

    

    /**
     * @param data        map of ratedTransaction
     * @param configuration aggregation configuration
     * @param result        JobExecutionResultImpl
     * @param billingRun
     * @param openOrderNumber
     * @return new InvoiceLine
     */
    public InvoiceLine create(Map<String, Object> data, Map<Long, Long> iLIdsRtIdsCorrespondence,
                              AggregationConfiguration configuration, JobExecutionResultImpl result,
                              Provider appProvider, BillingRun billingRun, String openOrderNumber) throws BusinessException {
        return initInvoiceLine(data, iLIdsRtIdsCorrespondence,
                appProvider, billingRun, configuration, openOrderNumber);
    }

    private InvoiceLine initInvoiceLine(Map<String, Object> data, Map<Long, Long> iLIdsRtIdsCorrespondence,
                                        Provider appProvider, BillingRun billingRun,
                                        AggregationConfiguration configuration, String openOrderNumber) {
        InvoiceLine invoiceLine = new InvoiceLine();
        
        EntityManager em = invoiceLineService.getEntityManager();
        
        ofNullable(data.get("billing_account__id")).ifPresent(id -> invoiceLine.setBillingAccount(em.getReference(BillingAccount.class, ((Number)id).longValue())));
        ofNullable(data.get("billing_run_id")).ifPresent(id -> invoiceLine.setBillingRun(em.getReference(BillingRun.class, ((Number)id).longValue())));
        ofNullable(data.get("service_instance_id")).ifPresent(id -> invoiceLine.setServiceInstance(em.getReference(ServiceInstance.class, ((Number)id).longValue())));
        ofNullable(data.get("user_account_id")).ifPresent(id -> invoiceLine.setUserAccount(em.getReference(UserAccount.class, ((Number)id).longValue())));
        ofNullable(data.get("offer_id")).ifPresent(id -> invoiceLine.setOfferTemplate(em.getReference(OfferTemplate.class, ((Number)id).longValue())));
        ofNullable(data.get("order_id")).ifPresent(id -> invoiceLine.setCommercialOrder(em.getReference(CommercialOrder.class, ((Number)id).longValue())));
        ofNullable(data.get("product_version_id")).ifPresent(id -> invoiceLine.setProductVersion(em.getReference(ProductVersion.class, ((Number)id).longValue())));
        ofNullable(data.get("order_lot_id")).ifPresent(id -> invoiceLine.setOrderLot(em.getReference(OrderLot.class, ((Number)id).longValue())));
        ofNullable(data.get("tax_id")).ifPresent(id -> invoiceLine.setTax(em.getReference(Tax.class, ((Number)id).longValue())));
        ofNullable(data.get("article_id")).ifPresent(id -> invoiceLine.setAccountingArticle(em.getReference(AccountingArticle.class, ((Number)id).longValue())));
        ofNullable(data.get("discount_plan_type"))
            .ifPresent(dpt -> invoiceLine.setDiscountPlanType(dpt instanceof DiscountPlanItemTypeEnum ? (DiscountPlanItemTypeEnum) dpt : DiscountPlanItemTypeEnum.valueOf((String) dpt)));
        ofNullable(data.get("discount_value")).ifPresent(id -> invoiceLine.setDiscountValue((BigDecimal) data.get("discount_value")));
        ofNullable(data.get("seller_id")).ifPresent(id -> invoiceLine.setSeller(em.getReference(Seller.class, ((Number)id).longValue())));
        ofNullable(data.get("invoice_type_id")).ifPresent(id -> invoiceLine.setInvoiceType(em.getReference(InvoiceType.class, ((Number)id).longValue())));
        ofNullable(data.get("method_payment_id")).ifPresent(id -> invoiceLine.setPaymentMethodId(((Number)id).longValue()));
        if(data.get("discounted_ratedtransaction_id")!=null) {
        	Long discountedILId = iLIdsRtIdsCorrespondence.get(((Number)data.get("discounted_ratedtransaction_id")).longValue());
         		if(discountedILId!=null) {
        			InvoiceLine discountedIL = invoiceLineService.findById(discountedILId);
            		invoiceLine.setDiscountedInvoiceLine(discountedIL);
                    String rtID = data.get("rated_transaction_ids").toString();
            		String[] splitrtId = rtID.split(",");
            		for (String id : splitrtId) {
            		    RatedTransaction discountRatedTransaction = ratedTransactionService.findById(Long.valueOf(id));
                        if(discountRatedTransaction!=null) {
                            invoiceLine.setDiscountPlan(discountRatedTransaction.getDiscountPlan());
                            invoiceLine.setDiscountPlanItem(discountRatedTransaction.getDiscountPlanItem());
                            invoiceLine.setSequence(discountRatedTransaction.getSequence());
                            invoiceLine.setDiscountAmount(invoiceLine.getDiscountAmount()
                                    .add(discountRatedTransaction.getDiscountValue()));
                            break;
                        }
                    }
            		
        		}
        		
        }

        Date usageDate = getUsageDate(data.get("usage_date"), configuration.getDateAggregationOption());
        invoiceLine.setValueDate(usageDate);
        if (invoiceLine.getValueDate() == null) {
            invoiceLine.setValueDate(new Date());
        }
        invoiceLine.setOrderNumber((String) data.get("order_number"));
        invoiceLine.setQuantity((BigDecimal) data.get("quantity"));
        invoiceLine.setDiscountRate(ZERO);
        invoiceLine.setBillingRun(billingRun);
        BigDecimal taxPercent = invoiceLine.getTax() != null ? invoiceLine.getTax().getPercent() : (BigDecimal) data.get("tax_percent");
        invoiceLine.setTaxRate(taxPercent);
        BigDecimal amountWithoutTax = ofNullable((BigDecimal) data.get("sum_without_tax")).orElse(ZERO);
        BigDecimal amountWithTax = ofNullable((BigDecimal) data.get("sum_with_tax")).orElse(ZERO);
        BigDecimal[] amounts = NumberUtils.computeDerivedAmounts(amountWithoutTax, amountWithTax, taxPercent, appProvider.isEntreprise(), appProvider.getRounding(),
                appProvider.getRoundingMode().getRoundingMode());
        invoiceLine.setAmountWithoutTax(amounts[0]);
        invoiceLine.setAmountWithTax(amounts[1]);
        invoiceLine.setAmountTax(amounts[2]);

        boolean isEnterprise = configuration.isEnterprise();
        if(billingRun != null
                && billingRun.isDisableAggregation()
                && billingRun.isAggregateUnitAmounts()) {
            BigDecimal unitAmount = Optional.ofNullable((BigDecimal) data.get(isEnterprise ? "sum_without_tax" : "sum_with_tax")).orElse(ZERO);
            BigDecimal quantity = (BigDecimal) data.getOrDefault("quantity", ZERO);
            BigDecimal unitPrice = quantity.compareTo(ZERO) == 0 ? unitAmount : unitAmount.divide(quantity,
                    appProvider.getRounding(), appProvider.getRoundingMode().getRoundingMode());
            invoiceLine.setUnitPrice(unitPrice);
        } else {
            invoiceLine.setUnitPrice(Optional.ofNullable((BigDecimal) data.get(isEnterprise ? "unit_amount_without_tax" : "unit_amount_with_tax")).orElse(ZERO));
        }
        invoiceLine.setRawAmount(isEnterprise ? amountWithoutTax : amountWithTax);
        
        if((boolean) data.getOrDefault("use_specific_price_conversion", false)) {
        	invoiceLine.setUseSpecificPriceConversion(true);
        	
        	BigDecimal convertedAmountWithTax = (BigDecimal) data.getOrDefault("sum_converted_amount_with_tax", ZERO);
        	BigDecimal convertedAmountWithoutTax = (BigDecimal) data.getOrDefault("sum_converted_amount_without_tax", ZERO);
        	invoiceLine.setTransactionalAmountWithTax (convertedAmountWithTax);
			invoiceLine.setTransactionalAmountWithoutTax(convertedAmountWithoutTax);
        	invoiceLine.setTransactionalAmountTax((BigDecimal) data.get("sum_converted_amount_tax"));
        	BigDecimal unitAmount = isEnterprise ? convertedAmountWithoutTax : convertedAmountWithTax;
            BigDecimal quantity = (BigDecimal) data.getOrDefault("quantity", ZERO);
            BigDecimal unitPrice = quantity.compareTo(ZERO) == 0 ? unitAmount : unitAmount.divide(quantity,
                    appProvider.getRounding(), appProvider.getRoundingMode().getRoundingMode());
			invoiceLine.setTransactionalUnitPrice(unitPrice);
        	invoiceLine.setTransactionalRawAmount(isEnterprise ? convertedAmountWithoutTax : amountWithTax);
        }
        
        DatePeriod validity = new DatePeriod();
        validity.setFrom(ofNullable((Date) data.get("start_date")).orElse(usageDate));
        validity.setTo(ofNullable((Date) data.get("end_date")).orElse(null));
        if (data.get("subscription_id") != null) {
            Subscription subscription = em.getReference(Subscription.class, ((Number) data.get("subscription_id")).longValue());
            invoiceLine.setSubscription(subscription);
            if (data.get("commercial_order_id") != null) {
                invoiceLine.setCommercialOrder(em.getReference(CommercialOrder.class, ((Number) data.get("commercial_order_id")).longValue()));
            }
            invoiceLine.setSubscriptions(Set.of(subscription));
        } else {
            String subscriptionIds = (String) data.get("subscription_ids");
            if (!StringUtils.isBlank(subscriptionIds)) {
                String[] ids = subscriptionIds.split(",");
                for (String id : ids) {
                    Subscription subscription = subscriptionService.findById(Long.valueOf(id));
                    if (subscription != null) {
                        invoiceLine.addSubscription(subscription);
                    }
                }
            }
        }
        invoiceLine.setValidity(validity);
        if(billingRun != null && billingRun.isUseAccountingArticleLabel()
                && invoiceLine.getAccountingArticle() != null) {
            String languageCode = getLanguageCode(invoiceLine.getBillingAccount(), appProvider);
            Map<String, String> descriptionsI18N = invoiceLine.getAccountingArticle().getDescriptionI18nNullSafe();
            invoiceLine.setLabel(ofNullable(descriptionsI18N.get(languageCode))
                    .orElse(invoiceLine.getAccountingArticle().getDescription()));
        } else {
            String label = StringUtils.EMPTY;
            if (data.get("label") != null) {
                label = (String) data.get("label");
            } else if (invoiceLine.getAccountingArticle() != null && invoiceLine.getAccountingArticle().getDescription() != null) {
                label = invoiceLine.getAccountingArticle().getDescription();
            }
            invoiceLine.setLabel(label);
        }
        ofNullable(openOrderNumber).ifPresent(invoiceLine::setOpenOrderNumber);
        if(configuration.getAdditionalAggregation() != null && !configuration.getAdditionalAggregation().isEmpty()) {
            Map<String, String> additionalAggregationFields = new HashMap<>();
            configuration.getAdditionalAggregation()
                    .forEach(additionalFields
                            -> additionalAggregationFields.put(additionalFields, (String) data.get(additionalFields)));
            invoiceLine.setAdditionalAggregationFields(additionalAggregationFields);
        }
        invoiceLine.setInvoiceRounding(appProvider.getInvoiceRounding());
        invoiceLine.setRoundingMode(appProvider.getRoundingMode());
        InvoiceLine.setRoundingConfig(appProvider.getInvoiceRounding(), appProvider.getRoundingMode());
        return invoiceLine;
    }

    /**
     * @param invoiceLineId id of invoice line to be updated
     * @param deltaAmounts difference of amount of tax, difference of amount with tax, difference of amount without tax
     *                     to be updated
     * @param deltaQuantity difference of quantity to be updated
     * @param beginDate beginDate
     * @param endDate endDate
     */
    public void update(Long invoiceLineId, BigDecimal[] deltaAmounts, BigDecimal deltaQuantity, Date beginDate,
                       Date endDate, BigDecimal unitPrice) throws BusinessException {
        invoiceLineService.getEntityManager()
                .createNamedQuery("InvoiceLine.updateByIncrementalMode")
                .setParameter("id", invoiceLineId).setParameter("deltaAmountWithoutTax", deltaAmounts[0])
                .setParameter("deltaAmountWithTax", deltaAmounts[1]).setParameter("deltaAmountTax", deltaAmounts[2])
                .setParameter("deltaQuantity", deltaQuantity).setParameter("beginDate", beginDate)
                .setParameter("endDate", endDate).setParameter("now", new Date())
                .setParameter("unitPrice", unitPrice).executeUpdate();
    }

    /**
     * get usage date from string.
     *
     * @param usageDate             a date string
     * @param dateAggregationOption a date aggregation option.
     * @return a date
     */
    private Date getUsageDate(Object usageDate, DateAggregationOption dateAggregationOption) {
    	if(usageDate instanceof Date) {
    		return (Date)usageDate;
    	}
        try {
        	String usageDateString = (String) usageDate;
            if (usageDateString != null) {
                if (usageDateString.length() == 7) {
                    if (DateAggregationOption.MONTH_OF_USAGE_DATE.equals(dateAggregationOption)) {
                        usageDateString = usageDateString.concat("-01");
                    } else if (DateAggregationOption.WEEK_OF_USAGE_DATE.equals(dateAggregationOption)) {
                        int year = Integer.parseInt(usageDateString.split("-")[0]);
                        int week = Integer.parseInt(usageDateString.split("-")[1]);
                        return DateUtils.getFirstDayFromYearAndWeek(year, week);
                    }
                }
                return DateUtils.parseDate(usageDateString);
            }
        } catch (Exception e) {
            log.error("cannot parse '{}' as date", usageDate);
        }
        return null;
    }

    private String getLanguageCode(BillingAccount billingAccount, Provider provider) {
        String languageCode = billingAccount.getBillingAccountTradingLanguageCode();
        if(languageCode == null) {
            languageCode = provider.getLanguage() != null ? provider.getLanguage().getLanguageCode() : "ENG";
        }
        return languageCode;
    }

}