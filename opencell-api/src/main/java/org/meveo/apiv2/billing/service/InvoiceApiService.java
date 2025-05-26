/**
 * 
 */
package org.meveo.apiv2.billing.service;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.meveo.apiv2.billing.TransactionMode.PROCESS_ALL;
import static org.meveo.model.billing.InvoiceStatusEnum.CANCELED;
import static org.meveo.model.billing.InvoiceStatusEnum.DRAFT;
import static org.meveo.model.billing.InvoiceStatusEnum.VALIDATED;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.*;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.UpdateHugeEntityJob;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.FilterDto;
import org.meveo.api.dto.billing.QuarantineBillingRunDto;
import org.meveo.api.dto.invoice.GenerateInvoiceRequestDto;
import org.meveo.api.exception.*;
import org.meveo.api.invoice.InvoiceApi;
import org.meveo.apiv2.billing.BasicInvoice;
import org.meveo.apiv2.billing.CancellationOutput;
import org.meveo.apiv2.billing.Failure;
import org.meveo.apiv2.billing.GenerateInvoiceResult;
import org.meveo.apiv2.billing.ImmutableCancellationOutput;
import org.meveo.apiv2.billing.ImmutableFailure;
import org.meveo.apiv2.billing.ImmutableInvoiceLine;
import org.meveo.apiv2.billing.ImmutableInvoiceLinesInput;
import org.meveo.apiv2.billing.ImmutableStatistic;
import org.meveo.apiv2.billing.InvoiceCancellationInput;
import org.meveo.apiv2.billing.InvoiceLine;
import org.meveo.apiv2.billing.InvoiceLineInput;
import org.meveo.apiv2.billing.InvoiceLinesInput;
import org.meveo.apiv2.billing.InvoiceLinesToReplicate;
import org.meveo.apiv2.billing.InvoiceNotValidated;
import org.meveo.apiv2.billing.InvoicePatchInput;
import org.meveo.apiv2.billing.ProcessCdrListResult.Statistics;
import org.meveo.apiv2.billing.ProcessingModeEnum;
import org.meveo.apiv2.billing.RejectReasonInput;
import org.meveo.apiv2.billing.ValidateInvoiceResult;
import org.meveo.apiv2.billing.impl.InvoiceMapper;
import org.meveo.apiv2.common.HugeEntity;
import org.meveo.apiv2.common.ImmutableHugeEntity;
import org.meveo.apiv2.ordering.services.ApiService;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.IBillableEntity;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.billing.*;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.filter.Filter;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.OperationCategoryEnum;
import org.meveo.model.settings.AdvancedSettings;
import org.meveo.service.billing.impl.*;
import org.meveo.service.filter.FilterService;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.securityDeposit.impl.FinanceSettingsService;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.meveo.service.settings.impl.AdvancedSettingsService;

public class InvoiceApiService extends BaseApi implements ApiService<Invoice> {

	private static final String INVOICE_API_MASS_ACTION_LIMIT_CODE = "invoice.api.mass.action.limit";

	@Inject
    private InvoiceService invoiceService;

	@Inject
	private InvoiceLineService invoiceLinesService;

	@Inject
	private FilterService filterService;

	@Inject
	private InvoiceBaseApi invoiceBaseApi;
	
    @Inject
    private InvoiceApi invoiceApi;

	@Inject
	protected ResourceBundle resourceMessages;

	@Inject
    protected RatedTransactionService ratedTransactionService;

	@Inject
	private FinanceSettingsService financeSettingsService;
	
	private final List<String> fieldToFetch = List.of("invoiceLines");

	@Inject
	private InvoiceTypeService invoiceTypeService;

	@Inject
	private AdvancedSettingsService advancedSettingsService;

	@Inject
	private BatchEntityService batchEntityService;
	
	@Inject
	private JobInstanceService jobInstanceService;

	@Inject
	private PurchaseOrderService purchaseOrderService;

	public List<Invoice> list(Long offset, Long limit, String sort, String orderBy, String filter) {
        PaginationConfiguration paginationConfiguration = new PaginationConfiguration(offset.intValue(), limit.intValue(), null, filter, null, null, null);
        return invoiceService.listWithlinkedInvoices(paginationConfiguration);
	}
	
	@Override
	public Long getCount(String filter) {
        PaginationConfiguration paginationConfiguration = new PaginationConfiguration(null, null, null, filter, null, null, null);
		return invoiceService.count(paginationConfiguration);
	}

	@Override
	public Optional<Invoice> findById(Long id) {
		return ofNullable(invoiceService.findById(id, fieldToFetch));
	}

	@Override
	public Invoice create(Invoice invoice) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<Invoice> update(Long id, Invoice baseEntity) {
		// TODO Auto-generated method stub
		return empty();
	}

	@Override
	public Optional<Invoice> patch(Long id, Invoice baseEntity) {
		// TODO Auto-generated method stub
		return empty();
	}

	@Override
	public Optional<Invoice> delete(Long id) {
        Invoice invoice = invoiceService.findById(id);
        if(invoice != null) {
            invoiceService.remove(invoice);
            return of(invoice);
        }
        return empty();
	}

	@Override
	public Optional<Invoice> findByCode(String code) {
		return ofNullable(invoiceService.findByInvoiceNumber(code));
	}

	public Optional<Invoice> findById(Long id, List<String> fieldTofetch) {
		return ofNullable(invoiceService.findById(id, fieldTofetch));
	}

	/**
	 * @param invoiceTypeId
	 * @param invoiceNumber
	 * @return
	 */
	public Optional<Invoice> findByInvoiceNumberAndTypeId(Long invoiceTypeId, String invoiceNumber) {
		return ofNullable(invoiceService.findByInvoiceTypeAndInvoiceNumber(invoiceNumber, invoiceTypeId));
	}

	/**
	 * @param invoice
	 * @param generateIfMissing
	 * @return
	 */
	public Optional<byte[]> fetchPdfInvoice(Invoice invoice, boolean generateIfMissing) {
		return ofNullable(invoiceService.getInvoicePdf(invoice, generateIfMissing));
		
	}

    /**
     * @param invoice
     * @return
     */
    public void deleteInvoicePdf(Invoice invoice) {
        if (invoiceService.isInvoicePdfExist(invoice)) {
            try {
                invoiceService.deleteInvoicePdf(invoice);
            } catch (Exception exception) {
                throw new InternalServerErrorException(exception);
            }


        } else {
            throw new NotFoundException("pdf invoice does not exists : ");
        }


    }

    /**
     * @param invoice
     * @return
     */
    public void deleteInvoiceXml(Invoice invoice) {
        if (invoiceService.isInvoiceXmlExist(invoice)) {
            try {
                invoiceService.deleteInvoiceXml(invoice);
            } catch (Exception exception) {
                throw new InternalServerErrorException(exception);
            }


        } else {
            throw new NotFoundException("xml invoice does not exists : ");
        }


    }


    /**
	 * @param basicInvoice
	 * @return
	 */
	public Invoice create(BasicInvoice basicInvoice) {
		return invoiceService.createBasicInvoice(basicInvoice);
	}
	
	/**
	 * @param typeCode
	 * @param invoiceNumber
	 * @return
	 */
	public Optional<Invoice> findByInvoiceNumberAndTypeCode(String typeCode, String invoiceNumber) {
		return ofNullable(invoiceService.findByInvoiceNumberAndTypeCode(invoiceNumber, typeCode));
	}

	/**
	 * Create Invoice Lines
	 * @param invoice Invoice to update {@link Invoice}
	 * @param invoiceLinesInput Invoice Lines Input {@link InvoiceLineInput} with a list of InvoiceLine to create {@link org.meveo.apiv2.billing.InvoiceLine}
	 * @return {@link InvoiceLineInput}
	 */
	public InvoiceLinesInput createLines(Invoice invoice, InvoiceLinesInput invoiceLinesInput) {
		ImmutableInvoiceLinesInput.Builder result = ImmutableInvoiceLinesInput.builder();

		for(InvoiceLine invoiceLineResource : invoiceLinesInput.getInvoiceLines()) {
			// For Each Invoice Line Resource, convert InvoiceLineResource to InvoiceLine
			org.meveo.model.billing.InvoiceLine invoiceLine = invoiceLinesService.getInvoiceLine(invoice, invoiceLineResource);
			// Populate CustomFields
			invoiceBaseApi.populateCustomFieldsForGenericApi(invoiceLineResource.getCustomFields(), invoiceLine, false);
			// Create Invoice Line
			invoiceLine = invoiceLinesService.createInvoiceLine(invoiceLine);
			invoiceLineResource = ImmutableInvoiceLine.copyOf(invoiceLineResource)
					.withId(invoiceLine.getId())
					.withAmountWithoutTax(invoiceLine.getAmountWithoutTax())
					.withAmountWithTax(invoiceLine.getAmountWithTax())
					.withAmountTax(invoiceLine.getAmountTax());
			if (CollectionUtils.isEmpty(invoice.getInvoiceLines())){
				invoice.setInvoiceLines(new ArrayList<>());
			}
			invoice.getInvoiceLines().add(invoiceLine);
			result.addInvoiceLines(invoiceLineResource);
		}
		
		if (InvoiceStatusEnum.NEW.equals(invoice.getStatus())) {
			invoice.setStatus(DRAFT);
			invoice.assignTemporaryInvoiceNumber();
			invoiceService.update(invoice);
			invoiceService.getEntityManager().flush();
		}
		invoiceService.calculateInvoice(invoice, false);
		invoiceService.updateBillingRunStatistics(invoice);
		result.skipValidation(invoiceLinesInput.getSkipValidation());
		return result.build();
	}
	

	/**
	 * @param invoice
	 */
	public void rebuildInvoice(Invoice invoice) {
		invoiceService.rebuildInvoice(invoice, true);
	}

	/**
	 * Update Invoice Line
	 * @param invoice Invoice to update {@link Invoice}
	 * @param invoiceLineInput Invoice Line Input to update {@link InvoiceLineInput}
	 * @param lineId Invoice Line Id
	 */
	public void updateLine(Invoice invoice, InvoiceLineInput invoiceLineInput, Long lineId) {
		// Get Invoice Line to update
		org.meveo.model.billing.InvoiceLine invoiceLine = invoiceLinesService.findInvoiceLine(invoice, lineId);
		DiscountPlan discountPlan = null;
		if (invoice.getStatus() != VALIDATED) {
			if (invoiceLine != null) {
				discountPlan = invoiceLine.getDiscountPlan();
			}
			invoiceLine = invoiceLinesService.initInvoiceLineFromResource(invoiceLineInput.getInvoiceLine(), invoiceLine);
		}
		// Populate Custom fields
		invoiceBaseApi.populateCustomFieldsForGenericApi(invoiceLineInput.getInvoiceLine().getCustomFields(), invoiceLine, false);
        // Update Invoice Line
		invoiceLinesService.updateInvoiceLine(invoiceLine, invoiceLineInput.getInvoiceLine(), discountPlan);
		invoiceService.getEntityManager().flush();
		invoiceService.calculateInvoice(invoice, false);
		BigDecimal lastApliedRate = invoiceService.getCurrentRate(invoice,invoice.getInvoiceDate());
		invoiceService.refreshAdvanceInvoicesConvertedAmount(invoice,lastApliedRate);
		invoiceService.updateBillingRunStatistics(invoice);
	}

	/**
	 * @param invoice
	 * @param lineId
	 */
	public void removeLine(Invoice invoice, Long lineId) {
		invoiceLinesService.remove(invoice, lineId);
		invoiceService.calculateInvoice(invoice, false);
        invoiceService.updateBillingRunStatistics(invoice);
	}

	/**
	 * @param invoice
	 */
	public void rejectInvoice(Invoice invoice, RejectReasonInput rejectReasonInput) {
		invoiceService.rejectInvoice(invoice, rejectReasonInput);
	}

	/**
	 * @param invoice
	 */
	public void validateInvoice(Invoice invoice) {
		invoiceService.validateInvoice(invoice, true);
	}

	/**
	 * @param invoice
	 */
	public void cancelInvoice(Invoice invoice, RatedTransactionAction rtAction) {
		invoiceService.cancelInvoiceWithoutDeleteAndRTAction(invoice, rtAction);
	}

	/**
	 * @param input InvoiceInput
	 * @return
	 */
	public Invoice create(org.meveo.apiv2.billing.InvoiceInput input) {
		Invoice invoice = new Invoice();
		if(input.getInvoice() != null && input.getInvoice().getCustomFields() != null) {
			populateCustomFieldsForGenericApi(input.getInvoice().getCustomFields(), invoice, true);
		}
		return invoiceService.createInvoiceV11(input.getInvoice(), input.getSkipValidation(), input.getIsDraft(),
				input.getIsVirtual(), input.getIsIncludeBalance(), input.getIsAutoValidation(), invoice);
	}

	@Transactional
	public Invoice update(Invoice invoice, Invoice input, org.meveo.apiv2.billing.Invoice invoiceResource) {
        Invoice updateInvoice = invoiceService.update(invoice, input, invoiceResource);
        if (invoiceResource.getCustomFields() != null) {
			populateCustomFieldsForGenericApi(invoiceResource.getCustomFields(), updateInvoice, true);
			invoiceService.update(updateInvoice);
		}
		invoiceService.cancelUnpaidAdv(invoice);
		invoiceService.getEntityManager().flush();
		invoiceService.calculateInvoice(updateInvoice, false);
		return updateInvoice;
    }

	/**
	 * @param invoice
	 */
	public void calculateInvoice(Invoice invoice) {
		invoiceService.calculateInvoice(invoice, true);
        invoiceService.updateBillingRunStatistics(invoice);
	}
	
	public Invoice duplicate(Invoice invoice) {
		return invoiceService.duplicate(invoice);
	}


    public Invoice duplicateInvoiceLines(Invoice invoice, List<Long> invoiceLineIds) {
        List<String> idsInvoiceLineNotFound = new ArrayList<>();
        for(Long lineId : invoiceLineIds) {
            org.meveo.model.billing.InvoiceLine invoiceLine = invoiceLinesService.findById(lineId);
            if (invoiceLine == null) {                
                idsInvoiceLineNotFound.add("" + lineId);
            }
        }

        String idsInvoiceLineNotFoundStr = "";
        if (!idsInvoiceLineNotFound.isEmpty()) {
            for(int i=0; i< idsInvoiceLineNotFound.size() - 1; i++) {
                idsInvoiceLineNotFoundStr += idsInvoiceLineNotFound.get(i) + ", ";
            }
            idsInvoiceLineNotFoundStr += idsInvoiceLineNotFound.get(idsInvoiceLineNotFound.size()-1);
            throw new MeveoApiException("Invoice Line ids does not exist: [" + idsInvoiceLineNotFoundStr + "]."); 
        }
        
        return invoiceService.duplicateInvoiceLines(invoice, invoiceLineIds);        
    }
    
	/**
	 * Generate Invoice
	 * @param invoice Invoice input
	 * @param isDraft
	 */
    public Optional<List<GenerateInvoiceResult>> generate(GenerateInvoiceRequestDto invoice, boolean isDraft) {
		IBillableEntity entity = invoiceService.getBillableEntity(invoice.getTargetCode(), invoice.getTargetType(),
				invoice.getOrderNumber(), invoice.getBillingAccountCode());
		if(entity == null ){
			throw new NotFoundException("BillableEntity does not exists");
		}
    	Filter ratedTransactionFilter = null;
		if(invoice.getFilter() != null) {
			ratedTransactionFilter = getFilterFromInput(invoice.getFilter());
			if (ratedTransactionFilter == null) {
				throw new NotFoundException("Filter does not exists");
			}
		}
		if (isDraft) {
			if (invoice.getGeneratePDF() == null) {
				invoice.setGeneratePDF(TRUE);
			}
			if (invoice.getGenerateAO() != null) {
				invoice.setGenerateAO(Boolean.FALSE);
			}
		}
		ICustomFieldEntity customFieldEntity = new Invoice();
		customFieldEntity =
				invoiceBaseApi.populateCustomFieldsForGenericApi(invoice.getCustomFields(), customFieldEntity, false);
		
		List<BillingAccount> billingAccountsAfter = null;
        if(true==invoice.isApplyBillingRules()) {
            Date firstTransactionDate = invoice.getFirstTransactionDate() == null ? new Date(0) : invoice.getFirstTransactionDate();
            Date lastTransactionDate = invoice.getLastTransactionDate() == null ? invoice.getInvoicingDate() : invoice.getLastTransactionDate();
            List<RatedTransaction> rts = ratedTransactionService.listRTsToInvoice(entity, firstTransactionDate, lastTransactionDate, lastTransactionDate, ratedTransactionFilter, null);
			if (financeSettingsService.isBillingRedirectionRulesEnabled()) {
				billingAccountsAfter = ratedTransactionService.applyInvoicingRules(rts);
			}
        }
        
        List<Invoice> invoices = new ArrayList<>();
        if (billingAccountsAfter == null || billingAccountsAfter.isEmpty()) {
            invoices = invoiceService.generateInvoice(entity, invoice, ratedTransactionFilter,
                isDraft, customFieldEntity.getCfValues(), true);
        }
        else {
            for (BillingAccount billingAccountAfter : billingAccountsAfter) {
                entity = billingAccountAfter;
                invoices.addAll(invoiceService.generateInvoice(entity, invoice, ratedTransactionFilter,
                    isDraft, customFieldEntity.getCfValues(), true));
            }            
        }
		if (invoices == null || invoices.isEmpty()) {
			throw new BusinessException(resourceMessages.getString("error.invoicing.noTransactions"));
		}

		for (Invoice inv : invoices) {
			if (invoice.getPurchaseOrders() != null && !invoice.getPurchaseOrders().isEmpty()) {
				invoiceService.managePurchaseOrders(invoice.getPurchaseOrders(), inv);
				invoiceService.update(inv);
			}
		}
		
		List<GenerateInvoiceResult> generateInvoiceResults = new ArrayList<>();
		InvoiceMapper invoiceMapper = new InvoiceMapper();
		for (Invoice inv : invoices) {
			List<Object[]> invoiceInfo = (List<Object[]>) invoiceService.getEntityManager().createNamedQuery("Invoice.getInvoiceTypeANDRecordedInvoiceID")
					.setParameter("id", inv.getId())
					.getResultList();
			generateInvoiceResults.add(invoiceMapper.toGenerateInvoiceResult(inv, (String) invoiceInfo.get(0)[0], (Long) invoiceInfo.get(0)[1]));			
		}
    	return of(generateInvoiceResults);
    }

	private Filter getFilterFromInput(FilterDto filterDto) throws MeveoApiException {
		Filter filter = null;
		if(!StringUtils.isBlank(filterDto.getPollingQuery()) && RatedTransaction.class.getSimpleName().equals(filterDto.getEntityClass())){
			filter = new Filter();
			filter.setPollingQuery(filterDto.getPollingQuery() + "AND status = 'OPEN'");
			filter.setEntityClass(filterDto.getEntityClass());
			return filter;
		}
		if (StringUtils.isBlank(filterDto.getCode()) && StringUtils.isBlank(filterDto.getInputXml())) {
			throw new BadRequestException("code or inputXml");
		}
		if (!StringUtils.isBlank(filterDto.getCode())) {
			filter = filterService.findByCode(filterDto.getCode());
			if (filter == null && StringUtils.isBlank(filterDto.getInputXml())) {
				throw new NotFoundException("Filter with code does not exists : " + filterDto.getCode());
			}
			if (filter != null && !filter.getShared() && !filter.getAuditable().isCreator(currentUser)) {
				throw new BadRequestException("INVALID_FILTER_OWNER");
			}
		}
		return filter;
	}
	
	public Invoice createAdjustment(Invoice invoice, InvoiceLinesToReplicate invoiceLinesToReplicate) {
	    Invoice adjInvoice = null;

	    invoice = invoiceService.findById(invoice.getId(), asList("invoiceLines", "invoiceType", "invoiceType.occTemplate", "linkedInvoices"));

	    if (invoice.getStatus() != InvoiceStatusEnum.VALIDATED) {
            throw new BadRequestException("Invoice should be Validated");
        }

		String invoiceType = invoice.getInvoiceType() != null ? invoice.getInvoiceType().getCode() : "";
		boolean invoiceTypeForbidden = invoiceTypeService.getListAdjustementCode().contains(invoiceType);
		if(invoiceTypeForbidden) {
			throw new BadRequestException("You cannot create ADJ from another ADJ invoice");
		}
	    
	    if (!OperationCategoryEnum.DEBIT.equals(invoice.getInvoiceType().getOccTemplate().getOccCategory())) {
	        throw new BadRequestException("You cannot make a credit note over another");
        }
	    
	    if (invoiceLinesToReplicate.getGlobalAdjustment() == null) {
            throw new MissingParameterException("globalAdjustment");
        }

		if(Boolean.TRUE.equals(invoiceLinesToReplicate.getRerate())
				&& !Boolean.TRUE.equals(advancedSettingsService.getParameter("rating.allowBilledItemsRerating"))) {
				throw new ForbiddenException(resourceMessages.getString("error.adjustment.action.forbidden"));
			}

	    try {
	        adjInvoice = invoiceService.createAdjustment(invoice, invoiceLinesToReplicate);

    	    if (invoice.getLinkedInvoices() != null) {
    	        invoice.getLinkedInvoices().size();
            }
    	    else {
    	        invoice.setLinkedInvoices(new HashSet<>());
    	    }
    	    invoiceService.update(invoice);

    	    if(Boolean.TRUE.equals(invoiceLinesToReplicate.getRerate())) {
    	        Map<String, Object> filters = new HashMap<>();

    	        if(Boolean.TRUE.equals(invoiceLinesToReplicate.getGlobalAdjustment())) {
    	            filters.put("ratedTransaction.invoiceLine.invoice.id", invoice.getId());
    	        } else {
    	            // filters.put("$operator", "AND");
    	            filters.put("inList ratedTransaction.invoiceLine.id", invoiceLinesToReplicate.getInvoiceLinesIds());
    	            if(CollectionUtils.isNotEmpty(invoiceLinesToReplicate.getInvoiceLinesRTs())) {
    	                filters.put("ratedTransaction.id", invoiceLinesToReplicate.getInvoiceLinesRTs());
    	            }
    	        }

				if(financeSettingsService.isEntityWithHugeVolume(WalletOperation.class.getSimpleName())) {
					HugeEntity batchEntity = ImmutableHugeEntity.builder()
							.filters(filters)
							.targetJob("MarkWOToRerateFromInvoiceJob")
							.build();
	
					batchEntityService.create(batchEntity, batchEntity.getFilters(), WalletOperation.class.getSimpleName());
				} else {
					JobInstance jobInstance = jobInstanceService.findByCode("MarkWOToRerateFromInvoiceJob");

					String selectQuery = batchEntityService.getSelectQuery(WalletOperation.class, filters, null, false);
					List<Long> ids = batchEntityService.getEntityManager().createQuery(selectQuery).getResultList();

					StringBuilder updateQuery = new StringBuilder("UPDATE ").append(WalletOperation.class.getSimpleName())
																			.append(" SET updated=")
																			.append(QueryBuilder.paramToString(new Date()));

					String fieldsToUpdate = (String) customFieldInstanceService.getCFValue(jobInstance, UpdateHugeEntityJob.CF_FIELDS_TO_UPDATE);
					updateQuery.append(", ").append(fieldsToUpdate);
					batchEntityService.update(updateQuery, ids);
				}
    	    }
        } catch (Exception e) {
            throw new BusinessApiException(e);
        }
	    
	    adjInvoice = invoiceService.findById(adjInvoice.getId(), asList("invoiceLines", "invoiceType", "invoiceType.occTemplate", "linkedInvoices"));

	    return adjInvoice;
	}

	public Long quarantineInvoice(Invoice invoice, QuarantineBillingRunDto quarantineBillingRunDto) {       
        return invoiceService.quarantineBillingRun(invoice, quarantineBillingRunDto);
    }

	/**
	 * Invoice refresh rate
	 * @param invoiceId Invoice identifier
	 * @return refresh result
	 */
	public Optional<Invoice> refreshRate(Long invoiceId) {
		Invoice invoice = ofNullable(invoiceService.findById(invoiceId, List.of("tradingCurrency")))
				.orElseThrow(() -> new NotFoundException("Invoice not found"));
		if(invoice.getStatus() != InvoiceStatusEnum.NEW && invoice.getStatus() != InvoiceStatusEnum.DRAFT) {
			throw new ForbiddenException("Refresh rate only allowed for invoices with status : NEW or DRAFT");
		}
		BigDecimal currentRate = invoice.getTradingCurrency().getCurrentRate();
		if(currentRate != null && currentRate.equals(invoice.getLastAppliedRate())) {
			return empty();
		} else {
			return of(invoiceService.refreshConvertedAmounts(invoice,
					currentRate, invoice.getTradingCurrency().getCurrentRateFromDate()));
		}
	}
	
	/**
	 * Update comment and custom fields in a validated invoice
	 * @param invoice {@link Invoice}
	 * @param invoiceResource {@link InvoicePatchInput}
	 * @return {@link Invoice}
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Invoice updateValidatedInvoice(Invoice invoice, org.meveo.apiv2.billing.InvoicePatchInput invoiceResource) {      
    	ICustomFieldEntity customFieldEntity = invoiceBaseApi.populateCustomFieldsForGenericApi(invoiceResource.getCustomFields(), invoice, true);
		return invoiceService.updateValidatedInvoice(invoice, invoiceResource.getComment(), customFieldEntity.getCfValues(), invoiceResource.getPurchaseOrders());
    }

	@Transactional
	public Invoice updateValidatedInvoice(Long invoiceId, InvoicePatchInput invoiceResource) {
		Invoice invoice = invoiceService.findById(invoiceId, asList("invoiceType"));
		if(invoice == null) {
			throw new EntityDoesNotExistsException(Invoice.class, invoiceId);
		}
		if(invoice.getStatus() != InvoiceStatusEnum.VALIDATED) {
			throw new ForbiddenException("Invoice should be Validated");
		}
		
		return updateValidatedInvoice(invoice, invoiceResource);
	}
	
	/**
	 * @param invoice
	 */
	public void setInvoiceExchangeRate(Invoice invoice, BigDecimal exchangeRate) {
		invoiceService.refreshConvertedAmounts(invoice, exchangeRate, new Date());
	}

	public Object validateInvoices(Map<String, Object> filters, ProcessingModeEnum mode, boolean failOnValidatedInvoice, boolean failOnCanceledInvoice, boolean ignoreValidationRules, boolean generateAO) {
		ValidateInvoiceResult result = new ValidateInvoiceResult();
		
		if (ProcessingModeEnum.STOP_ON_FIRST_FAIL.equals(mode)) {
			throw new InvalidParameterException("Mode STOP_ON_FIRST_FAIL is not valid for this API");
		}
		
		if (MapUtils.isEmpty(filters)) {
			throw new InvalidParameterException("filters is required");
		}
		
		List<Invoice> invoices = invoiceService.findByFilter(filters);
		if (invoices == null || invoices.isEmpty()) {
			throw new NotFoundException("No invoice found for the provided filters");
		}
		
		Statistics statics = result.getStatistics();
		statics.setTotal(invoices.size());
		invoices.forEach(invoice -> {
			try {
				if (InvoiceStatusEnum.VALIDATED.equals(invoice.getStatus())) {
					if (failOnValidatedInvoice) {
						result.getInvoicesNotValidated().add(new InvoiceNotValidated(invoice.getId(), String.format("Invoice %s already validated", invoice.getInvoiceNumber())));
						statics.addFail();
					}
				} else if (InvoiceStatusEnum.CANCELED.equals(invoice.getStatus())) {
					if (failOnCanceledInvoice) {
						result.getInvoicesNotValidated().add(new InvoiceNotValidated(invoice.getId(), String.format("Cannot validate already canceled invoice %s", invoice.getId())));
						statics.addFail();
					}
				} else {
					invoice = validateInvoice(ignoreValidationRules, invoice, generateAO);
					if (InvoiceStatusEnum.VALIDATED.equals(invoice.getStatus())) {
						result.getInvoicesValidated().add(invoice.getId());
						statics.addSuccess();
					} else {
						result.getInvoicesNotValidated().add(new InvoiceNotValidated(invoice.getId(), invoice.getRejectReason()));
						statics.addFail();
					}
				}
			} catch (Exception e) {
				result.getInvoicesNotValidated().add(new InvoiceNotValidated(invoice.getId(), e.getMessage()));
				statics.addFail();
				if (mode == ProcessingModeEnum.ROLLBACK_ON_ERROR) {
					throw new BadRequestException(e.getMessage());
				}
			}

		});

		result.getActionStatus().setMessage(String.format("Invoices : %d validated, %d failed", result.getInvoicesValidated().size(), result.getInvoicesNotValidated().size()));
		return result;
	}

	private Invoice validateInvoice(boolean ignoreValidationRules, Invoice invoice, boolean generateAO) throws Exception {
		if (ignoreValidationRules) {
			invoiceService.validateInvoice(invoice.getId());
			if (generateAO) {
				invoiceService.generateRecordedInvoiceAO(invoice.getId());
			}
		} else {
			invoiceService.rebuildInvoice(invoice, true);
			invoice = invoiceService.retrieveIfNotManaged(invoice);
			if (InvoiceStatusEnum.DRAFT.equals(invoice.getStatus())) { 
				invoiceApi.validateInvoice(invoice.getId(), generateAO, false, true);
			}
		}
		return invoiceService.refreshOrRetrieve(invoice);
	}

	/**
	 * Get mass API limit
	 * @return Mass API limit
	 */
	private Integer getMassApiLimit() {
		final AdvancedSettings massApiLimitSettings = advancedSettingsService.findByCode(INVOICE_API_MASS_ACTION_LIMIT_CODE);
		return advancedSettingsService.parseValue(massApiLimitSettings);
	}

	/**
	 * Validate invoice limit
	 * @param invoiceCount Invoice count
	 * @param massApiLimit Mass API limit
	 */
	private void validateInvoiceLimit(int invoiceCount, int massApiLimit) {
		if (invoiceCount > massApiLimit) {
			throw new BusinessApiException("Number of invoices exceeded action limit");
		}
	}


	/**
	 * Cancel invoices based on filter
	 *
	 * @param cancellationInput Invoice Cancellation Input
	 * @return Cancellation output
	 */
	public CancellationOutput cancelInvoice(InvoiceCancellationInput cancellationInput) {
		final Integer billingRunId = (Integer) ofNullable(cancellationInput.getFilters().get("billingRun.id"))
				.orElseThrow(() -> new BusinessApiException("Billing run ID is required"));
		final Integer massApiLimit = getMassApiLimit();

		Map<String, Object> filters = new HashMap<>(cancellationInput.getFilters());
		filters.put("billingRun.id", billingRunId);
		List<Long> invoicesToProcess = invoiceService.getInvoiceIds(filters);
		validateInvoiceLimit(invoicesToProcess.size(), massApiLimit);
		List<Long> cancelledInvoices = new ArrayList<>();
		Map<Long, String> failures = new HashMap<>();
		if (PROCESS_ALL == cancellationInput.getMode()) {
			invoicesToProcess.forEach(invoiceId
					-> cancelSingleInvoice(invoiceId, cancellationInput, cancelledInvoices, failures));
		} else {
			List<Long> invoicesToExclude = getInvoicesToExclude(invoicesToProcess, failures, cancellationInput);
			invoicesToProcess.removeAll(invoicesToExclude);
			cancelledInvoices.addAll(invoicesToProcess);
			invoiceService.cancelInvoices(invoicesToProcess,
					cancellationInput.getRatedTransactionAction(), billingRunId.longValue());
		}
		return buildCustomResponse(cancelledInvoices, failures, massApiLimit,
				(invoices, failuresResponse, stats) -> ImmutableCancellationOutput.builder()
						.statistics(stats)
						.invoicesCancelled(invoices)
						.invoicesNotCancelled(failuresResponse)
						.build());
	}

	private void cancelSingleInvoice(Long invoiceId, InvoiceCancellationInput cancellationInput,
									 List<Long> cancelledInvoice, Map<Long, String> failures) {
		try {
			Invoice invoice = invoiceService.findById(invoiceId);
			if (VALIDATED == invoice.getStatus() && TRUE.equals(cancellationInput.getFailOnValidatedInvoice())) {
				failures.put(invoice.getId(), "Invoice in status VALIDATED cannot be cancelled");
			} else if (CANCELED == invoice.getStatus() && TRUE.equals(cancellationInput.getFailOnCanceledInvoice())) {
				failures.put(invoice.getId(), "Invoice in status CANCELED cannot be cancelled");
			} else {
				if (VALIDATED != invoice.getStatus() && CANCELED != invoice.getStatus()) {
					invoiceService.cancelInvoiceWithoutDeleteAndRTAction(invoice, cancellationInput.getRatedTransactionAction());
					cancelledInvoice.add(invoice.getId());
				}
			}
		} catch (Exception exception) {
			log.error("Failed to cancel invoice {}, error : {} ", invoiceId, exception.getMessage());
			failures.put(invoiceId, exception.getMessage());
		}
	}

	private List<Long> getInvoicesToExclude(List<Long> invoicesToProcess,
											Map<Long, String> failures, InvoiceCancellationInput cancellationInput) {
		List<Object[]> invoices = invoiceService.getInvoiceToExcludeFromCancellation(invoicesToProcess);
		List<Long> invoicesToExclude = new ArrayList<>();
		invoices.forEach(inv -> {
			if ((VALIDATED.name().equals(inv[1]) && TRUE.equals(cancellationInput.getFailOnValidatedInvoice()))
					|| (CANCELED.name().equals(inv[1]) && TRUE.equals(cancellationInput.getFailOnCanceledInvoice()))) {
				failures.put(((Long) inv[0]), "Invoice in status " + inv[1] + " cannot be cancelled");
			}
			invoicesToExclude.add(((Long) inv[0]));
		});
		return invoicesToExclude;
	}

	/**
	 * Builds a statistics response summarizing success and failure counts
	 *
	 * @param invoices invoices
	 * @param failures Failures map
	 * @param limit    Mass API limit
	 * @return Custom response output
	 */
	private <T> T buildCustomResponse(List<Long> invoices,
									  Map<Long, String> failures,
									  long limit,
									  TriFunction<List<Long>, List<Failure>, ImmutableStatistic, T> responseBuilder) {
		List<Failure> failuresResponse = failures.entrySet()
				.stream()
				.map(entry -> ImmutableFailure.builder()
						.id(entry.getKey())
						.reason(entry.getValue())
						.build())
				.collect(toList());
		long failureCount = failures.size();
		ImmutableStatistic statistics = ImmutableStatistic.builder()
				.total(invoices.size() + failureCount)
				.success((long) invoices.size())
				.fail(failureCount)
				.limit(limit)
				.build();
		return responseBuilder.apply(invoices, failuresResponse, statistics);
	}
}
