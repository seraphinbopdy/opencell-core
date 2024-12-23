package org.meveo.service.billing.impl;



import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Hibernate;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.storage.StorageFactory;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.commons.utils.EjbUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.RegistrationNumber;
import org.meveo.model.admin.Seller;
import org.meveo.model.article.AccountingArticle;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.InvoiceLine;
import org.meveo.model.billing.InvoiceType;
import org.meveo.model.billing.InvoiceTypeEnum;
import org.meveo.model.billing.IsoIcd;
import org.meveo.model.billing.LinkedInvoice;
import org.meveo.model.billing.SubCategoryInvoiceAgregate;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TaxInvoiceAgregate;
import org.meveo.model.billing.UntdidAllowanceCode;
import org.meveo.model.billing.UntdidTaxationCategory;
import org.meveo.model.billing.VatDateCodeEnum;
import org.meveo.model.cpq.commercial.CommercialOrder;
import org.meveo.model.crm.Provider;
import org.meveo.model.payments.CardPaymentMethod;
import org.meveo.model.payments.CheckPaymentMethod;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.DDPaymentMethod;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.shared.Address;
import org.meveo.model.shared.ContactInformation;
import org.meveo.model.shared.Name;
import org.meveo.model.shared.Title;
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.cpq.order.CommercialOrderService;
import org.meveo.service.crm.impl.ProviderService;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressLine;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.BillingReference;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.BranchType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CardAccount;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CountryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CreditNoteLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.DeliveryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.FinancialAccountType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.FinancialInstitution;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.LocationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.OrderReference;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyIdentification;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyLegalEntity;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyName;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyTaxScheme;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PaymentMandate;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PaymentMeans;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PeriodType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PersonType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PriceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ServiceProviderParty;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxScheme;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxSubtotal;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AdditionalStreetName;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AllowanceChargeReason;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AllowanceChargeReasonCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Amount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.BaseAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.BaseQuantity;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.BuildingNumber;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ChargeIndicator;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CityName;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CompanyID;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CompanyLegalForm;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CountrySubentity;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CountrySubentityCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CreditNoteTypeCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CreditedQuantity;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CustomizationID;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Department;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Description;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DescriptionCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DocumentCurrencyCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DueDate;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ElectronicMail;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.EndDate;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.EndpointID;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.FamilyName;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.FirstName;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.HolderName;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ID;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IdentificationCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IndustryClassificationCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InvoiceTypeCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InvoicedQuantity;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IssueDate;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.JobTitle;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Line;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.LineExtensionAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Note;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PayableAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PaymentMeansCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Percent;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PostalZone;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PrepaidAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PriceAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PrimaryAccountNumberID;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ProfileID;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.RegistrationName;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.SalesOrderID;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.StartDate;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.StreetName;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxCurrencyCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxExclusiveAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxExemptionReason;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxExemptionReasonCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxInclusiveAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxTypeCode;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxableAmount;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Telephone;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.UBLVersionID;
import oasis.names.specification.ubl.schema.xsd.creditnote_2.CreditNote;
import oasis.names.specification.ubl.schema.xsd.invoice_2.Invoice;
import oasis.names.specification.ubl.schema.xsd.invoice_2.ObjectFactory;


public class InvoiceUblHelper {
	
	
	private final static InvoiceUblHelper INSTANCE = new InvoiceUblHelper();
	private final static oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ObjectFactory objectFactorycommonBasic;
	private final static oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ObjectFactory objectFactoryCommonAggrement;
	
	private final static UntdidAllowanceCodeService untdidAllowanceCodeService;
	private final static InvoiceAgregateService invoiceAgregateService;
	private final static PaymentTermService paymentTermService;
	private final static EinvoiceSettingService einvoiceSettingService;
	private final static ProviderService providerService;
	private final static CommercialOrderService commercialOrderService;

	private static final String XUN = "XUN";
	public static final String ISO_IEC_6523 = "ISO/IEC 6523";
	public static final String SIRET = "0009";
	public static final String SIREN = "0002";
	private static final String IV = "IV";
	private static final String UNCL_3035 = "UNCL 3035";
	
	private Map<String, String> descriptionMap = new HashMap<>();
	private static int rounding = 2;
	
	static {
		objectFactorycommonBasic = new oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ObjectFactory();
		objectFactoryCommonAggrement = new oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ObjectFactory();
		invoiceAgregateService = (InvoiceAgregateService) EjbUtils.getServiceInterface(InvoiceAgregateService.class.getSimpleName());
		untdidAllowanceCodeService = (UntdidAllowanceCodeService) EjbUtils.getServiceInterface(UntdidAllowanceCodeService.class.getSimpleName());
		paymentTermService = (PaymentTermService) EjbUtils.getServiceInterface(PaymentTermService.class.getSimpleName());
		einvoiceSettingService = (EinvoiceSettingService) EjbUtils.getServiceInterface(EinvoiceSettingService.class.getSimpleName());
		providerService = (ProviderService) EjbUtils.getServiceInterface(ProviderService.class.getSimpleName());
		commercialOrderService = (CommercialOrderService) EjbUtils.getServiceInterface(CommercialOrderService.class.getSimpleName());
		Provider provider = providerService.getProvider();
		if(provider != null) {
			rounding = provider.getInvoiceRounding();
	}
	}
	
	private InvoiceUblHelper(){}
	
	public static InvoiceUblHelper getInstance(){ return  INSTANCE; }
	
	public Path createInvoiceUBL(org.meveo.model.billing.Invoice invoice){
		Invoice invoiceXml = new ObjectFactory().createInvoice();
		InvoiceType invoiceType = invoice.getInvoiceType();
		CreditNote creditNote = null;
		final String CREDIT_NOTE_INVOICE_TYPE = "381";
		if(invoiceType != null && invoiceType.getUntdidInvoiceCodeType() != null  && invoiceType.getUntdidInvoiceCodeType().getCode().contentEquals(CREDIT_NOTE_INVOICE_TYPE)) {
			creditNote = new oasis.names.specification.ubl.schema.xsd.creditnote_2.ObjectFactory().createCreditNote();
		}
		String invoiceLanguageCode = invoice.getBillingAccount().getTradingLanguage() != null ? invoice.getBillingAccount().getTradingLanguage().getLanguage() != null ?
																								invoice.getBillingAccount().getTradingLanguage().getLanguage().getLanguageCode() : null : null;
		setUblExtension(invoiceXml, creditNote);
		setAllowanceCharge(invoice, invoiceXml, creditNote);
		
		
		if (CollectionUtils.isNotEmpty(invoice.getInvoiceAgregates())) {
			List<TaxInvoiceAgregate> taxInvoiceAgregates = invoice.getInvoiceAgregates().stream().filter(invAgg -> "T".equals(invAgg.getDescriminatorValue()))
					.map(invAgg -> (TaxInvoiceAgregate) invAgg)
					.collect(Collectors.toList());
			setTaxTotal(taxInvoiceAgregates, invoice.getAmountTax(), invoiceXml, creditNote,  invoice.getTradingCurrency() != null ? invoice.getTradingCurrency().getCurrencyCode() : null);
		}
		setPaymentTerms(invoice, invoiceXml, creditNote, invoice.getInvoiceType(), invoiceLanguageCode);
		setAccountingSupplierParty(invoice.getSeller(), invoiceXml, creditNote, invoiceLanguageCode);
		setAccountingCustomerParty(invoice.getBillingAccount(), invoiceXml, creditNote);
		setPaymentMeans(invoice.getBillingAccount().getCustomerAccount().getPreferredPaymentMethod(), invoiceXml, creditNote);
		String curreny = invoice.getTradingCurrency() != null ? invoice.getTradingCurrency().getCurrencyCode():null;
		BigDecimal amountWithoutTax = invoice.getAmountWithoutTax();
		BigDecimal amountWithTax = invoice.getAmountWithTax();
		BigDecimal totalPrepaidAmount = invoice.getLinkedInvoices().stream().filter(li -> li.getType() == InvoiceTypeEnum.ADVANCEMENT_PAYMENT).map(LinkedInvoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal lineExtensionAmount = invoice.getInvoiceLines()
												.stream()
												.map(InvoiceLine::getAmountWithTax)
												.reduce(BigDecimal.ZERO, BigDecimal::add);
		var profileID = getProfileID(invoice.getInvoiceLines());
		BigDecimal payableAmount = invoice.getNetToPay();
		if (creditNote != null) {
			setGeneralInfo(invoice, creditNote);
			setBillingReference(invoice, creditNote);
			setOrderReference(invoice, creditNote);
			setOrderReferenceId(invoice, invoiceXml);
			setInvoiceLine(invoice.getInvoiceLines(), creditNote, invoiceLanguageCode);
			creditNote.setLegalMonetaryTotal(setTaxExclusiveAmount(totalPrepaidAmount, curreny, amountWithoutTax , amountWithTax, lineExtensionAmount, payableAmount));
			creditNote.setProfileID(profileID);
		} else {
			setGeneralInfo(invoice, invoiceXml);
			//setBillingReference(invoice, invoiceXml);
			setOrderReference(invoice, invoiceXml);
			setOrderReferenceId(invoice, invoiceXml);
			setInvoiceLine(invoice.getInvoiceLines(), invoiceXml, invoiceLanguageCode);
			invoiceXml.setLegalMonetaryTotal(setTaxExclusiveAmount(totalPrepaidAmount, curreny, amountWithoutTax , amountWithTax, lineExtensionAmount, payableAmount));
			var commercialorderIds = invoice.getInvoiceLines().stream().map(InvoiceLine::getCommercialOrder).filter(Objects::nonNull)
					.collect(Collectors.toSet());
			setBillingReferenceForInvoice(commercialorderIds, invoiceXml);
			invoiceXml.setProfileID(profileID);
		}
		
		
		// check directory if exist
		ParamBean paramBean = ParamBean.getInstance();
		File ublDirectory = new File(paramBean.getChrootDir("") + File.separator + paramBean.getProperty("meveo.ubl.directory", "/ubl"));
		if (!StorageFactory.existsDirectory(ublDirectory)) {
			StorageFactory.createDirectory(ublDirectory);
		}
		String invoiceTypeAdj = creditNote != null ? "invoice_ADJ_" : "invoice_";
		File ublInvoiceFileName = new File(	ublDirectory.getAbsolutePath() + File.separator +  invoiceTypeAdj +  invoice.getInvoiceNumber() + "_"	+ new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".xml");
		Path pathCreatedFile = null;

		try {
			pathCreatedFile = Files.createFile(Paths.get(ublInvoiceFileName.getAbsolutePath()));
			toXml(creditNote != null ? creditNote : invoiceXml, ublInvoiceFileName);
		} catch (Exception e) {
			throw new BusinessException(e);
		}

		return pathCreatedFile;
	}

	/**
	 * Set the billing reference for the invoice
	 * @param invoice the invoice
	 * @param invoiceXml the invoice xml
	 */
	private void setOrderReferenceId(org.meveo.model.billing.Invoice invoice, Invoice invoiceXml) {
		if(StringUtils.isNotBlank(invoice.getExternalPurchaseOrderNumber())) {
			if (invoiceXml.getOrderReference() == null) {
				OrderReference orderReference = objectFactoryCommonAggrement.createOrderReference();
				ID id  = objectFactorycommonBasic.createID();
				id.setValue(invoice.getExternalPurchaseOrderNumber());
				orderReference.setID(id);
				invoiceXml.setOrderReference(orderReference);
			} else {
				ID id  = objectFactorycommonBasic.createID();
				id.setValue(invoice.getExternalPurchaseOrderNumber());
				invoiceXml.getOrderReference().setID(id);
			}
		}
	}


	public void toXml(Object invoiceXml, File absoluteFileName) throws JAXBException {
		if(absoluteFileName == null || !absoluteFileName.isFile()) {
			throw new BusinessException("The file doesn't exist");
		}
		JAXBContext context = JAXBContext.newInstance(invoiceXml.getClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		//marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2");
		marshaller.marshal(invoiceXml, absoluteFileName);
	}
	
	private void setPaymentTerms(org.meveo.model.billing.Invoice source, Invoice target, CreditNote creditNote, InvoiceType invoiceType, String invoiceLanguageCode) {
		if(invoiceType != null && invoiceType.getUntdidInvoiceCodeType() != null && invoiceType.getUntdidInvoiceCodeType().getCode().equals("380")) {
			PaymentTermsType paymentTermsType = objectFactoryCommonAggrement.createPaymentTermsType();
			paymentTermService.findAllEnabledPaymentTerm().forEach(pt -> {
				String noteValue = pt.getDescriptionI18n().get(invoiceLanguageCode);
				if(noteValue != null) {
					Note note = objectFactorycommonBasic.createNote();
					note.setValue(ValueExpressionWrapper.evaluateToStringMultiVariable(noteValue, "invoice", source, "inv", source));
					paymentTermsType.getNotes().add(note);
				}
			});
			if(creditNote == null)
				target.getPaymentTerms().add(paymentTermsType);
			else{
				creditNote.getPaymentTerms().add(paymentTermsType);
			}
		}
	}
	
	private void setUblExtension(Invoice target, CreditNote creditNote){
		UBLVersionID ublVersionID = objectFactorycommonBasic.createUBLVersionID();
		ublVersionID.setValue("2.2");
		if(creditNote != null){
			creditNote.setUBLVersionID(ublVersionID);
		}else{
			target.setUBLVersionID(ublVersionID);
		}
		
	}
	
	private static void setGeneralInfo(org.meveo.model.billing.Invoice source, Invoice target){
		if(source.getInvoiceType() != null && source.getInvoiceType().getUntdidInvoiceCodeType() != null) {
			InvoiceType invoiceType = source.getInvoiceType();
			InvoiceTypeCode invoiceTypeCode = objectFactorycommonBasic.createInvoiceTypeCode();
			invoiceTypeCode.setListID("UNCL 1001");
			invoiceTypeCode.setValue(invoiceType.getUntdidInvoiceCodeType().getCode());
			target.setInvoiceTypeCode(invoiceTypeCode);
		}
		ID id = objectFactorycommonBasic.createID();
		id.setValue(source.getInvoiceNumber());
		target.setID(id);
		
		IssueDate issueDate = getIssueDate(source.getInvoiceDate());
		target.setIssueDate(issueDate);
		
		if(source.getStartDate() != null){
			PeriodType periodType = objectFactoryCommonAggrement.createPeriodType();
			StartDate startDate = objectFactorycommonBasic.createStartDate();
			EndDate endDate = objectFactorycommonBasic.createEndDate();
			
			startDate.setValue(toXmlDate(source.getStartDate()));
			endDate.setValue(toXmlDate(source.getEndDate()));
			periodType.setStartDate(startDate);
			periodType.setEndDate(endDate);
			VatDateCodeEnum vatDateCode = einvoiceSettingService.findEinvoiceSetting().getVatDateCode();
			DescriptionCode descriptionCode = objectFactorycommonBasic.createDescriptionCode();
			descriptionCode.setValue(String.valueOf(vatDateCode.getPaidToDays()));
			periodType.getDescriptionCodes().add(descriptionCode);
			target.getInvoicePeriods().add(periodType);
		}

		// Invoice/Delivery/DeliveryLocation/Address
		if (source.getBillingAccount() != null && source.getBillingAccount().getUsersAccounts() != null
				&& source.getBillingAccount().getUsersAccounts().size() == 1 && source.getBillingAccount().getUsersAccounts().get(0).getAddress() != null) {
			target.getDeliveries().add(getDeliveryType(source));
		}

		if(StringUtils.isNotBlank(source.getComment())){
		Note note = objectFactorycommonBasic.createNote();
		note.setValue(source.getComment());
		target.getNotes().add(note);
		}
		CustomizationID customizationID = objectFactorycommonBasic.createCustomizationID();
		customizationID.setValue("urn:cen.eu:en16931:2017#conformant#urn:ubl.eu:1p0:extended-ctc-fr");
		target.setCustomizationID(customizationID);

		setTaxCurrencyCodeAndDocumentCurrencyCode(objectFactorycommonBasic, source, target);
		
		DueDate dueDate = objectFactorycommonBasic.createDueDate();
		dueDate.setValue(toXmlDate(source.getDueDate()));
		target.setDueDate(dueDate);
		
		var monetaryTotalType = objectFactoryCommonAggrement.createMonetaryTotalType();
		var taxInclusiveAmount = objectFactorycommonBasic.createTaxInclusiveAmount();
		final String currencyId = source.getTradingCurrency() != null ? source.getTradingCurrency().getCurrencyCode() : null;
		taxInclusiveAmount.setCurrencyID(currencyId);
		taxInclusiveAmount.setValue(source.getAmountWithTax().setScale(rounding, RoundingMode.HALF_UP));
		monetaryTotalType.setTaxInclusiveAmount(taxInclusiveAmount);
		var allowanceTotalAmount = objectFactorycommonBasic.createAllowanceTotalAmount();
		allowanceTotalAmount.setCurrencyID(currencyId);
		allowanceTotalAmount.setValue(source.getDiscountAmount());
		monetaryTotalType.setAllowanceTotalAmount(allowanceTotalAmount);
		PayableAmount payableAmount = objectFactorycommonBasic.createPayableAmount();
		payableAmount.setValue(source.getAmountWithTax());
		payableAmount.setCurrencyID(currencyId);
		monetaryTotalType.setPayableAmount(payableAmount);
		target.setLegalMonetaryTotal(monetaryTotalType);
	}

	private static void setGeneralInfo(org.meveo.model.billing.Invoice source, CreditNote target){
		if(source.getInvoiceType() != null && source.getInvoiceType().getUntdidInvoiceCodeType() != null) {
			InvoiceType invoiceType = source.getInvoiceType();
			CreditNoteTypeCode invoiceTypeCode = objectFactorycommonBasic.createCreditNoteTypeCode();
			invoiceTypeCode.setListID("UNCL 1001");
			invoiceTypeCode.setValue(invoiceType.getUntdidInvoiceCodeType().getCode());
			target.setCreditNoteTypeCode(invoiceTypeCode);
		}
		ID id = objectFactorycommonBasic.createID();
		id.setValue(source.getInvoiceNumber());
		target.setID(id);
		
		IssueDate issueDate = getIssueDate(source.getInvoiceDate());
		target.setIssueDate(issueDate);
		
		if(source.getStartDate() != null){
			PeriodType periodType = objectFactoryCommonAggrement.createPeriodType();
			StartDate startDate = objectFactorycommonBasic.createStartDate();
			EndDate endDate = objectFactorycommonBasic.createEndDate();
			
			startDate.setValue(toXmlDate(source.getStartDate()));
			endDate.setValue(toXmlDate(source.getEndDate()));
			periodType.setStartDate(startDate);
			periodType.setEndDate(endDate);
			target.getInvoicePeriods().add(periodType);
		}

		// Invoice/Delivery/DeliveryLocation/Address
		if (source.getBillingAccount() != null && source.getBillingAccount().getUsersAccounts() != null && source.getBillingAccount().getUsersAccounts().size() == 1 &&
			 source.getBillingAccount().getUsersAccounts().get(0).getAddress() != null) {
			target.getDeliveries().add(getDeliveryType(source));
		}
		
		if(StringUtils.isNotBlank(source.getComment())){
		Note note = objectFactorycommonBasic.createNote();
		note.setValue(source.getComment());
		target.getNotes().add(note);
		}
		
		
		CustomizationID customizationID = objectFactorycommonBasic.createCustomizationID();
		customizationID.setValue("urn:cen.eu:en16931:2017#conformant#urn:ubl.eu:1p0:extended-ctc-fr");
		target.setCustomizationID(customizationID);
		
		setTaxCurrencyCodeAndDocumentCurrencyCode(objectFactorycommonBasic, source, target);
		
		
		var monetaryTotalType = objectFactoryCommonAggrement.createMonetaryTotalType();
		var taxInclusiveAmount = objectFactorycommonBasic.createTaxInclusiveAmount();
		final String currencyId = source.getTradingCurrency() != null ? source.getTradingCurrency().getCurrencyCode() : null;
		taxInclusiveAmount.setCurrencyID(currencyId);
		taxInclusiveAmount.setValue(source.getAmountWithTax().setScale(rounding, RoundingMode.HALF_UP));
		monetaryTotalType.setTaxInclusiveAmount(taxInclusiveAmount);
		var allowanceTotalAmount = objectFactorycommonBasic.createAllowanceTotalAmount();
		allowanceTotalAmount.setCurrencyID(currencyId);
		allowanceTotalAmount.setValue(source.getDiscountAmount());
		monetaryTotalType.setAllowanceTotalAmount(allowanceTotalAmount);
		PayableAmount payableAmount = objectFactorycommonBasic.createPayableAmount();
		payableAmount.setValue(source.getAmountWithTax());
		payableAmount.setCurrencyID(currencyId);
		monetaryTotalType.setPayableAmount(payableAmount);
		target.setLegalMonetaryTotal(monetaryTotalType);
	}

	/**
	 * Set the payment means for the invoice
	 * @param paymentMethod the payment method
	 * @param target the invoice
	 * @param creditNote The credit note
	 */
	private void setPaymentMeans(PaymentMethod paymentMethod, Invoice target, CreditNote creditNote){
		PaymentMeans paymentMeans = objectFactoryCommonAggrement.createPaymentMeans();

		if(paymentMethod != null) {
			PaymentMeansCode paymentMeansCode = objectFactorycommonBasic.createPaymentMeansCode();
			paymentMeansCode.setListID("UN/ECE 4461");
			paymentMeansCode.setListAgencyID("NES");
			paymentMeansCode.setListAgencyName("Northern European Subset");
			paymentMeansCode.setListName("Payment Means");
			paymentMeansCode.setValue(paymentMethod.getPaymentMeans() != null ? paymentMethod.getPaymentMeans().getCode() : "59");
			paymentMeans.setPaymentMeansCode(paymentMeansCode);
			
			
		}
		
		//check
		if(Hibernate.unproxy(paymentMethod) instanceof CheckPaymentMethod) {
			ID payerFinancialAccountId = objectFactorycommonBasic.createID();
			payerFinancialAccountId.setValue("no IBAN");
			FinancialAccountType payerFinancialAccount = objectFactoryCommonAggrement.createFinancialAccountType();
			payerFinancialAccount.setID(payerFinancialAccountId);
			paymentMeans.setPayerFinancialAccount(payerFinancialAccount);
		}

		// DirectDebit
		if(Hibernate.unproxy(paymentMethod) instanceof DDPaymentMethod) {
			DDPaymentMethod bank = (DDPaymentMethod) Hibernate.unproxy(paymentMethod);
			setPaymentMeansForSEPA(bank, paymentMeans);
		}

		if(paymentMeans.getPaymentMeansCode() != null || paymentMeans.getPayeeFinancialAccount() != null){
			if(creditNote == null) {
				target.getPaymentMeans().add(paymentMeans);
			} else {
				creditNote.getPaymentMeans().add(paymentMeans);
			}
		}
	}

	/**
	 * Set the payment means for credit card payment method
	 * @param paymentMethod the payment method
	 * @param paymentMeans the payment means
	 */
	// We can use this method in the future when we will have only the credit card payment method
	private static void setPaymentMeansForCreditCard(PaymentMethod paymentMethod, PaymentMeans paymentMeans) {
		CardPaymentMethod card = (CardPaymentMethod) Hibernate.unproxy(paymentMethod);

		if (paymentMeans.getPaymentMeansCode() == null) {
			PaymentMeansCode paymentMeansCode = objectFactorycommonBasic.createPaymentMeansCode();
			paymentMeans.setPaymentMeansCode(paymentMeansCode);
		}

		paymentMeans.getPaymentMeansCode().setListID("UN/ECE 4461");
		paymentMeans.getPaymentMeansCode().setName("CreditCard");

		// PaymentMeans/CardAccount
		CardAccount cardAccount = objectFactoryCommonAggrement.createCardAccount();

		// PaymentMeans/CardAccount/PrimaryAccountNumberID
		if (StringUtils.isNotBlank(card.getHiddenCardNumber())) {
			PrimaryAccountNumberID primaryAccountNumberID = objectFactorycommonBasic.createPrimaryAccountNumberID();
			primaryAccountNumberID.setValue(card.getHiddenCardNumber());
			cardAccount.setPrimaryAccountNumberID(primaryAccountNumberID);
		}

		// PaymentMeans/CardAccount/HolderName
		if (StringUtils.isNotBlank(card.getOwner())) {
			HolderName holderName = objectFactorycommonBasic.createHolderName();
			holderName.setValue(card.getOwner());
			cardAccount.setHolderName(holderName);
		}

		paymentMeans.setCardAccount(cardAccount);
	}

	/**
	 * Set the payment means for direct debit payment method
	 * @param bank the bank
	 * @param paymentMeans the payment means
	 */
	// We can use this method in the future when we will have only the direct debit payment method
	private static void setPaymentMeansForDirectDebit(DDPaymentMethod bank, PaymentMeans paymentMeans) {
		paymentMeans.getPaymentMeansCode().setName("DirectDebit");
		paymentMeans.getPaymentMeansCode().setValue("49");
		FinancialAccountType financialAccountType = objectFactoryCommonAggrement.createFinancialAccountType();

		// PaymentMeans/PayeeFinancialAccount/ID
		if(StringUtils.isNotBlank(bank.getBankCoordinates().getIban()) || StringUtils.isNotBlank(bank.getBankCoordinates().getBankId())){
			ID payeeFinancialAccountId = objectFactorycommonBasic.createID();
			payeeFinancialAccountId.setValue(StringUtils.isNotBlank(bank.getBankCoordinates().getIban()) ? bank.getBankCoordinates().getIban() : bank.getBankCoordinates().getBankId());
			financialAccountType.setID(payeeFinancialAccountId);
		}

		// PaymentMeans/PayeeFinancialAccount/Name
		if(StringUtils.isNotBlank(bank.getBankCoordinates().getAccountOwner())){
			oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Name name = objectFactorycommonBasic.createName();
			name.setValue(bank.getBankCoordinates().getAccountOwner());
			financialAccountType.setName(name);
		}

		// PaymentMeans/PayeeFinancialAccount/FinancialInstitutionBranch/FinancialInstitution/ID
		if(StringUtils.isNotBlank(bank.getBankCoordinates().getBankCode())){
			BranchType branchType = objectFactoryCommonAggrement.createBranchType();
			FinancialInstitution financialInstitution = objectFactoryCommonAggrement.createFinancialInstitution();
			ID financialInstitutionId = objectFactorycommonBasic.createID();

			financialInstitutionId.setValue(bank.getBankCoordinates().getBankCode());
			financialInstitution.setID(financialInstitutionId);
			branchType.setFinancialInstitution(financialInstitution);
			financialAccountType.setFinancialInstitutionBranch(branchType);
		}

		// PaymentMeans/PayeeFinancialAccount/FinancialInstitutionBranch/ID
		if(StringUtils.isNotBlank(bank.getBankCoordinates().getIban()) || StringUtils.isNotBlank(bank.getBankCoordinates().getBankId())){
			ID financialInstitutionBranchId = objectFactorycommonBasic.createID();
			financialInstitutionBranchId.setValue(StringUtils.isNotBlank(bank.getBankCoordinates().getIban()) ? bank.getBankCoordinates().getIban() : bank.getBankCoordinates().getBankId());
			if (financialAccountType.getFinancialInstitutionBranch() == null) {
				BranchType branchType = objectFactoryCommonAggrement.createBranchType();
				financialAccountType.setFinancialInstitutionBranch(branchType);
			}

			financialAccountType.getFinancialInstitutionBranch().setID(financialInstitutionBranchId);
		}

		paymentMeans.setPayeeFinancialAccount(financialAccountType);
	}

	/**
	 * Set the payment means for SEPA payment method
	 * @param bank the bank
	 * @param paymentMeans the payment means
	 */
	private static void setPaymentMeansForSEPA(DDPaymentMethod bank, PaymentMeans paymentMeans) {
		if (paymentMeans.getPaymentMeansCode() == null) {
			PaymentMeansCode paymentMeansCode = objectFactorycommonBasic.createPaymentMeansCode();
			paymentMeans.setPaymentMeansCode(paymentMeansCode);
		}

		paymentMeans.getPaymentMeansCode().setListID("UN/ECE 4461");
		paymentMeans.getPaymentMeansCode().setName("SEPA");
		paymentMeans.getPaymentMeansCode().setValue("59");

		Provider provider = providerService.getProvider();
		
		PaymentMandate paymentMandate = objectFactoryCommonAggrement.createPaymentMandate();
		// PaymentMeans/PaymentMandate
		if (StringUtils.isNotBlank(bank.getMandateIdentification())) {
			ID mandateID = objectFactorycommonBasic.createID();
			mandateID.setValue(bank.getMandateIdentification());
			paymentMandate.setID(mandateID);
			
		}

		// PaymentMeans/PayeeFinancialAccount
		if (bank.getBankCoordinates() != null) {
			FinancialAccountType payerFinancialAccount = objectFactoryCommonAggrement.createFinancialAccountType();
			ID payerFinancialAccountId = objectFactorycommonBasic.createID();
			payerFinancialAccountId.setValue(bank.getBankCoordinates().getIban());
			payerFinancialAccount.setID(payerFinancialAccountId);
			paymentMandate.setPayerFinancialAccount(payerFinancialAccount);
		}
		paymentMeans.setPaymentMandate(paymentMandate);

		// PaymentMeans/PayeeFinancialInstitution
		if (provider.getBankCoordinates() != null) {
			FinancialAccountType payeeFinancialInstitution = objectFactoryCommonAggrement.createFinancialAccountType();
			ID payeeFinancialInstitutionId = objectFactorycommonBasic.createID();
			payeeFinancialInstitutionId.setValue(provider.getBankCoordinates().getIban());
			payeeFinancialInstitution.setID(payeeFinancialInstitutionId);
			paymentMeans.setPayeeFinancialAccount(payeeFinancialInstitution);
		}
	}

	private void setInvoiceLine(List<InvoiceLine> invoiceLines, Invoice target, String invoiceLanguageCode){
		invoiceLines.forEach(invoiceLine -> {
			if(invoiceLine.getAccountingArticle() == null || (invoiceLine.getAccountingArticle().getAllowanceCode() != null && !"Standard".equalsIgnoreCase(invoiceLine.getAccountingArticle().getAllowanceCode().getDescription()))) {
				return;
			}
			// InvoiceLine/ Item/ ClassifiedTaxCategory/ Percent
			InvoiceLineType invoiceLineType = objectFactoryCommonAggrement.createInvoiceLineType();
			ItemType itemType = getItemTyp(invoiceLine, invoiceLanguageCode);
			// InvoiceLine/ InvoicedQuantity
			InvoicedQuantity invoicedQuantity = objectFactorycommonBasic.createInvoicedQuantity();
			invoicedQuantity.setValue(invoiceLine.getQuantity().setScale(rounding, RoundingMode.HALF_UP));
			invoicedQuantity.setUnitCode(XUN);
			invoiceLineType.setInvoicedQuantity(invoicedQuantity);
			setPriceAndCreditLine(invoiceLine, invoiceLineType, itemType);
			invoiceLineType.getInvoicePeriods().add(setPeriodTypeInvoiceLine(invoiceLine.getInvoice(), invoiceLine));
			target.getInvoiceLines().add(invoiceLineType);
		});
	}
	private void setInvoiceLine(List<InvoiceLine> invoiceLines, CreditNote target, String invoiceLanguageCode){
		invoiceLines.forEach(invoiceLine -> {
			if(invoiceLine.getAccountingArticle() == null || (invoiceLine.getAccountingArticle().getAllowanceCode() != null && !"Standard".equalsIgnoreCase(invoiceLine.getAccountingArticle().getAllowanceCode().getDescription()))) {
				return;
			}
			// InvoiceLine/ Item/ ClassifiedTaxCategory/ Percent
			CreditNoteLineType invoiceLineType = objectFactoryCommonAggrement.createCreditNoteLineType();
			ItemType itemType = getItemTyp(invoiceLine, invoiceLanguageCode);
			// InvoiceLine/ InvoicedQuantity
			CreditedQuantity invoicedQuantity = objectFactorycommonBasic.createCreditedQuantity();
			invoicedQuantity.setUnitCode(XUN);
			invoicedQuantity.setValue(invoiceLine.getQuantity().setScale(rounding, RoundingMode.HALF_UP));
			invoiceLineType.setCreditedQuantity(invoicedQuantity);
			setPriceAndCreditLine(invoiceLine, invoiceLineType, itemType);
			invoiceLineType.getInvoicePeriods().add(setPeriodTypeInvoiceLine(invoiceLine.getInvoice(), invoiceLine));
			target.getCreditNoteLines().add(invoiceLineType);
		});
	}
	
	private void setPriceAndCreditLine(InvoiceLine invoiceLine, Object invoiceLineType, ItemType itemType) {
		// InvoiceLine/ Price/ BaseQuantity
		PriceType priceType = getPriceType(invoiceLine);
		// InvoiceLine/ LineExtensionAmount
		LineExtensionAmount lineExtensionAmount = getLineExtensionAmount(invoiceLine);
		// InvoiceLine/ Note
		ID id = objectFactorycommonBasic.createID();
		id.setValue(invoiceLine.getId().toString());
		Note note = objectFactorycommonBasic.createNote();
		note.setValue(invoiceLine.getLabel());
		if(invoiceLineType instanceof  CreditNoteLineType){
			CreditNoteLineType CreditlineType = (CreditNoteLineType) invoiceLineType;
			CreditlineType.setID(id);
			CreditlineType.setPrice(priceType);
			CreditlineType.setLineExtensionAmount(lineExtensionAmount);
			CreditlineType.getNotes().add(note);
			CreditlineType.setItem(itemType);
		}else{
			InvoiceLineType lineType = (InvoiceLineType) invoiceLineType;
			lineType.setID(id);
			lineType.setPrice(priceType);
			lineType.setLineExtensionAmount(lineExtensionAmount);
			lineType.getNotes().add(note);
			lineType.setItem(itemType);
		}
	}
	
	private static LineExtensionAmount getLineExtensionAmount(InvoiceLine invoiceLine) {
		LineExtensionAmount lineExtensionAmount = objectFactorycommonBasic.createLineExtensionAmount();
		lineExtensionAmount.setCurrencyID(invoiceLine.getInvoice().getTradingCurrency().getCurrencyCode());
		lineExtensionAmount.setValue(invoiceLine.getAmountWithTax().setScale(rounding, RoundingMode.HALF_UP));
		return lineExtensionAmount;
	}
	
	private ItemType getItemTyp(InvoiceLine invoiceLine, String invoiceLanguageCode){
		ItemType itemType = objectFactoryCommonAggrement.createItemType();
		TaxCategoryType taxCategoryType = objectFactoryCommonAggrement.createTaxCategoryType();
		
		//id.setValue();
		String untdidTaxationCategory = invoiceLine.getTax() != null ? invoiceLine.getTax().getUntdidTaxationCategory() != null ?  invoiceLine.getTax().getUntdidTaxationCategory().getCode() : null: null;
		if(untdidTaxationCategory != null) {
			ID id = objectFactorycommonBasic.createID();
			id.setSchemeID("UN/ECE 5305");
			id.setSchemeAgencyID("6");
			id.setValue(untdidTaxationCategory);
			taxCategoryType.setID(id);
		}
		Percent percent = objectFactorycommonBasic.createPercent();
		percent.setValue(invoiceLine.getTaxRate().setScale(rounding, RoundingMode.HALF_UP));
		taxCategoryType.setPercent(percent);
		// InvoiceLine/ Item/ ClassifiedTaxCategory/TaxScheme/TaxTypeCode
		TaxScheme taxScheme = objectFactoryCommonAggrement.createTaxScheme();
		TaxTypeCode taxTypeCode = objectFactorycommonBasic.createTaxTypeCode();
		taxTypeCode.setValue(invoiceLine.getTax().getCode());
		taxScheme.setTaxTypeCode(taxTypeCode);
		taxCategoryType.setTaxScheme(taxScheme);
		itemType.getClassifiedTaxCategories().add(taxCategoryType);
		//InvoiceLine/ Item/ Description
		Description description = objectFactorycommonBasic.createDescription();
		
		// InvoiceLine/ Item/ Name
		oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Name name = objectFactorycommonBasic.createName();
		name.setValue(invoiceLine.getLabel());
		description.setValue(invoiceLine.getLabel());
		itemType.setName(name);
		itemType.getDescriptions().add(description);

		return itemType;
	}
	
	private PriceType getPriceType(InvoiceLine invoiceLine){
		PriceType priceType = objectFactoryCommonAggrement.createPriceType();
		BaseQuantity baseQuantity = objectFactorycommonBasic.createBaseQuantity();
		baseQuantity.setValue(new BigDecimal(1));
		priceType.setBaseQuantity(baseQuantity);
		
		final String currencyCode = invoiceLine.getInvoice().getTradingCurrency().getCurrencyCode();
		// InvoiceLine/ Price/ PriceAmount
		PriceAmount priceAmount = objectFactorycommonBasic.createPriceAmount();
		priceAmount.setCurrencyID(currencyCode);
		priceAmount.setValue(invoiceLine.getUnitPrice().setScale(rounding, RoundingMode.HALF_UP));
		priceType.setPriceAmount(priceAmount);
		return priceType;
	}
	private void setAccountingCustomerParty(BillingAccount billingAccount, Invoice target, CreditNote creditNote) {
        // AccountingCustomerParty/Party
        CustomerPartyType customerPartyType = objectFactoryCommonAggrement.createCustomerPartyType();
        PartyType partyType = objectFactoryCommonAggrement.createPartyType();

        Address address = billingAccount.getAddress();
        if (billingAccount.getAddress() != null) {
            AddressType postalAddress = objectFactoryCommonAggrement.createAddressType();
            // AccountingCustomerParty/Party/PostalAddress/CityName
            if (StringUtils.isNotBlank(address.getCity())) {
                CityName cityName = objectFactorycommonBasic.createCityName();
                cityName.setValue(address.getCity());
                postalAddress.setCityName(cityName);
            }
            //AccountingCustomerParty/Party/PostalAddress/PostalZone
            if (StringUtils.isNotBlank(address.getZipCode())) {
                PostalZone postalZone = objectFactorycommonBasic.createPostalZone();
                postalZone.setValue(address.getZipCode());
                postalAddress.setPostalZone(postalZone);
            }
            //AccountingCustomerParty/Party/PostalAddress/Country
            if (address.getCountry() != null) {
                CountryType countryType = objectFactoryCommonAggrement.createCountryType();
                IdentificationCode identificationCode = objectFactorycommonBasic.createIdentificationCode();
				identificationCode.setListID("ISO3166-1");
				identificationCode.setListAgencyID("6");
                identificationCode.setValue(address.getCountry().getCode());
                countryType.setIdentificationCode(identificationCode);
                postalAddress.setCountry(countryType);
            }
            //AccountingCustomerParty/Party/PostalAddress/PostalAddress
            if (StringUtils.isNotBlank(address.getAddress1())) {
                StreetName streetName = objectFactorycommonBasic.createStreetName();
                streetName.setValue(address.getAddress1());
                postalAddress.setStreetName(streetName);
            }
            partyType.setPostalAddress(postalAddress);
        }
        // AccountingCustomerParty/Party/PartyTaxScheme/CompanyID
        if (StringUtils.isNotBlank(billingAccount.getVatNo()) || (billingAccount.getSeller() != null && billingAccount.getSeller().getVatNo() != null)) {
            // AccountingSupplierParty/Party/PartyTaxScheme/CompanyID
            var vatNo = StringUtils.isNotBlank(billingAccount.getVatNo()) ? billingAccount.getVatNo() : billingAccount.getSeller().getVatNo();
            PartyTaxScheme partyTaxScheme = objectFactoryCommonAggrement.createPartyTaxScheme();
            CompanyID companyID = objectFactorycommonBasic.createCompanyID();
            companyID.setSchemeAgencyID("ZZZ");
            companyID.setSchemeID(address != null && address.getCountry() != null ? address.getCountry().getCountryCode() : null);
            companyID.setValue(vatNo);
            partyTaxScheme.setCompanyID(companyID);
            partyTaxScheme.setTaxScheme(getTaxSheme());
            partyType.getPartyTaxSchemes().add(partyTaxScheme);
            //TODO : AccountingCustomerParty/Party/PartyTaxScheme/TaxScheme/ID ask @Emmanuel for this field INTRD-12578
        }
        // AccountingCustomerParty/Party/PartyLegalEntity
        PartyLegalEntity partyLegalEntity = objectFactoryCommonAggrement.createPartyLegalEntity();

		PartyName partyName = objectFactoryCommonAggrement.createPartyName();
		oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Name name = objectFactorycommonBasic.createName();
		name.setValue(StringUtils.isNotBlank(billingAccount.getCustomerAccount().getDescription()) ? billingAccount.getCustomerAccount().getDescription() : "-");
		partyName.setName(name);
		partyType.getPartyNames().add(partyName);

        // AccountingCustomerParty/Party/PartyLegalEntity/RegistrationName
        if (StringUtils.isNotBlank(billingAccount.getDescription())) {
            RegistrationName registrationName = objectFactorycommonBasic.createRegistrationName();
            registrationName.setValue(billingAccount.getDescription());
            partyLegalEntity.setRegistrationName(registrationName);
        }
        if (billingAccount.getAddress() != null) {
            // AccountingCustomerParty/Party/PartyLegalEntity/RegistrationAddress
            AddressType addressType = getRegistrationAddress(billingAccount.getAddress());
            partyLegalEntity.setRegistrationAddress(addressType);
        }

		addPartyIdentifications(billingAccount.getRegistrationNumbers(), partyType);
        partyType.getPartyLegalEntities().add(partyLegalEntity);

        // AccountingCustomerParty/Party/PartyLegalEntity/Contact
        // todo : Check this contact namespace is correct
        if (billingAccount.getContactInformation() != null) {
            ContactType contactType = getContactInformation(billingAccount.getContactInformation());
			if(billingAccount.getName() != null ) {
				name = objectFactorycommonBasic.createName();
				if(StringUtils.isNotBlank(billingAccount.getName().getFirstName())) {
					name.setValue(billingAccount.getName().getFirstName() + " " + billingAccount.getName().getLastName() != null ? billingAccount.getName().getLastName() : "");
				}
				contactType.setName(name);
			}
            partyType.setContact(contactType);
        }
        // AccountingCustomerParty/Party/PartyLegalEntity/Person

        if (billingAccount.getName() != null) {
            // AccountingSupplierParty/Party/Person/FirstName
            PersonType personType = getPersonType(billingAccount.getName());
            partyType.getPersons().add(personType);
        }

		//AccountingCustomerParty/Party/ServiceProviderParty
		var icd00225Exist = billingAccount.getCustomerAccount().getRegistrationNumbers().stream().anyMatch(rn -> {
			if(rn.getIsoIcd() != null) {
				var isoIcd = (IsoIcd) Hibernate.unproxy(rn.getIsoIcd());
				if(isoIcd.getCode().equals("0225")) {
					return true;
				}
			}
			return false;
		});
		if(icd00225Exist){
		partyType.getServiceProviderParties().add(getServiceProviderParty(billingAccount));
		}

        customerPartyType.setParty(partyType);
        if (creditNote == null)
            target.setAccountingCustomerParty(customerPartyType);
        else
            creditNote.setAccountingCustomerParty(customerPartyType);
    }

	private TaxScheme getTaxSheme(){
		TaxScheme taxScheme = objectFactoryCommonAggrement.createTaxScheme();
		TaxTypeCode taxTypeCode = objectFactorycommonBasic.createTaxTypeCode();
		taxTypeCode.setValue("TVA_SUR_ENCAISSEMENT");
		ID id = objectFactorycommonBasic.createID();
		id.setSchemeID("UN/ECE 5153");
		id.setSchemeAgencyID("6");
		id.setValue("VAT");
		taxScheme.setTaxTypeCode(taxTypeCode);
		taxScheme.setID(id);
		return taxScheme;
	}
	private PersonType getPersonType(Name name){
		PersonType personType = objectFactoryCommonAggrement.createPersonType();
		if(StringUtils.isNotBlank(name.getFirstName())){
			FirstName firstName = objectFactorycommonBasic.createFirstName();
			firstName.setValue(name.getFirstName());
			personType.setFirstName(firstName);
		}
		//AccountingSupplierParty/Party/Person/FamilyName
		if(StringUtils.isNotBlank(name.getLastName())){
			FamilyName familyName = objectFactorycommonBasic.createFamilyName();
			familyName.setValue(name.getLastName());
			personType.setFamilyName(familyName);
		}
		return personType;
	}
	
	private AddressType getRegistrationAddress(Address address){
		if(address == null) return null;
		AddressType addressType = objectFactoryCommonAggrement.createAddressType();
		// AccountingCustomerParty/Party/PartyLegalEntity/RegistrationAddress/CityName
		if(StringUtils.isNotBlank(address.getCity())){
			CityName cityName = objectFactorycommonBasic.createCityName();
			cityName.setValue(address.getCity());
			addressType.setCityName(cityName);
		}
		// AccountingCustomerParty/Party/PartyLegalEntity/RegistrationAddress/PostalZone
		if(StringUtils.isNotBlank(address.getZipCode())){
			PostalZone postalZone = objectFactorycommonBasic.createPostalZone();
			postalZone.setValue(address.getZipCode());
			addressType.setPostalZone(postalZone);
		}
		// AccountingCustomerParty/Party/PartyLegalEntity/RegistrationAddress/CountrySubentity
		if(StringUtils.isNotBlank(address.getState())){
			CountrySubentity countrySubentity = objectFactorycommonBasic.createCountrySubentity();
			countrySubentity.setValue(address.getState());
			addressType.setCountrySubentity(countrySubentity);
		}
		// AccountingCustomerParty/Party/PartyLegalEntity/RegistrationAddress/Country
		if (address.getCountry() != null) {
			CountryType countryType = objectFactoryCommonAggrement.createCountryType();
			IdentificationCode identificationCode = objectFactorycommonBasic.createIdentificationCode();
			identificationCode.setListID("ISO3166-1");
			identificationCode.setListAgencyID("6");
			identificationCode.setValue(address.getCountry().getCode());
			countryType.setIdentificationCode(identificationCode);
			addressType.setCountry(countryType);
		}
		// AccountingCustomerParty/Party/PartyLegalEntity/RegistrationAddress/RegistrationAddress
		if(StringUtils.isNotBlank(address.getAddress1())){
			StreetName streetName = objectFactorycommonBasic.createStreetName();
			streetName.setValue(address.getAddress1());
			addressType.setStreetName(streetName);
		}
		return addressType;
	}
	private void setAccountingSupplierParty(Seller seller, Invoice target, CreditNote creditNote, String invoiceLanguageCode){
		SupplierPartyType supplierPartyType = objectFactoryCommonAggrement.createSupplierPartyType();
		//AccountingSupplierParty/Party
		PartyType partyType = objectFactoryCommonAggrement.createPartyType();
		// AccountingSupplierParty/Party/PartyLegalEntity
		PartyLegalEntity partyLegalEntity = objectFactoryCommonAggrement.createPartyLegalEntity();
		// AccountingSupplierParty/Party/PartyLegalEntity/RegistrationName
		RegistrationName registrationName = objectFactorycommonBasic.createRegistrationName();
		registrationName.setValue(seller.getDescription());
		partyLegalEntity.setRegistrationName(registrationName);
		// AccountingSupplierParty/Party/PartyLegalEntity/RegistrationAddress/StreetName
		if(seller.getAddress() != null) {
			Address address = seller.getAddress();
			AddressType addressType = objectFactoryCommonAggrement.createAddressType();
			StreetName streetName = objectFactorycommonBasic.createStreetName();
			streetName.setValue(address.getAddress1());
			addressType.setStreetName(streetName);
			// AccountingSupplierParty/Party/PartyLegalEntity/RegistrationAddress/AdditionalStreetName
			if (StringUtils.isNotBlank(address.getAddress2())) {
				AdditionalStreetName additionalStreetName = objectFactorycommonBasic.createAdditionalStreetName();
				additionalStreetName.setValue(address.getAddress2());
				addressType.setAdditionalStreetName(additionalStreetName);
			}
			if(StringUtils.isNotBlank(address.getAddress3())){
				AddressLine addressLine = objectFactoryCommonAggrement.createAddressLine();
				Line line = objectFactorycommonBasic.createLine();
				line.setValue(address.getAddress3());
				addressLine.setLine(line);
				addressType.getAddressLines().add(addressLine);
			}
			// AccountingSupplierParty/Party/PartyLegalEntity/RegistrationAddress/PostalZone
			PostalZone postalZone = objectFactorycommonBasic.createPostalZone();
			postalZone.setValue(address.getZipCode());
			addressType.setPostalZone(postalZone);
			// AccountingSupplierParty/Party/PartyLegalEntity/RegistrationAddress/Country/IdentificationCode
			if (address.getCountry() != null) {
				CountryType countryType = objectFactoryCommonAggrement.createCountryType();
				IdentificationCode identificationCode = objectFactorycommonBasic.createIdentificationCode();
				identificationCode.setListID("ISO3166-1");
				identificationCode.setListAgencyID("6");
				identificationCode.setValue(address.getCountry().getCode());
				countryType.setIdentificationCode(identificationCode);
				addressType.setCountry(countryType);
			}
			if(StringUtils.isNotBlank(address.getCity())){
				CityName cityName = objectFactorycommonBasic.createCityName();
				cityName.setValue(address.getCity());
				addressType.setCityName(cityName);
			}
			if(StringUtils.isNotBlank(address.getState())) {
				CountrySubentity countrySubentity = objectFactorycommonBasic.createCountrySubentity();
				countrySubentity.setValue(address.getState());
				addressType.setCountrySubentity(countrySubentity);
			}
			partyType.setPostalAddress(addressType);
		}
		if(StringUtils.isNotBlank(seller.getVatNo())){
			// AccountingSupplierParty/Party/PartyTaxScheme/CompanyID
			PartyTaxScheme taxScheme = objectFactoryCommonAggrement.createPartyTaxScheme();
			CompanyID companyID = objectFactorycommonBasic.createCompanyID();
			String countryCode = seller.getAddress() != null && seller.getAddress().getCountry() != null ? seller.getAddress().getCountry().getCountryCode() : "";
			companyID.setSchemeID(countryCode);
			companyID.setSchemeAgencyID("ZZZ");
			companyID.setValue(seller.getVatNo());
			taxScheme.setCompanyID(companyID);
			taxScheme.setTaxScheme(getTaxSheme());
			partyType.getPartyTaxSchemes().add(taxScheme);
		}
		
		if(seller.getContactInformation() != null) {
			//AccountingSupplierParty/Party/Contact/Telephone
			ContactType contactType = getContactInformation(seller.getContactInformation());
			if(StringUtils.isNotBlank(seller.getDescription())){
				oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Name name = objectFactorycommonBasic.createName();
				name.setValue(seller.getDescription());
				contactType.setName(name);
			}
			partyType.setContact(contactType);
		}
		// AccountingSupplierParty/Party/Person
		if(seller.getName() != null) {
			// AccountingSupplierParty/Party/Person/FirstName
			Name name = seller.getName();
			PersonType personType = objectFactoryCommonAggrement.createPersonType();
			if(StringUtils.isNotBlank(name.getFirstName())){
				FirstName firstName = objectFactorycommonBasic.createFirstName();
				firstName.setValue(name.getFirstName());
				personType.setFirstName(firstName);
			}
			//AccountingSupplierParty/Party/Person/FamilyName
			if(StringUtils.isNotBlank(name.getLastName())){
				FamilyName familyName = objectFactorycommonBasic.createFamilyName();
				familyName.setValue(name.getLastName());
				personType.setFamilyName(familyName);
			}
			// AccountingSupplierParty/Party/Person/JobTitle
			if(name.getTitle() != null && StringUtils.isNotBlank(name.getTitle().getCode())){
				JobTitle jobTitle = objectFactorycommonBasic.createJobTitle();
				jobTitle.setValue(translateTitle(invoiceLanguageCode, name.getTitle()));
				personType.setJobTitle(jobTitle);
			}
			partyType.getPersons().add(personType);
		}
		addPartyIdentifications(seller.getRegistrationNumbers(), partyType);
		if(seller.getLegalEntityType() != null) {
			// AccountingSupplierParty/Party/PartyLegalEntity/CompanyLegalForm
			CompanyLegalForm companyLegalForm = objectFactorycommonBasic.createCompanyLegalForm();
			companyLegalForm.setValue(seller.getLegalEntityType().getCode());
			partyLegalEntity.setCompanyLegalForm(companyLegalForm);
			partyLegalEntity.setHeadOfficeParty(null);
		}

		PartyName partyName = objectFactoryCommonAggrement.createPartyName();
		oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Name name = objectFactorycommonBasic.createName();
		name.setValue(seller.getDescriptionOrCode());
		partyName.setName(name);
		partyType.getPartyNames().add(partyName);

		partyType.getPartyLegalEntities().add(partyLegalEntity);
		supplierPartyType.setParty(partyType);
		if(creditNote == null)
			target.setAccountingSupplierParty(supplierPartyType);
		else
			creditNote.setAccountingSupplierParty(supplierPartyType);
		
	}
	
	private ContactType getContactInformation(ContactInformation contactInformation) {
		ContactType contactType = objectFactoryCommonAggrement.createContactType();
		if(StringUtils.isNotBlank(contactInformation.getPhone())) {
			Telephone telephone = objectFactorycommonBasic.createTelephone();
			telephone.setValue(contactInformation.getPhone());
			contactType.setTelephone(telephone);
		}
		// AccountingSupplierParty/Party/Contact/ElectronicMail
		if(StringUtils.isNotBlank(contactInformation.getEmail())) {
			ElectronicMail electronicMail = objectFactorycommonBasic.createElectronicMail();
			electronicMail.setValue(contactInformation.getEmail());
			contactType.setElectronicMail(electronicMail);
		}
		return contactType;
	}
	
	private void setTaxTotal(List<TaxInvoiceAgregate> taxInvoiceAgregates, BigDecimal amountTax,  Invoice target, CreditNote creditNote,  String currency) {
		if(CollectionUtils.isNotEmpty(taxInvoiceAgregates)) {
			TaxTotalType taxTotalType = objectFactoryCommonAggrement.createTaxTotalType();
			TaxAmount taxAmount = objectFactorycommonBasic.createTaxAmount();
			taxAmount.setCurrencyID(currency);
			taxAmount.setValue(amountTax.setScale(rounding, RoundingMode.HALF_UP));
			taxTotalType.setTaxAmount(taxAmount);
			taxInvoiceAgregates.forEach(taxInvoiceAgregate -> {
				TaxSubtotal taxSubtotal = objectFactoryCommonAggrement.createTaxSubtotal();
				if(taxInvoiceAgregate.getTax() != null) {
					//TaxTotal/ TaxSubtotal / TaxCategory/ TaxScheme/ TaxTypeCode
					TaxCategoryType taxCategoryType = setTaxCategory(taxInvoiceAgregate);
					taxSubtotal.setTaxCategory(taxCategoryType);
					
				}
				// TaxTotal/ TaxSubtotal / TaxableAmount
				TaxableAmount taxableAmount = objectFactorycommonBasic.createTaxableAmount();
				final String currencyCode = taxInvoiceAgregate.getTradingCurrency() != null ? taxInvoiceAgregate.getTradingCurrency().getCurrencyCode() : currency;
				taxableAmount.setCurrencyID(currencyCode);
				taxableAmount.setValue(taxInvoiceAgregate.getAmountWithoutTax().setScale(rounding, RoundingMode.HALF_UP));
				taxSubtotal.setTaxableAmount(taxableAmount);
				// TaxTotal/ TaxSubtotal / Percent
				Percent percent = objectFactorycommonBasic.createPercent();
				percent.setValue(taxInvoiceAgregate.getTaxPercent().setScale(rounding, RoundingMode.HALF_UP));
				taxSubtotal.setPercent(percent);
				// TaxTotal/ TaxSubtotal / TaxAmount
				TaxAmount taxAmountSubTotal = objectFactorycommonBasic.createTaxAmount();
				taxAmountSubTotal.setValue(taxInvoiceAgregate.getAmountTax().setScale(rounding, RoundingMode.HALF_UP));
				taxAmountSubTotal.setCurrencyID(currencyCode);
				taxSubtotal.setTaxAmount(taxAmountSubTotal);
				taxTotalType.getTaxSubtotals().add(taxSubtotal);
			});
			if(creditNote == null)
				target.getTaxTotals().add(taxTotalType);
			else{
				creditNote.getTaxTotals().add(taxTotalType);
			}
		}
	}
	private void setAllowanceCharge(org.meveo.model.billing.Invoice invoice, Invoice target, CreditNote creditNote){
		final var currency = invoice.getTradingCurrency() != null ? invoice.getTradingCurrency().getCurrencyCode() : null;
		List<SubCategoryInvoiceAgregate> subCategoryInvoiceAgregates = (List<SubCategoryInvoiceAgregate>) invoiceAgregateService.listByInvoiceAndType(invoice, SubCategoryInvoiceAgregate.class);
		if(CollectionUtils.isNotEmpty(subCategoryInvoiceAgregates)){
			subCategoryInvoiceAgregates.forEach(subCategoryInvoiceAgregate -> {
				//if(invoiceLine.getAccountingArticle() == null || (invoiceLine.getAccountingArticle().getAllowanceCode() != null && "Standard".equalsIgnoreCase(invoiceLine.getAccountingArticle().getAllowanceCode().getDescription()))){
				if(subCategoryInvoiceAgregate.getDiscountPlanItem() == null){
					return;
				}
				AllowanceChargeType allowanceCharge = objectFactoryCommonAggrement.createAllowanceChargeType();
				ChargeIndicator chargeIndicator = objectFactorycommonBasic.createChargeIndicator();
				chargeIndicator.setValue(false);
				allowanceCharge.setChargeIndicator(chargeIndicator);
					AllowanceChargeReasonCode allowanceChargeReasonCode = objectFactorycommonBasic.createAllowanceChargeReasonCode();
					allowanceChargeReasonCode.setValue(subCategoryInvoiceAgregate.getDiscountPlanItem().getCode());
					allowanceCharge.setAllowanceChargeReasonCode(allowanceChargeReasonCode);
					
					AllowanceChargeReason allowanceChargeReason = objectFactorycommonBasic.createAllowanceChargeReason();
					allowanceChargeReason.setValue(subCategoryInvoiceAgregate.getDiscountPlanItem().getDescription());
					allowanceCharge.getAllowanceChargeReasons().add(allowanceChargeReason);

			
				Amount amount = objectFactorycommonBasic.createAmount();
				BaseAmount baseAmount = objectFactorycommonBasic.createBaseAmount();

				if(currency != null){
					amount.setCurrencyID(currency);
					baseAmount.setCurrencyID(currency);
				}
				amount.setValue(subCategoryInvoiceAgregate.getAmountWithTax().setScale(rounding, RoundingMode.HALF_UP).abs());
				allowanceCharge.setAmount(amount);

				baseAmount.setCurrencyID(invoice.getTradingCurrency() != null ? invoice.getTradingCurrency().getCurrencyCode() : null);
				baseAmount.setValue(subCategoryInvoiceAgregate.getAmountWithoutTax().setScale(rounding, RoundingMode.HALF_UP).abs());
				allowanceCharge.setBaseAmount(baseAmount);
				if(creditNote != null)
					creditNote.getAllowanceCharges().add(allowanceCharge);
				else
					target.getAllowanceCharges().add(allowanceCharge);
				
			});
		}
		
	}
	
	private static IssueDate getIssueDate(Date date){
		IssueDate issueDate = objectFactorycommonBasic.createIssueDate();
		issueDate.setValue(toXmlDate(date));
		return issueDate;
	}
	
	private OrderReference getOrderReference(CommercialOrder commercialOrder, Date invoiceDate) {
		if(commercialOrder == null) return null;
		OrderReference orderReference = objectFactoryCommonAggrement.createOrderReference();
		SalesOrderID salesOrderID = objectFactorycommonBasic.createSalesOrderID();
		salesOrderID.setValue(commercialOrder != null ? commercialOrder.getOrderNumber() : StringUtils.EMPTY);
		orderReference.setSalesOrderID(salesOrderID);
		orderReference.setIssueDate(getIssueDate(invoiceDate));
		return orderReference;
	}
	private void setOrderReference(org.meveo.model.billing.Invoice source, Invoice target){
		target.setOrderReference(getOrderReference(source.getCommercialOrder(), source.getInvoiceDate()));
	}
	private void setOrderReference(org.meveo.model.billing.Invoice source, CreditNote target){
		OrderReference orderReference = objectFactoryCommonAggrement.createOrderReference();
		SalesOrderID salesOrderID = objectFactorycommonBasic.createSalesOrderID();
		Optional<LinkedInvoice> documentReference = source.getLinkedInvoices().stream().filter(linkedInvoice -> linkedInvoice.getLinkedInvoiceValue().getInvoiceType().getCode().equalsIgnoreCase("COM")).findFirst();
		ID id = objectFactorycommonBasic.createID();
		id.setValue(source.getExternalPurchaseOrderNumber());
		orderReference.setID(id);

		if(documentReference.isPresent()){
			salesOrderID.setValue(documentReference.get().getLinkedInvoiceValue().getInvoiceNumber());
			orderReference.setSalesOrderID(salesOrderID);
			orderReference.setIssueDate(getIssueDate(documentReference.get().getLinkedInvoiceValue().getDueDate()));
			target.setOrderReference(orderReference);
		}
	}
	private void setBillingReference(org.meveo.model.billing.Invoice source, Invoice target){
		source.getLinkedInvoices().forEach(linInv -> {
			BillingReference billingReference = setBillingReference(linInv.getLinkedInvoiceValue());
			if(billingReference != null &&
							StringUtils.isNotBlank(billingReference.getInvoiceDocumentReference().getID().getValue()))
			target.getBillingReferences().add(billingReference);
		});
	}
	private void setBillingReference(org.meveo.model.billing.Invoice source, CreditNote target){
		source.getLinkedInvoices().forEach(linInv -> {
			BillingReference billingReference = setBillingReference(linInv.getLinkedInvoiceValue());
			if(billingReference != null &&
					StringUtils.isNotBlank(billingReference.getInvoiceDocumentReference().getID().getValue()))
			target.getBillingReferences().add(billingReference);
		});
	}
	private BillingReference setBillingReference(org.meveo.model.billing.Invoice source){
		if(StringUtils.isBlank(source.getInvoiceNumber())) return null;
		BillingReference billingReference = objectFactoryCommonAggrement.createBillingReference();
		DocumentReferenceType documentReferenceType = objectFactoryCommonAggrement.createDocumentReferenceType();
		ID id = objectFactorycommonBasic.createID();
		id.setValue(source.getInvoiceNumber());
		documentReferenceType.setID(id);

		IssueDate dueDate = objectFactorycommonBasic.createIssueDate();
		dueDate.setValue(toXmlDate(source.getDueDate()));
		documentReferenceType.setIssueDate(dueDate);

		billingReference.setInvoiceDocumentReference(documentReferenceType);
		return billingReference;
	}
	
	private static void setTaxCurrencyCodeAndDocumentCurrencyCode(oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ObjectFactory objectFactorycommonBasic,
	                                                                        org.meveo.model.billing.Invoice source,
	                                                                  Invoice target){
		TaxCurrencyCode taxCurrencyCode = objectFactorycommonBasic.createTaxCurrencyCode();
		DocumentCurrencyCode documentCurrencyCode = objectFactorycommonBasic.createDocumentCurrencyCode();
		getTaxCurrencyCodeAndDocumentCurrencyCode(source, taxCurrencyCode, documentCurrencyCode);
		target.setDocumentCurrencyCode(documentCurrencyCode);
		target.setTaxCurrencyCode(taxCurrencyCode);
	}
	private static void setTaxCurrencyCodeAndDocumentCurrencyCode(oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ObjectFactory objectFactorycommonBasic,
	                                                              org.meveo.model.billing.Invoice source,
	                                                              CreditNote target){
		TaxCurrencyCode taxCurrencyCode = objectFactorycommonBasic.createTaxCurrencyCode();
		DocumentCurrencyCode documentCurrencyCode = objectFactorycommonBasic.createDocumentCurrencyCode();
		getTaxCurrencyCodeAndDocumentCurrencyCode(source, taxCurrencyCode, documentCurrencyCode);
		target.setDocumentCurrencyCode(documentCurrencyCode);
		target.setTaxCurrencyCode(taxCurrencyCode);
	}
	
	private static void getTaxCurrencyCodeAndDocumentCurrencyCode(org.meveo.model.billing.Invoice source,  TaxCurrencyCode taxCurrencyCode, DocumentCurrencyCode documentCurrencyCode){
		if(source.getTradingCurrency() != null && source.getTradingCurrency().getCurrencyCode() != null){
			taxCurrencyCode.setValue(source.getTradingCurrency().getCurrencyCode());
			documentCurrencyCode.setValue(source.getTradingCurrency().getCurrencyCode());
		}else if(source.getBillingAccount() != null && source.getBillingAccount().getTradingCurrency() != null && source.getBillingAccount().getTradingCurrency().getCurrencyCode() != null){
			taxCurrencyCode.setValue(source.getBillingAccount().getTradingCurrency().getCurrencyCode());
			documentCurrencyCode.setValue(source.getBillingAccount().getTradingCurrency().getCurrencyCode());
		}
		documentCurrencyCode.setListID("ISO 4217");
		documentCurrencyCode.setListAgencyID("6");
	}
	private static XMLGregorianCalendar toXmlDate(Date date){
		if(date == null) return null;
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private TaxCategoryType setTaxCategory(TaxInvoiceAgregate taxInvoiceAgregate) {
		TaxCategoryType taxCategoryType = objectFactoryCommonAggrement.createTaxCategoryType();
		Tax tax = taxInvoiceAgregate.getTax();
		if(tax != null && taxInvoiceAgregate.getTax().getUntdidTaxationCategory() != null) {
			ID id = objectFactorycommonBasic.createID();
			id.setSchemeID("UN/ECE 5305");
			id.setSchemeAgencyID("6");
			id.setValue(taxInvoiceAgregate.getTax().getUntdidTaxationCategory().getCode());
			taxCategoryType.setID(id);
		}
		
		Percent percent = objectFactorycommonBasic.createPercent();
		percent.setValue(taxInvoiceAgregate.getTaxPercent().setScale(rounding, RoundingMode.HALF_UP));
		taxCategoryType.setPercent(percent);
		
		if(tax != null && tax.getUntdidTaxationCategory() != null) {
			UntdidTaxationCategory untdidTaxationCategory = tax.getUntdidTaxationCategory();
			if(!untdidTaxationCategory.getSemanticModel().equalsIgnoreCase("Standard rate")){
				TaxExemptionReason taxExemptionReason = objectFactorycommonBasic.createTaxExemptionReason();
				taxExemptionReason.setValue(untdidTaxationCategory.getSemanticModel());
				taxCategoryType.getTaxExemptionReasons().add(taxExemptionReason);
			}
			if(tax.getUntdidVatex() != null) {
				TaxExemptionReasonCode taxExemptionReasonCode = objectFactorycommonBasic.createTaxExemptionReasonCode();
				taxExemptionReasonCode.setListID("CWA 15577");
				taxExemptionReasonCode.setListAgencyID("ZZZ");
				taxExemptionReasonCode.setValue(tax.getUntdidVatex().getCode());
				taxCategoryType.setTaxExemptionReasonCode(taxExemptionReasonCode);
			}
		}
		
		TaxScheme taxScheme = objectFactoryCommonAggrement.createTaxScheme();
		TaxTypeCode taxTypeCode = objectFactorycommonBasic.createTaxTypeCode();
		taxTypeCode.setValue(taxInvoiceAgregate.getTax().getCode());
		taxScheme.setTaxTypeCode(taxTypeCode);
		taxCategoryType.setTaxScheme(taxScheme);
		return taxCategoryType;
	}
	
	private static MonetaryTotalType setTaxExclusiveAmount(BigDecimal totalPrepaidAmount, String currency, BigDecimal amountWithoutTax, BigDecimal amountWithTax, BigDecimal lineExtensionAmount, BigDecimal payableAmount) {
		MonetaryTotalType moneyTotalType = objectFactoryCommonAggrement.createMonetaryTotalType();
		TaxInclusiveAmount taxInclusiveAmount = objectFactorycommonBasic.createTaxInclusiveAmount();
		taxInclusiveAmount.setCurrencyID(currency);
		taxInclusiveAmount.setValue(amountWithTax.setScale(rounding, RoundingMode.HALF_UP));
		moneyTotalType.setTaxInclusiveAmount(taxInclusiveAmount);

		TaxExclusiveAmount taxExclusiveAmount = objectFactorycommonBasic.createTaxExclusiveAmount();
		taxExclusiveAmount.setCurrencyID(currency);
		taxExclusiveAmount.setValue(amountWithoutTax.setScale(rounding, RoundingMode.HALF_UP));
		moneyTotalType.setTaxExclusiveAmount(taxExclusiveAmount);
		
		LineExtensionAmount lineExtensionAmountType = objectFactorycommonBasic.createLineExtensionAmount();
		lineExtensionAmountType.setCurrencyID(currency);
		lineExtensionAmountType.setValue(lineExtensionAmount.setScale(rounding, RoundingMode.HALF_UP));
		moneyTotalType.setLineExtensionAmount(lineExtensionAmountType);
		
		PayableAmount payableAmountType = objectFactorycommonBasic.createPayableAmount();
		payableAmountType.setCurrencyID(currency);
		payableAmountType.setValue(payableAmount.setScale(rounding, RoundingMode.HALF_UP));
		moneyTotalType.setPayableAmount(payableAmountType);

		if(totalPrepaidAmount.compareTo(BigDecimal.ZERO) != 0){
			PrepaidAmount prepaidAmount = objectFactorycommonBasic.createPrepaidAmount();
			prepaidAmount.setCurrencyID(currency);
			prepaidAmount.setValue(totalPrepaidAmount);
			moneyTotalType.setPrepaidAmount(prepaidAmount);
		}


		return moneyTotalType;

	}
	
    private BillingReference setBillingReferenceForInvoice(Set<CommercialOrder> commercialOrders, Invoice target) {
        BillingReference billingReference = null;
        DocumentReferenceType documentReferenceType = null;
        ID id = null;
        if (CollectionUtils.isNotEmpty(commercialOrders)) {
            for (CommercialOrder commercialOrder : commercialOrders) {
                if (StringUtils.isBlank(commercialOrder.getOrderNumber())) {
                    continue;
                }
                billingReference = objectFactoryCommonAggrement.createBillingReference();
                documentReferenceType = objectFactoryCommonAggrement.createDocumentReferenceType();
                id = objectFactorycommonBasic.createID();
                id.setValue(commercialOrder.getOrderNumber());
                documentReferenceType.setID(id);
                billingReference.setInvoiceDocumentReference(documentReferenceType);

                IssueDate dueDate = objectFactorycommonBasic.createIssueDate();
                dueDate.setValue(toXmlDate(commercialOrder.getOrderDate()));
                documentReferenceType.setIssueDate(dueDate);

                target.getBillingReferences().add(billingReference);
            }
        }

		return billingReference;
	}
	
	private String translateAccountingArticle(AccountingArticle accountingArticle, String invoiceLanguageCode){
		if(accountingArticle == null) return null;
		String accountingArticleKey = "AA_" + accountingArticle.getCode() + "_" + invoiceLanguageCode;
		if(descriptionMap.get(accountingArticleKey) != null) {
			return descriptionMap.get(accountingArticleKey);
		}
		String descTranslated = null;
		if (accountingArticle.getDescriptionI18n() != null && accountingArticle.getDescriptionI18n().get(invoiceLanguageCode)!= null) {
			descTranslated = accountingArticle.getDescriptionI18n().get(invoiceLanguageCode);
		} else {
			descTranslated = StringUtils.isNotBlank(accountingArticle.getDescription()) ? accountingArticle.getDescription() : "";
		}
		descriptionMap.put(accountingArticleKey, descTranslated);
		return descTranslated;
	}
	
	private String translateTitle(String invoiceLanguageCode, Title title){
		if(title == null) return null;
		if(StringUtils.isBlank(invoiceLanguageCode)) return title.getDescription();
		String titleKey = "T_" + title.getCode() + "_" + invoiceLanguageCode;
		if(descriptionMap.containsKey(titleKey)) {
			return descriptionMap.get(titleKey);
		}
		String descTranslated = null;
		if (title.getDescriptionI18n() != null && title.getDescriptionI18n().get(invoiceLanguageCode) != null) {
			descTranslated = title.getDescriptionI18n().get(invoiceLanguageCode);
		} else {
			descTranslated = StringUtils.isNotBlank(title.getDescription()) ? title.getDescription() : "";
		}
		descriptionMap.put(titleKey, descTranslated);
		return descTranslated;
	}

	/**
	 * Set accounting supplier party.
	 *
	 * @param billingAccount the billing account
	 */
	private ServiceProviderParty getServiceProviderParty(BillingAccount billingAccount) {
		PartyType partyType = objectFactoryCommonAggrement.createPartyType();
		ServiceProviderParty serviceProviderParty = objectFactoryCommonAggrement.createServiceProviderParty();
		if(billingAccount.getCustomerAccount() != null){
			if (billingAccount.getCustomerAccount().getAddress() != null) {
				partyType.setPostalAddress(getPostalAddress(billingAccount.getCustomerAccount()));
			}

			if (StringUtils.isNotBlank(billingAccount.getCustomerAccount().getDescription())) {
				//serviceProviderParty.setPartyLegalEntity(getPartyLegalEntity(billingAccount.getCustomerAccount()));
				partyType.getPartyNames().add(getPartyName(billingAccount.getCustomerAccount()));
			}
		}
		partyType.setIndustryClassificationCode(getIndustryClassificationCode());
		serviceProviderParty.setParty(partyType);
		return serviceProviderParty;
	}

	/**
	 * Gets the industry classification code.
	 *
	 * @return the industry classification code
	 */
	private IndustryClassificationCode getIndustryClassificationCode() {
		IndustryClassificationCode industryClassificationCode = objectFactorycommonBasic.createIndustryClassificationCode();
		industryClassificationCode.setValue(IV);
		industryClassificationCode.setListID(UNCL_3035);
		return industryClassificationCode;
	}

	/**
	 * Gets the contact information.
	 *
	 * @param pCustomerAccount the customer account
	 * @return the contact information
	 */
	private AddressType getPostalAddress(CustomerAccount pCustomerAccount) {
		AddressType addressType = objectFactoryCommonAggrement.createAddressType();

		// AccountingCustomerParty/Party/PostalAddress/StreetName
		StreetName streetName = objectFactorycommonBasic.createStreetName();
		streetName.setValue(pCustomerAccount.getAddress().getAddress1());
		addressType.setStreetName(streetName);

		// AccountingCustomerParty/Party/PostalAddress/AdditionalStreetName
		AdditionalStreetName additionalStreetName = objectFactorycommonBasic.createAdditionalStreetName();
		additionalStreetName.setValue(pCustomerAccount.getAddress().getAddress2());
		addressType.setAdditionalStreetName(additionalStreetName);
		
		if(StringUtils.isNotBlank(pCustomerAccount.getAddress().getAddress3())){
			AddressLine addressLine = objectFactoryCommonAggrement.createAddressLine();
			Line line = objectFactorycommonBasic.createLine();
			line.setValue(pCustomerAccount.getAddress().getAddress3());
			addressLine.setLine(line);
			addressType.getAddressLines().add(addressLine);
		}
		// AccountingCustomerParty/Party/PostalAddress/Department
		Department department = objectFactorycommonBasic.createDepartment();
		department.setValue(pCustomerAccount.getAddress().getAddress4());
		addressType.setDepartment(department);

		// AccountingCustomerParty/Party/PostalAddress/CityName
		CityName cityName = objectFactorycommonBasic.createCityName();
		cityName.setValue(pCustomerAccount.getAddress().getCity());
		addressType.setCityName(cityName);

		// AccountingCustomerParty/Party/PostalAddress/PostalZone
		PostalZone postalZone = objectFactorycommonBasic.createPostalZone();
		postalZone.setValue(pCustomerAccount.getAddress().getZipCode());
		addressType.setPostalZone(postalZone);

		// AccountingCustomerParty/Party/PostalAddress/CountrySubentityCode
		CountrySubentityCode countrySubentityCode = objectFactorycommonBasic.createCountrySubentityCode();
		countrySubentityCode.setValue(pCustomerAccount.getAddress().getState());
		addressType.setCountrySubentityCode(countrySubentityCode);

		// AccountingCustomerParty/Party/PostalAddress/Country
		CountryType countryType = objectFactoryCommonAggrement.createCountryType();
		IdentificationCode identificationCode = objectFactorycommonBasic.createIdentificationCode();
		identificationCode.setListID("ISO3166-1");
		identificationCode.setListAgencyID("6");
		if(pCustomerAccount.getAddress() != null && pCustomerAccount.getAddress().getCountry() != null){
		identificationCode.setValue(pCustomerAccount.getAddress().getCountry().getCode());
		}
		countryType.setIdentificationCode(identificationCode);
		addressType.setCountry(countryType);

		return addressType;
	}

	/**
	 * Gets the party name.
	 *
	 * @param pCustomerAccount the customer account
	 * @return the party name
	 */
	private PartyName getPartyName(CustomerAccount pCustomerAccount) {
		oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.Name name = objectFactorycommonBasic.createName();
		name.setValue(pCustomerAccount.getDescription());
		PartyName partyName = objectFactoryCommonAggrement.createPartyName();
		partyName.setName(name);
		return partyName;
	}

	/**
	 * Gets the party legal entity.
	 *
	 * @param pCustomerAccount the customer account
	 * @return the party legal entity
	 */
	private PartyLegalEntity getPartyLegalEntity(CustomerAccount pCustomerAccount) {
		RegistrationName registrationName = objectFactorycommonBasic.createRegistrationName();
		registrationName.setValue(pCustomerAccount.getDescription());
		PartyLegalEntity partyLegalEntity = objectFactoryCommonAggrement.createPartyLegalEntity();
		partyLegalEntity.setRegistrationName(registrationName);
		return partyLegalEntity;
	}

	/**
	 * Gets the delivery type.
	 *
	 * @param pInvoice the invoice
	 * @return the delivery type
	 */
	private static DeliveryType getDeliveryType(org.meveo.model.billing.Invoice pInvoice) {
		AddressType addressType = objectFactoryCommonAggrement.createAddressType();

		StreetName streetName = objectFactorycommonBasic.createStreetName();
		streetName.setValue(pInvoice.getBillingAccount().getUsersAccounts().get(0).getAddress().getAddress1());
		addressType.setStreetName(streetName);
		
		AdditionalStreetName additionalStreetName = objectFactorycommonBasic.createAdditionalStreetName();
		additionalStreetName.setValue(pInvoice.getBillingAccount().getUsersAccounts().get(0).getAddress().getAddress2());
		addressType.setAdditionalStreetName(additionalStreetName);
		
		CityName cityName = objectFactorycommonBasic.createCityName();
		cityName.setValue(pInvoice.getBillingAccount().getUsersAccounts().get(0).getAddress().getCity());
		addressType.setCityName(cityName);
		
		PostalZone postalZone = objectFactorycommonBasic.createPostalZone();
		postalZone.setValue(pInvoice.getBillingAccount().getUsersAccounts().get(0).getAddress().getZipCode());
		addressType.setPostalZone(postalZone);
		
		CountrySubentity countrySubentity = objectFactorycommonBasic.createCountrySubentity();
		countrySubentity.setValue(pInvoice.getBillingAccount().getUsersAccounts().get(0).getAddress().getState());
		addressType.setCountrySubentity(countrySubentity);

		if(CollectionUtils.isNotEmpty(pInvoice.getBillingAccount().getUsersAccounts()) && pInvoice.getBillingAccount().getUsersAccounts().get(0).getAddress() != null && pInvoice.getBillingAccount().getUsersAccounts().get(0).getAddress().getCountry() != null){
			CountryType countryType = objectFactoryCommonAggrement.createCountryType();
			IdentificationCode identificationCode = objectFactorycommonBasic.createIdentificationCode();
			identificationCode.setValue(pInvoice.getBillingAccount().getUsersAccounts().get(0).getAddress().getCountry().getCountryCode());
			identificationCode.setListID("ISO3166-1");
			identificationCode.setListAgencyID("6");
			countryType.setIdentificationCode(identificationCode);
			addressType.setCountry(countryType);
		}
		


		LocationType locationType = objectFactoryCommonAggrement.createLocationType();
		locationType.setAddress(addressType);

		DeliveryType deliveryType = objectFactoryCommonAggrement.createDeliveryType();
		deliveryType.setDeliveryLocation(locationType);

		return deliveryType;
	}
	
	private PeriodType setPeriodTypeInvoiceLine(org.meveo.model.billing.Invoice invoice, InvoiceLine invoiceLine) {
		if(invoiceLine.getSubscription() == null || invoice == null) return null;
		
		PeriodType periodType = objectFactoryCommonAggrement.createPeriodType();
		if(invoice.getStartDate() != null){
			StartDate startDate = objectFactorycommonBasic.createStartDate();
			startDate.setValue(toXmlDate(invoice.getStartDate()));
			periodType.setStartDate(startDate);
		}
		if(invoice.getEndDate() != null){
			EndDate endDate = objectFactorycommonBasic.createEndDate();
			endDate.setValue(toXmlDate(invoice.getEndDate()));
			periodType.setEndDate(endDate);
		}
		
		VatDateCodeEnum vatDateCode = einvoiceSettingService.findEinvoiceSetting().getVatDateCode();
		DescriptionCode descriptionCode = objectFactorycommonBasic.createDescriptionCode();
		descriptionCode.setValue(String.valueOf(vatDateCode.getPaidToDays()));
		periodType.getDescriptionCodes().add(descriptionCode);
		return periodType;
	}

	private ProfileID getProfileID(List<InvoiceLine> invoiceLines) {
		ProfileID profileID = objectFactorycommonBasic.createProfileID();
		if(CollectionUtils.isNotEmpty(invoiceLines)) {
			var physicalExist = invoiceLines.stream().filter(invoiceLine -> invoiceLine.getAccountingArticle() != null)
									.map(InvoiceLine::getAccountingArticle).map(AccountingArticle::isPhysical).collect(Collectors.toSet());
			if(physicalExist.contains(true) && physicalExist.contains(false)) {
				profileID.setValue("M1");
			}else if(physicalExist.contains(true)) {
				profileID.setValue("B1");
			}else if (physicalExist.contains(false)){
					profileID.setValue("S1");
			}else return null;

}
		return profileID;
	}

	private void addPartyIdentifications( List<RegistrationNumber> registrationNumbers, PartyType partyType) {
		if (CollectionUtils.isNotEmpty(registrationNumbers)) {
			for (RegistrationNumber registerNumber : registrationNumbers) {
				if (registerNumber.getIsoIcd() == null) continue;
				if (registerNumber.getIsoIcd().getCode() != null) {
					PartyIdentification partyIdentification = objectFactoryCommonAggrement.createPartyIdentification();
					ID id = objectFactorycommonBasic.createID();
					id.setSchemeID(registerNumber.getIsoIcd().getCode());
					id.setSchemeAgencyID(ISO_IEC_6523);
					id.setValue(registerNumber.getRegistrationNo());
					partyIdentification.setID(id);
					partyType.getPartyIdentifications().add(partyIdentification);
				}
			}
		}
	}
}
