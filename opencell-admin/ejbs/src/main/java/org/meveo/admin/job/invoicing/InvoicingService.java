package org.meveo.admin.job.invoicing;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.AsyncResult;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.NumberUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.I18nDescripted;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingCycle;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.CategoryInvoiceAgregate;
import org.meveo.model.billing.DiscountPlanInstance;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceAgregate;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.InvoiceStatusEnum;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.InvoiceType;
import org.meveo.model.billing.SubCategoryInvoiceAgregate;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TaxInvoiceAgregate;
import org.meveo.model.billing.TradingLanguage;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletInstance;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.catalog.DiscountPlanItem;
import org.meveo.model.catalog.DiscountPlanItemTypeEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.order.Order;
import org.meveo.model.payments.MatchingStatusEnum;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.shared.DateUtils;
import org.meveo.security.MeveoUser;
import org.meveo.security.keycloak.CurrentUserProvider;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.billing.impl.RejectedBillingAccountService;
import org.meveo.service.billing.impl.ServiceSingleton;
import org.meveo.service.catalog.impl.DiscountPlanItemService;
import org.meveo.service.catalog.impl.DiscountPlanService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.meveo.service.script.billing.TaxScriptService;

import com.google.common.collect.Lists;

/**
 * InvoicingService. this class contains services used in invoicing generation job
 *
 */

@Stateless
public class InvoicingService extends PersistenceService<Invoice> {

    private final static BigDecimal HUNDRED = new BigDecimal("100");
    @Inject
    private BillingAccountService billingAccountService;
    @Inject
    private RejectedBillingAccountService rejectedBillingAccountService;
    @Inject
    private ServiceSingleton serviceSingleton;
    @Inject
    private InvoiceService invoiceService;
    @Inject
    private TaxScriptService taxScriptService;
    @Inject
    private CurrentUserProvider currentUserProvider;
    private static final int MAX_IL_TO_UPDATE_PER_TRANSACTION = 10000;
    @Inject
    private InvoiceSubCategoryService invoiceSubCategoryService;
    private Map<Long, Tax> taxes=new TreeMap<Long, Tax>();
    private Map<Long, TradingLanguage> tradingLanguages=new TreeMap<Long, TradingLanguage>();
    private Map<Long, DiscountPlan> discountPlans=new TreeMap<Long, DiscountPlan>();
    private Map<String, String> descriptionMap = new HashMap<>();
	
	@Inject
	private DiscountPlanService discountPlanService;
	@Inject
	private DiscountPlanItemService discountPlanItemService;
    
	/** Creates the aggregates and invoice async. group of BAs at a time in a separate transaction.
	 * 
	 * @param billingRun
	 * @param billingCycle
	 * @param jobInstanceId
	 * @param lastCurrentUser
	 * @param isFullAutomatic
	 * @param result 
	 */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Future<String> createAgregatesAndInvoiceForJob(List<Long> baIds, BillingRun billingRun, BillingCycle billingCycle,  Long jobInstanceId, MeveoUser lastCurrentUser, boolean isFullAutomatic, JobExecutionResultImpl result) {
		return processInvoicingItems(billingRun, billingCycle, readInvoicingItems(billingRun, baIds), jobInstanceId, lastCurrentUser, isFullAutomatic, result);
	}
	
	private List<BillingAccountDetailsItem> readInvoicingItems(BillingRun billingRun, List<Long> baIDs) {
		boolean isBalanceDue = ParamBean.getInstance().getPropertyAsBoolean("invoice.balance.limitByDueDate", true);
        boolean isBalanceLitigation = ParamBean.getInstance().getPropertyAsBoolean("invoice.balance.includeLitigation", false);
        Date toDate = isBalanceDue ? billingRun.getInvoiceDate() : null;
        List<MatchingStatusEnum> status = isBalanceLitigation? List.of(MatchingStatusEnum.O, MatchingStatusEnum.P, MatchingStatusEnum.I) : List.of(MatchingStatusEnum.O, MatchingStatusEnum.P);
		String namedQuery = isBalanceDue ? "BillingAccount.getBillingAccountDetailsItemsLimitAOsByDate" : "BillingAccount.getBillingAccountDetailsItems";
		final Query queryB = getEntityManager().createNamedQuery(namedQuery).setParameter("baIDs", baIDs).setParameter("aoStatus", status);
		if(isBalanceDue) {
			queryB.setParameter("dueDate", DateUtils.setDateToEndOfDay(toDate));
		}
		List<Object[]> resultList = queryB.getResultList();
		if(resultList==null || resultList.isEmpty()) {
			return new ArrayList<>();
		}
		final Map<Long, BillingAccountDetailsItem> billingAccountDetailsMap = resultList.stream().map(BillingAccountDetailsItem::new).collect(Collectors.toMap(BillingAccountDetailsItem::getBillingAccountId, Function.identity()));
		Query query = getEntityManager().createNamedQuery("InvoiceLine.getInvoicingItems").setParameter("ids", baIDs).setParameter("billingRunId", billingRun.getId());
		final Map<Long, List<InvoicingItem>> itemsByBAID = ((List<Object[]>)query.getResultList()).stream().map(InvoicingItem::new).collect(Collectors.groupingBy(InvoicingItem::getBillingAccountId));
		log.info("======= {} InvoicingItems found =======",itemsByBAID.size());
		
		billingAccountDetailsMap.values().stream().forEach(x->x.setInvoicingItems(itemsByBAID.get(x.getBillingAccountId()).stream()
		        .collect(Collectors.groupingBy(InvoicingItem::getInvoiceKey)).values().stream().collect(Collectors.toList())));
		return new ArrayList<>(billingAccountDetailsMap.values());
	}
    
	private Future<String> processInvoicingItems(BillingRun billingRun, BillingCycle billingCycle, List<BillingAccountDetailsItem> invoicingItemsList, Long jobInstanceId, MeveoUser lastCurrentUser, boolean isFullAutomatic, JobExecutionResultImpl result) {
        currentUserProvider.reestablishAuthentication(lastCurrentUser);
        List<List<Invoice>> invoicesByBA = generateInvoices(billingRun, invoicingItemsList, jobInstanceId, isFullAutomatic, billingCycle, result);
        if(!CollectionUtils.isEmpty(invoicesByBA)) {
            writeInvoicingData(billingRun, isFullAutomatic, invoicesByBA, billingCycle);
        }
        return new AsyncResult<>("OK");
    }
	private void validateInvoices(BillingRun billingRun, List<List<Invoice>> invoicesByBA) {
        invoicesByBA.stream().forEach(invoices-> invoiceService.applyAutomaticInvoiceCheck(invoices, true, false, billingRun));
	}

	private List<List<Invoice>> generateInvoices(BillingRun billingRun, List<BillingAccountDetailsItem> invoicingItemsList,
                                                 Long jobInstanceId, boolean isFullAutomatic, BillingCycle billingCycle, JobExecutionResultImpl result) {
        List<List<Invoice>> invoicesByBA = new ArrayList<>();
        List<Invoice> invoices;
        for (BillingAccountDetailsItem billingAccountDetailsItem : invoicingItemsList) {
        	invoices = new ArrayList<>();
            BillingAccount billingAccount = getEntityManager().getReference(BillingAccount.class, billingAccountDetailsItem.getBillingAccountId());
            try {
                createAggregatesAndInvoiceFromInvoicingItems(billingAccountDetailsItem, billingRun, invoices, billingCycle, billingAccount,isFullAutomatic);
                result.incrementInvoiceNumber(invoices.size());
                invoicesByBA.add(invoices);
                result.addNbItemsCorrectlyProcessed(invoices.size());
            } catch (Exception e) {
                log.error("Failed to create invoices for entity {}", billingAccount.getId(), e);
                result.addErrorReport("BA: "+billingAccount.getId()+" error: "+e.getMessage());
                rejectedBillingAccountService.create(billingAccount, billingRun, e.getMessage());
            }
        }
        return invoicesByBA;
    }
    
    private void createAggregatesAndInvoiceFromInvoicingItems(BillingAccountDetailsItem billingAccountDetailsItem, BillingRun billingRun, List<Invoice> invoices, BillingCycle billingCycle, BillingAccount billingAccount, boolean isFullAutomatic){

    	for (List<InvoicingItem> groupedItems : billingAccountDetailsItem.getInvoicingItems()) {
	        final Invoice invoice = initInvoice(billingAccountDetailsItem, billingRun, billingAccount, billingCycle, isFullAutomatic);
	        Set<SubCategoryInvoiceAgregate> invoiceSCAs = createInvoiceAgregates(billingAccountDetailsItem, billingAccount, invoice, groupedItems);
	        evalDueDate(invoice, billingCycle, null, billingAccountDetailsItem.getCaDueDateDelayEL(), billingRun.isExceptionalBR());
	        invoiceService.setInitialCollectionDate(invoice, billingCycle, billingRun);
	        invoice.setSubCategoryInvoiceAgregate(invoiceSCAs);
	        invoices.add(invoice);
    	}
    }

    private void writeInvoicingData(BillingRun billingRun, boolean isFullAutomatic, List<List<Invoice>> invoicesbyBA, BillingCycle billingCycle) {
        log.info("======== CREATING INVOICES FOR {} BAs========", invoicesbyBA.size());
        invoicesbyBA.forEach(invoices->assignNumberAndCreate(billingRun, isFullAutomatic, invoices, billingCycle));
        getEntityManager().flush();//to be able to update ILs
        log.info("======== UPDATING ILs ========");
        invoicesbyBA.forEach(invoices -> invoices.forEach(invoice
                -> invoice.getSubCategoryInvoiceAgregate().forEach(sca
                -> updateInvoiceLines(invoice, billingRun, sca))));
        validateInvoices(billingRun, invoicesbyBA);
    }
    private void assignNumberAndCreate(BillingRun billingRun, boolean isFullAutomatic, List<Invoice> invoices, BillingCycle billingCycle) {
        if(!isFullAutomatic) {
            invoices.forEach(invoice->invoice.setTemporaryInvoiceNumber(serviceSingleton.getTempInvoiceNumber(billingRun.getId())));
        } else {
        	invoices.forEach(invoice->serviceSingleton.assignInvoiceNumberVirtual(invoice));
        	invoiceService.incrementBAInvoiceDate(billingRun, invoices.get(0).getBillingAccount());
        }
		invoices.forEach(invoice -> {
            invoiceService.create(invoice);
            invoiceService.postCreate(invoice);
        });
    }
    
    private void updateInvoiceLines(Invoice invoice, BillingRun billingRun, SubCategoryInvoiceAgregate sca) {
        final List<Long> largeList = sca.getIlIDs();
        for (List<Long> ilIDs : Lists.partition(largeList, MAX_IL_TO_UPDATE_PER_TRANSACTION)) {
            Query query = getEntityManager().createNamedQuery("InvoiceLine.linkToInvoice").setParameter("invoice", invoice).setParameter("invoiceAgregateF", sca).setParameter("ids", ilIDs);
            query.executeUpdate();
        }
    }
    private Set<SubCategoryInvoiceAgregate> createInvoiceAgregates(BillingAccountDetailsItem billingAccountDetailsItem,
                                BillingAccount billingAccount, Invoice invoice, List<InvoicingItem> groupedItems) {
        String languageCode = getTradingLanguageCode(billingAccountDetailsItem.getTradingLanguageId());
        boolean calculateTaxOnSubCategoryLevel = invoice.getInvoiceType().getTaxScript() == null;
        
        Map<SubCategoryInvoiceAgregate, List<InvoicingItem>> itemsBySubCategory = createInvoiceSubCategories(groupedItems, invoice);
        
        List<DiscountPlanItem> applicableDiscountPlanItems = getApplicableDiscounts(billingAccountDetailsItem, invoice);
        final Map<String, List<SubCategoryInvoiceAgregate>> scMap = itemsBySubCategory.keySet().stream().collect(Collectors.groupingBy(SubCategoryInvoiceAgregate::getCategoryAggKey));
        for (List<SubCategoryInvoiceAgregate> scAggregateList : scMap.values()) {
            CategoryInvoiceAgregate cAggregate = initInvoiceCategoryAgg(billingAccount, invoice, languageCode,scAggregateList);
            for (SubCategoryInvoiceAgregate scAggregate : scAggregateList) {
                cAggregate.addSubCategoryInvoiceAggregate(scAggregate);
                if (!BigDecimal.ZERO.equals(scAggregate.getAmount())) {
                    for (DiscountPlanItem discountPlanItem : applicableDiscountPlanItems) {
                        initDiscountAggregates(billingAccount, invoice, scAggregate, itemsBySubCategory.get(scAggregate), cAggregate, discountPlanItem);
                    }
                }
            }
        }
        initTaxAggregations(billingAccountDetailsItem, invoice, calculateTaxOnSubCategoryLevel, billingAccount, languageCode, groupedItems);
        invoice.setNetToPay(invoice.getAmountWithTax().add(invoice.getDueBalance() != null ? invoice.getDueBalance() : BigDecimal.ZERO));
        return itemsBySubCategory.keySet();
    }
    private CategoryInvoiceAgregate initInvoiceCategoryAgg(BillingAccount billingAccount, Invoice invoice, String languageCode, List<SubCategoryInvoiceAgregate> scAggregateList) {
        final SubCategoryInvoiceAgregate firstSCIA = scAggregateList.get(0);
        final InvoiceSubCategory invoiceSubCategory = firstSCIA.getInvoiceSubCategory();
        final InvoiceCategory invoiceCategory = invoiceSubCategory.getInvoiceCategory();
        CategoryInvoiceAgregate cAggregate = new CategoryInvoiceAgregate(invoiceCategory, billingAccount, firstSCIA.getUserAccount(), invoice);
		cAggregate.setUseSpecificPriceConversion(scAggregateList.stream().anyMatch(InvoiceAgregate::isUseSpecificPriceConversion));
        cAggregate.updateAudit(currentUser);
        addTranslatedDescription(languageCode, invoiceCategory, cAggregate,"C");
        invoice.addInvoiceAggregate(cAggregate);
        return cAggregate;
    }
    private Map<SubCategoryInvoiceAgregate, List<InvoicingItem>> createInvoiceSubCategories(List<InvoicingItem> invoicingItems, Invoice invoice) {
        final Map<String, List<InvoicingItem>> scaGroup = invoicingItems.stream().collect(Collectors.groupingBy(InvoicingItem::getScaKey));
        Map<SubCategoryInvoiceAgregate, List<InvoicingItem>> itemsBySubCategory = new HashMap<SubCategoryInvoiceAgregate, List<InvoicingItem>>();
        scaGroup.values().forEach(items->initSubCategoryInvoiceAggregate(items, invoice, itemsBySubCategory));
        return itemsBySubCategory;
    }
    
    private void initSubCategoryInvoiceAggregate(List<InvoicingItem> items, Invoice invoice, Map<SubCategoryInvoiceAgregate, List<InvoicingItem>> itemsBySubCategory) {
        final InvoicingItem invoicingItem = items.get(0);
        InvoiceSubCategory invoiceSubCategory = invoiceSubCategoryService.findFromMap(invoicingItem.getInvoiceSubCategoryId());
        UserAccount userAccount = invoicingItem.getUserAccountId()==null? null : getEntityManager().getReference(UserAccount.class, invoicingItem.getUserAccountId());
		SubCategoryInvoiceAgregate scAggregate = new SubCategoryInvoiceAgregate(getEntityManager().getReference(InvoiceSubCategory.class, invoicingItem.getInvoiceSubCategoryId()), invoice.getBillingAccount(), userAccount, null, invoice, invoiceSubCategory.getAccountingCode());
        scAggregate.updateAudit(currentUser);
        addTranslatedDescription(getTradingLanguageCode(invoicingItem.getBillingAccountId()), invoiceSubCategory, scAggregate,"");
        addAggregationAmounts(items, scAggregate, invoicingItem.getTaxId());
        addInvoiceAggregateWithAmounts(invoice, scAggregate);
        itemsBySubCategory.put(scAggregate,items);
    }
    private Invoice initInvoice(BillingAccountDetailsItem billingAccountDetailsItem, BillingRun billingRun, BillingAccount billingAccount, BillingCycle billingCycle, boolean isFullAutomatic) {
        InvoiceType invoiceType = invoiceService.determineInvoiceType(false, false, false, billingCycle, billingRun, billingAccount);
        Invoice invoice = new Invoice();
        invoice.setBillingAccount(billingAccount);
        invoice.setSeller(billingAccountDetailsItem.getSellerId() != null ? getEntityManager().getReference(Seller.class, billingAccountDetailsItem.getSellerId()) : null);
        invoice.setStatus(isFullAutomatic?InvoiceStatusEnum.VALIDATED:InvoiceStatusEnum.DRAFT);
        invoice.setInvoiceType(invoiceType);
        invoice.setInvoiceDate(billingRun.getInvoiceDate());
        if (billingRun != null) {
            invoice.setBillingRun(billingRun);
        }
        
        if (billingAccountDetailsItem.getPaymentMethodId() != null) {
            invoice.setPaymentMethodType(billingAccountDetailsItem.getPaymentMethodType());
            invoice.setPaymentMethod(getEntityManager().getReference(PaymentMethod.class, billingAccountDetailsItem.getPaymentMethodId()));
        }
        // Set due balance
        
        int balanceFlag = paramBeanFactory.getInstance().getPropertyAsInteger("balance.multiplier", 1);
        BigDecimal balance = billingAccountDetailsItem.getDueBalance();
        balance = balance.multiply(new BigDecimal(balanceFlag));
		invoice.setDueBalance(balance.setScale(getInvoiceRounding(), getRoundingMode()));
        invoice.setNewInvoicingProcess(true);
        populateCustomFieldDefaultValues(invoice);
        return invoice;
    }

    private void populateCustomFieldDefaultValues(Invoice invoice) {
        Map<String, CustomFieldTemplate> associatedCF =
                customFieldTemplateService.findByAppliesTo(Invoice.class.getSimpleName());
        for (Map.Entry<String, CustomFieldTemplate> cfField : associatedCF.entrySet()) {
            if (isNotBlank(cfField.getValue().getDefaultValue())) {
                customFieldInstanceService.setCFValue(invoice, cfField.getKey(), cfField.getValue().getDefaultValue());
            }
        }
    }

    private void addTranslatedDescription(String languageCode, I18nDescripted invoiceSubCategory, InvoiceAgregate aggregate, String prefix) {
        String translationKey = prefix+invoiceSubCategory.getId() + "_" + languageCode;
        String descTranslated = descriptionMap.get(translationKey);
        if (descTranslated == null) {
            descTranslated = invoiceSubCategory.getDescriptionOrCode();
            if ((invoiceSubCategory.getDescriptionI18n() != null) && (invoiceSubCategory.getDescriptionI18n().get(languageCode) != null)) {
                descTranslated = invoiceSubCategory.getDescriptionI18n().get(languageCode);
            }
            descriptionMap.put(translationKey, descTranslated);
        }
        aggregate.setDescription(descTranslated);
    }
    
    private void initTaxAggregations(BillingAccountDetailsItem BillingAccountDetailsItem, Invoice invoice, boolean calculateTaxOnSubCategoryLevel, BillingAccount billingAccount, String languageCode, List<InvoicingItem> invoicingItems) {
        Boolean isExonerated = billingAccountService.isExonerated(billingAccount, BillingAccountDetailsItem.getExoneratedFromTaxes(), BillingAccountDetailsItem.getExonerationTaxEl());
        if (isExonerated) {
            return;
        }
        if (calculateTaxOnSubCategoryLevel) {
            Map<Long, List<InvoicingItem>> itemsByTax = invoicingItems.stream().collect(Collectors.groupingBy(InvoicingItem::getTaxId));
            //tax aggregations will be used to override amounts
            Map<Long, TaxInvoiceAgregate> taxAggByMap = new TreeMap<Long, TaxInvoiceAgregate>(); 
            invoice.initAmounts();
            for(List<InvoicingItem> items: itemsByTax.values()) {
                if(!items.isEmpty() && items.get(0).getTaxId() != null) {
                    createOrMergeTaxAggregate(invoice, billingAccount, languageCode, taxAggByMap, items, null, items.get(0).getTaxId());
                }
            }
            for(TaxInvoiceAgregate compositeTaxAgregate : taxAggByMap.values().stream().filter(x->x.getTax().isComposite()).collect(Collectors.toList())) {
            	createOrMergeSubTaxes(invoice, billingAccount, languageCode, taxAggByMap, compositeTaxAgregate);
            }
        } else {
             // If tax calculation is not done at subcategory level, then call a global script to do calculation for the whole invoice
            if ((invoice.getInvoiceType() != null) && (invoice.getInvoiceType().getTaxScript() != null)) {
                Map<String, TaxInvoiceAgregate> taxAggregates = taxScriptService.createTaxAggregates(invoice.getInvoiceType().getTaxScript().getCode(), invoice);
                if (taxAggregates != null) {
                    for (TaxInvoiceAgregate taxAggregate : taxAggregates.values()) {
                        taxAggregate.setInvoice(invoice);
                        invoice.addInvoiceAggregate(taxAggregate);
                    }
                }
            }
        }
    }

	private void createOrMergeSubTaxes(Invoice invoice, BillingAccount billingAccount, String languageCode,
			Map<Long, TaxInvoiceAgregate> taxAggByMap, TaxInvoiceAgregate compositeTaxAgregate) {
		Tax compositeTax = compositeTaxAgregate.getTax();
		BigDecimal compositePercent = compositeTax.getPercent();
		List<Tax> subTaxes = compositeTax.getSubTaxes();
		BigDecimal composteTaxAmount=compositeTaxAgregate.getAmountTax();
		BigDecimal subTaxTotalAmount=BigDecimal.ZERO;
		for(int i=0; i<subTaxes.size(); i++) {
		    Tax subTax = subTaxes.get(i);
		    BigDecimal amountTax = BigDecimal.ZERO.equals(compositePercent)? BigDecimal.ZERO
		            : (i==subTaxes.size()-1)? composteTaxAmount.subtract(subTaxTotalAmount)
		                    : composteTaxAmount.multiply(subTax.getPercent()).divide(compositePercent,appProvider.getInvoiceRounding(),RoundingMode.HALF_UP);
		    subTaxTotalAmount=subTaxTotalAmount.add(amountTax);
		    TaxInvoiceAgregate subTaxAgg = new TaxInvoiceAgregate(compositeTaxAgregate, subTax, subTax.getPercent(), amountTax);
		    createOrMergeTaxAggregate(invoice, billingAccount, languageCode, taxAggByMap, null, subTaxAgg, subTax.getId());
		}
	}

	private void createOrMergeTaxAggregate(Invoice invoice, BillingAccount billingAccount, String languageCode, Map<Long, TaxInvoiceAgregate> taxAggByMap, List<InvoicingItem> items, TaxInvoiceAgregate subTax, Long taxId) {
		final Tax tax = getTax(taxId);
		boolean aggAlreadyCreated = taxAggByMap.get(taxId)!=null;
		TaxInvoiceAgregate taxAggregate = aggAlreadyCreated ? taxAggByMap.get(taxId) : new TaxInvoiceAgregate(billingAccount, tax, tax.getPercent(), null);

		if(subTax==null) {
			addAggregationAmounts(items, taxAggregate, taxId);
			addInvoiceAggregateWithAmounts(invoice, taxAggregate, tax.isComposite());
		} else {
			addAggregationAmounts(subTax, taxAggregate);
			if(!aggAlreadyCreated) {
				taxAggregate.setInvoice(invoice);
				taxAggregate.setBillingRun(invoice.getBillingRun());
				invoice.addInvoiceAggregate(taxAggregate);
			}
		}
		if(!aggAlreadyCreated) {
			addTranslatedDescription(languageCode, getTax(taxId), taxAggregate, "T");
			taxAggregate.updateAudit(currentUser);
			taxAggByMap.put(taxId, taxAggregate);
		}
		
	}
	
    private Tax getTax(Long taxId) {
        if(taxes.isEmpty()) {
            taxes =  getEntityManager().createNamedQuery("Tax.getAllTaxes",Tax.class).getResultList().stream().collect(Collectors.toMap(Tax::getId, Function.identity()));
        }
        return taxes.get(taxId);
    }
    
    private DiscountPlan getDiscountPlan(long discountPlanId) {
        if(discountPlans.isEmpty()) {
            //final BinaryOperator<DiscountPlan> mergeFunction = (x1, x2) -> {x1.getDiscountPlanItems().addAll(x2.getDiscountPlanItems());return x1;};
            discountPlans =  getEntityManager().createNamedQuery("DiscountPlan.getAll",DiscountPlan.class).getResultList().stream().collect(Collectors.toMap(DiscountPlan::getId, Function.identity(), (x1, x2) -> x1));
        }
        return discountPlans.get(discountPlanId);
    }
    
    private String getTradingLanguageCode(Long tradingLanguageId) {
        if(tradingLanguages.isEmpty()) {
            tradingLanguages =  getEntityManager().createNamedQuery("TradingLanguage.findAll",TradingLanguage.class).getResultList().stream().collect(Collectors.toMap(TradingLanguage::getId, Function.identity()));
        }
        return tradingLanguages.get(tradingLanguageId)!=null?tradingLanguages.get(tradingLanguageId).getLanguageCode():"";
    }
    private void addAggregationAmounts(List<InvoicingItem> items, InvoiceAgregate invoiceAggregate, Long taxId) {
        InvoicingItem summuryItem = new InvoicingItem(items);
        Tax tax = getTax(taxId);
        BigDecimal[] amounts = NumberUtils.computeDerivedAmounts(summuryItem.getAmountWithoutTax(), summuryItem.getAmountWithTax(), tax.getPercent(), isEntreprise() , getInvoiceRounding(), getRoundingMode());
        BigDecimal[] transactionalAmounts = NumberUtils.computeDerivedAmounts(summuryItem.getTransactionalAmountWithoutTax(), summuryItem.getTransactionalAmountWithTax(), tax.getPercent(), isEntreprise() , getInvoiceRounding(), getRoundingMode());
        invoiceAggregate.addAmountWithoutTax(amounts[0]);
        invoiceAggregate.addAmountWithTax(amounts[1]);
        invoiceAggregate.addAmountTax(amounts[2]);
        invoiceAggregate.addTransactionAmountWithoutTax(transactionalAmounts[0]);
        invoiceAggregate.addTransactionAmountWithTax(transactionalAmounts[1]);
        invoiceAggregate.addTransactionAmountTax(transactionalAmounts[2]);
        invoiceAggregate.addItemNumber(summuryItem.getCount());
		if (items.stream().anyMatch(InvoicingItem::isUseSpecificTransactionalAmount)) {
			invoiceAggregate.setUseSpecificPriceConversion(true);
		}
		;
        if(invoiceAggregate instanceof SubCategoryInvoiceAgregate) {
            ((SubCategoryInvoiceAgregate)invoiceAggregate).addILs(summuryItem.getilIDs());
        }
    }
    
    private void addAggregationAmounts(InvoiceAgregate subTaxAggregate, InvoiceAgregate invoiceAggregate) {
        invoiceAggregate.addAmountWithoutTax(subTaxAggregate.getAmountWithoutTax());
        invoiceAggregate.addAmountWithTax(subTaxAggregate.getAmountWithTax());
        invoiceAggregate.addAmountTax(subTaxAggregate.getAmountTax());
        invoiceAggregate.addTransactionAmountWithoutTax(subTaxAggregate.getTransactionalAmountWithoutTax());
        invoiceAggregate.addTransactionAmountWithTax(subTaxAggregate.getTransactionalAmountWithTax());
        invoiceAggregate.addTransactionAmountTax(subTaxAggregate.getTransactionalAmountTax());
        invoiceAggregate.addItemNumber(subTaxAggregate.getItemNumber());
    }
    
    private List<DiscountPlanItem> getApplicableDiscounts(BillingAccountDetailsItem billingAccountDetailsItem, Invoice invoice) {
        // Determine which discount plan items apply to this invoice
        List<DiscountPlanItem> applicableDiscountPlanItems = new ArrayList<>();
        //#MEL subscription!=null only if billing by subscription
       /* Subscription subscription = invoice.getSubscription();
        if (subscription == null) {
            List<DiscountPlanItem> result = getApplicableDiscountPlanItems(billingAccount, subscriptionDiscountPlanInstancesfromBillingAccount(billingAccount), invoice);
            ofNullable(result).ifPresent(discountPlans -> applicableDiscountPlanItems.addAll(discountPlans));
        } else if ( subscription.getDiscountPlanInstances() != null && !subscription.getDiscountPlanInstances().isEmpty()) {
            applicableDiscountPlanItems.addAll(getApplicableDiscountPlanItems(billingAccount, subscription.getDiscountPlanInstances(), invoice));
        }*/
        final List<DiscountPlanSummary> discountPlanInstances = billingAccountDetailsItem.getdiscountPlanSummaries();
        if (discountPlanInstances != null && !discountPlanInstances.isEmpty()) {
            applicableDiscountPlanItems.addAll(getApplicableDiscountPlanItems(billingAccountDetailsItem, discountPlanInstances,invoice));
        }
		List<DiscountPlanItem> getApplicableDiscountPlanItems = new ArrayList<>();
	    applicableDiscountPlanItems.forEach(discountPlanItem -> {
			BillingAccount billingAccount = invoice.getBillingAccount();
		    if(invoice.getDiscountPlan()!=null && discountPlanService.isDiscountPlanApplicable(billingAccount, discountPlanItem.getDiscountPlan(), invoice.getInvoiceDate())) {
			    List<DiscountPlanItem> discountItems = discountPlanItemService.getApplicableDiscountPlanItems(billingAccount, discountPlanItem.getDiscountPlan(),
					    null, invoice.getInvoiceDate());
			    getApplicableDiscountPlanItems.addAll(discountItems);
		    }
	    });
        return getApplicableDiscountPlanItems;
    }
    private List<DiscountPlanInstance> subscriptionDiscountPlanInstancesfromBillingAccount(BillingAccount billingAccount) {
        return billingAccount.getUsersAccounts().stream().map(userAccount -> userAccount.getSubscriptions())
                .map(this::addSubscriptionDiscountPlan).flatMap(Collection::stream).collect(toList());
    }
    private List<DiscountPlanInstance> addSubscriptionDiscountPlan(List<Subscription> subscriptions) {
        return subscriptions.stream().map(Subscription::getDiscountPlanInstances).flatMap(Collection::stream).collect(toList());
    }
    private SubCategoryInvoiceAgregate initDiscountAggregates(BillingAccount billingAccount, Invoice invoice, 
            SubCategoryInvoiceAgregate scAggregate, List<InvoicingItem> itemsBySubCategory, CategoryInvoiceAgregate cAggregate, DiscountPlanItem discountPlanItem) throws BusinessException {
        BigDecimal amountToApplyDiscountOn = isEntreprise() ? scAggregate.getAmountWithoutTax() : scAggregate.getAmountWithTax();
        if (BigDecimal.ZERO.compareTo(amountToApplyDiscountOn) == 0) {
            return null;
        }
        // Apply discount if matches the category, subcategory, or applies to any category
        if (!((discountPlanItem.getInvoiceCategory() == null && discountPlanItem.getInvoiceSubCategory() == null)
                || (discountPlanItem.getInvoiceSubCategory() != null && discountPlanItem.getInvoiceSubCategory().getId().equals(scAggregate.getInvoiceSubCategory().getId()))
                || (discountPlanItem.getInvoiceCategory() != null && discountPlanItem.getInvoiceSubCategory() == null && discountPlanItem.getInvoiceCategory().getId().equals(scAggregate.getInvoiceSubCategory().getInvoiceCategory().getId())))) {
            return null;
        }
        BigDecimal discountValue = getDiscountAmountOrPercent(invoice, scAggregate, amountToApplyDiscountOn, discountPlanItem);
        if (BigDecimal.ZERO.compareTo(discountValue) == 0) {
            return null;
        }
        BigDecimal discountAmount = null;
        // Percent based discount
        if (discountPlanItem.getDiscountPlanItemType() == DiscountPlanItemTypeEnum.PERCENTAGE) {
            itemsBySubCategory.stream().forEach(item->apllyDiscountPercent(item, discountValue.abs().divide(HUNDRED)));
            discountAmount = applyDiscount(amountToApplyDiscountOn, discountValue.abs().divide(HUNDRED));
        } else {
            discountAmount = discountValue;
            dispatchDiscountBetweenItems(itemsBySubCategory, discountAmount, amountToApplyDiscountOn);
        }
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        SubCategoryInvoiceAgregate discountAggregate = new SubCategoryInvoiceAgregate(scAggregate.getInvoiceSubCategory(), billingAccount, scAggregate.getUserAccount(), scAggregate.getWallet(), invoice, null);
        discountAggregate.updateAudit(currentUser);
        discountAggregate.setItemNumber(scAggregate.getItemNumber());
        discountAggregate.setCategoryInvoiceAgregate(cAggregate);
        discountAggregate.setDiscountAggregate(true);
        if (discountPlanItem.getDiscountPlanItemType().equals(DiscountPlanItemTypeEnum.PERCENTAGE)) {
            discountAggregate.setDiscountPercent(discountValue);
        }
        discountAggregate.setDiscountPlanItem(discountPlanItem);
        discountAggregate.setDescription(discountPlanItem.getCode());
        
        InvoicingItem discountedItemSum= new InvoicingItem(itemsBySubCategory);
        discountAggregate.setAmountWithoutTax(discountedItemSum.getAmountWithoutTax().subtract(scAggregate.getAmountWithoutTax()));
        discountAggregate.setAmountWithTax(discountedItemSum.getAmountWithTax().subtract(scAggregate.getAmountWithTax()));
        discountAggregate.setAmountTax(discountedItemSum.getAmountTax().subtract(scAggregate.getAmountTax()));
        addInvoiceAggregateWithAmounts(invoice, discountAggregate);
        return discountAggregate;
    }
    
    public void addInvoiceAggregateWithAmounts(Invoice invoice, InvoiceAgregate invoiceAgg, boolean compositeTax) {
    	if(!compositeTax) {
    		invoice.addInvoiceAggregate(invoiceAgg);
    	}
        invoice.addAmountTax(invoiceAgg.getAmountTax());
        invoice.addAmountWithTax(invoiceAgg.getAmountWithTax());
        invoice.addAmountWithoutTax(invoiceAgg.getAmountWithoutTax());
        invoice.addTransactionalAmountTax(invoiceAgg.getTransactionalAmountTax());
        invoice.addTransactionalAmountWithTax(invoiceAgg.getTransactionalAmountWithTax());
        invoice.addTransactionalAmountWithoutTax(invoiceAgg.getTransactionalAmountWithoutTax());
		if (invoiceAgg.isUseSpecificPriceConversion()) {
			invoice.setUseSpecificPriceConversion(true);
		}
		invoice.setAmountWithoutTaxBeforeDiscount(invoice.getAmountWithoutTax());
    }
    
    public void addInvoiceAggregateWithAmounts(Invoice invoice, InvoiceAgregate invoiceAgg) {
    	addInvoiceAggregateWithAmounts(invoice, invoiceAgg, false);
    }
    
    private BigDecimal applyDiscount(BigDecimal amountToApplyDiscountOn, BigDecimal discountValue) {
        return amountToApplyDiscountOn.multiply(BigDecimal.ONE.subtract(discountValue.abs())).setScale(getInvoiceRounding(), getRoundingMode());
    }
    
    private void dispatchDiscountBetweenItems(List<InvoicingItem> taxItems, BigDecimal disountAmount, BigDecimal amount){
        BigDecimal percent = disountAmount.divide(amount, 12, getRoundingMode());
        taxItems.stream().forEach(item->apllyDiscountPercent(item, percent));
        //TODO #MEL adjust delta
    }
    private void apllyDiscountPercent(InvoicingItem taxItem, BigDecimal percent) {
        BigDecimal[] amounts = getAppliedDiscount(taxItem.getAmountWithoutTax(), taxItem.getAmountWithTax(), percent);
        taxItem.setAmountWithTax(amounts[0]);
        taxItem.setAmountWithoutTax(amounts[1]);
        taxItem.setAmountTax(amounts[2]);
    }
    private BigDecimal[] getAppliedDiscount(BigDecimal amountWithoutTax, BigDecimal amountWithTax, BigDecimal percent) {
        amountWithoutTax = applyDiscount(amountWithoutTax, percent);
        amountWithTax = applyDiscount(amountWithTax, percent);
        return new BigDecimal[] { amountWithTax, amountWithoutTax, amountWithTax.subtract(amountWithoutTax) };
    }
    private RoundingMode getRoundingMode() {
        return appProvider.getInvoiceRoundingMode().getRoundingMode();
    }
    private int getInvoiceRounding() {
        return appProvider.getInvoiceRounding();
    }
    
    private boolean isEntreprise() {
        return appProvider.isEntreprise();
    }
    /**
     * Determine a discount amount or percent to apply
     *
     * @param invoice Invoice to apply discount on
     * @param scAggregate Subcategory aggregate to apply discount on
     * @param amount Amount to apply discount on
     * @param discountPlanItem Discount configuration
     * @return A discount percent (0-100)
     */
    private BigDecimal getDiscountAmountOrPercent(Invoice invoice, SubCategoryInvoiceAgregate scAggregate, BigDecimal amount, DiscountPlanItem discountPlanItem) {
        BigDecimal computedDiscount = discountPlanItem.getDiscountValue();
        final String dpValueEL = discountPlanItem.getDiscountValueEL();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(dpValueEL)) {
            //#MEL TODO
            final BigDecimal evalDiscountValue = evaluateDiscountPercentExpression(dpValueEL, scAggregate.getBillingAccount(), scAggregate.getWallet(), invoice, amount);
            log.debug("for discountPlan {} percentEL -> {}  on amount={}", discountPlanItem.getCode(), computedDiscount, amount);
            if (computedDiscount != null) {
                computedDiscount = evalDiscountValue;
            }
        }
        if (computedDiscount == null || amount == null) {
            return BigDecimal.ZERO;
        }
        return computedDiscount;
    }
    private List<DiscountPlanItem> getApplicableDiscountPlanItems(BillingAccountDetailsItem billingAccountDetailsItem, List<DiscountPlanSummary> discountPlansum, Invoice invoice)
            throws BusinessException {
        List<DiscountPlanItem> applicableDiscountPlanItems = new ArrayList<>();
        for (DiscountPlanSummary dps : discountPlansum) {
            final DiscountPlan discountPlan = getDiscountPlan(dps.getDiscountPlanId());
            if (!(isEffective(dps, invoice.getInvoiceDate()) && discountPlan.isActive())) {
                continue;
            }
            for (DiscountPlanItem discountPlanItem : discountPlan.getDiscountPlanItems()) {
                if (discountPlanItem.isActive() && matchDiscountPlanItemExpression(discountPlanItem.getExpressionEl(), invoice, dps, discountPlan)) {
                    applicableDiscountPlanItems.add(discountPlanItem);
                }
            }
        }
        return applicableDiscountPlanItems;
    }
    private boolean isEffective(DiscountPlanSummary dpi, Date invoiceDate) {
        return (dpi.getStartDate()==null || invoiceDate.compareTo(dpi.getStartDate()) >= 0) && (dpi.getEndDate()==null || invoiceDate.before(dpi.getEndDate()));
    }
    private boolean matchDiscountPlanItemExpression(String expression, Invoice invoice, DiscountPlanSummary dps, DiscountPlan discountPlan) throws BusinessException {
        Boolean result = true;
        if (StringUtils.isBlank(expression)) {
            return result;
        }
        Map<Object, Object> userMap = new HashMap<Object, Object>();
        BillingAccount billingAccount = retrieveIfNotManaged(invoice).getBillingAccount();
        if (expression.indexOf("ca") >= 0) {
            userMap.put("ca", billingAccount .getCustomerAccount());
        }
        if (expression.indexOf("ba") >= 0) {
            userMap.put("ba", billingAccount);
        }
        if (expression.indexOf("iv") >= 0) {
            userMap.put("iv", invoice);
        }
        if (expression.indexOf("invoice") >= 0) {
            userMap.put("invoice", invoice);
        }
        if (expression.indexOf("dpi") >= 0) {
            DiscountPlanInstance dpi = new DiscountPlanInstance();
            dpi.setDiscountPlan(discountPlan);
            dpi.setStartDate(dps.getStartDate());
            dpi.setEndDate(dps.getEndDate());
            userMap.put("dpi", dpi );
        }
        if (expression.indexOf("su") >= 0) {
            userMap.put("su", invoice.getSubscription());
        }
        Object res = ValueExpressionWrapper.evaluateExpression(expression, userMap, Boolean.class);
        try {
            result = (Boolean) res;
        } catch (Exception e) {
            throw new BusinessException("Expression " + expression + " do not evaluate to boolean but " + res);
        }
        return result;
    }
    private BigDecimal evaluateDiscountPercentExpression(String expression, BillingAccount billingAccount, WalletInstance wallet, Invoice invoice, BigDecimal subCatTotal)
            throws BusinessException {
        //#MEL
        if (StringUtils.isBlank(expression)) {
            return null;
        }
        Map<Object, Object> userMap = new HashMap<Object, Object>();
        userMap.put("ca", billingAccount.getCustomerAccount());
        userMap.put("ba", billingAccount);
        userMap.put("iv", invoice);
        userMap.put("invoice", invoice);
        userMap.put("wa", wallet);
        userMap.put("amount", subCatTotal);
        BigDecimal result = ValueExpressionWrapper.evaluateExpression(expression, userMap, BigDecimal.class);
        return result;
    }
    
    private void evalDueDate(Invoice invoice, BillingCycle billingCycle, String orderDueDateDelayEL, String caDueDateDelayEL, boolean isExceptionalBR) {
        BillingAccount billingAccount = invoice.getBillingAccount();
        Order order = invoice.getOrder();
        // Determine invoice due date delay either from Order, Customer account or Billing cycle
        String dueDateDelayEL = (order != null && !StringUtils.isBlank(orderDueDateDelayEL)) ? orderDueDateDelayEL:
            (!StringUtils.isBlank(caDueDateDelayEL)) ? caDueDateDelayEL : billingCycle != null ? billingCycle.getDueDateDelayEL() : null;
        Integer delay = invoiceService.evaluateDueDelayExpression(dueDateDelayEL, billingAccount, invoice, order);
        Date dueDate = invoice.getInvoiceDate();
        if(isExceptionalBR && delay == null) {
            delay = 0;
        }
        if (delay != null) {
            dueDate = DateUtils.addDaysToDate(invoice.getInvoiceDate(), delay);
        } else {
            throw new BusinessException("Due date delay is null");
        }
        invoice.setDueDate(dueDate);
    }
}