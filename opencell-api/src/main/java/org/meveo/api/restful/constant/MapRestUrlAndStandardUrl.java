package org.meveo.api.restful.constant;

import org.meveo.api.restful.JaxRsActivatorGenericApiV1;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapRestUrlAndStandardUrl {

    public static final String SEPARATOR = ",";

    public static final String GET = "GET";

    public static final String POST = "POST";

    public static final String PUT = "PUT";

    public static final String DELETE = "DELETE";

    // Here is the syntax to declare a mapping between a RESTful URL and a standard URL
    // (K, V) = ("{METHOD}{SEPARATOR}{standardURL}", "{restfulURL}")
    public static final Map<String, String> MAP_RESTFUL_URL_AND_STANDARD_URL = new LinkedHashMap<>() {
        {

            // for entity Seller
            put( POST + SEPARATOR + "/seller", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/sellers" );
            put( GET + SEPARATOR + "/seller/list", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/sellers" );
            put( GET + SEPARATOR + "/seller", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/sellers/{sellerCode}" );
            put( PUT + SEPARATOR + "/seller", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/sellers/{sellerCode}" );
            put( DELETE + SEPARATOR + "/seller/{sellerCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/sellers/{sellerCode}" );

            // for entity BillingCycle
            put( POST + SEPARATOR + "/billingCycle", JaxRsActivatorGenericApiV1.REST_PATH + "/billingCycles" );
            put( GET + SEPARATOR + "/billingCycle/list", JaxRsActivatorGenericApiV1.REST_PATH + "/billingCycles" );
            put( GET + SEPARATOR + "/billingCycle", JaxRsActivatorGenericApiV1.REST_PATH + "/billingCycles/{billingCycleCode}" );
            put( PUT + SEPARATOR + "/billingCycle", JaxRsActivatorGenericApiV1.REST_PATH + "/billingCycles/{billingCycleCode}" );
            put( DELETE + SEPARATOR + "/billingCycle/{billingCycleCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/billingCycles/{billingCycleCode}" );

            // for entity InvoiceCategory
            put( POST + SEPARATOR + "/invoiceCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceCategories" );
            put( GET + SEPARATOR + "/invoiceCategory/list", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceCategories" );
            put( GET + SEPARATOR + "/invoiceCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceCategories/{invoiceCategoryCode}" );
            put( PUT + SEPARATOR + "/invoiceCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceCategories/{invoiceCategoryCode}" );
            put( DELETE + SEPARATOR + "/invoiceCategory/{invoiceCategoryCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceCategories/{invoiceCategoryCode}" );

            // for entity InvoiceSequence
            put( POST + SEPARATOR + "/invoiceSequence", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSequences" );
            put( GET + SEPARATOR + "/invoiceSequence/list", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSequences" );
            put( GET + SEPARATOR + "/invoiceSequence", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSequences/{invoiceSequenceCode}" );
            put( PUT + SEPARATOR + "/invoiceSequence", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSequences/{invoiceSequenceCode}" );

            // for entity InvoiceSubCategory
            put( POST + SEPARATOR + "/invoiceSubCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSubCategories" );
            put( GET + SEPARATOR + "/invoiceSubCategory/list", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSubCategories" );
            put( GET + SEPARATOR + "/invoiceSubCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSubCategories/{invoiceSubCategoryCode}" );
            put( PUT + SEPARATOR + "/invoiceSubCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSubCategories/{invoiceSubCategoryCode}" );
            put( DELETE + SEPARATOR + "/invoiceSubCategory/{invoiceSubCategoryCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceSubCategories/{invoiceSubCategoryCode}" );

            // for entity InvoiceType
            put( POST + SEPARATOR + "/invoiceType", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceTypes" );
            put( GET + SEPARATOR + "/invoiceType/list", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceTypes" );
            put( GET + SEPARATOR + "/invoiceType", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceTypes/{invoiceTypeCode}" );
            put( PUT + SEPARATOR + "/invoiceType", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceTypes/{invoiceTypeCode}" );
            put( DELETE + SEPARATOR + "/invoiceType/{invoiceTypeCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/invoiceTypes/{invoiceTypeCode}" );

            // for entity User
            put( POST + SEPARATOR + "/user", JaxRsActivatorGenericApiV1.REST_PATH + "/users" );
            put( GET + SEPARATOR + "/user/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/users" );
            put( GET + SEPARATOR + "/user", JaxRsActivatorGenericApiV1.REST_PATH + "/users/{userCode}" );
            put( PUT + SEPARATOR + "/user", JaxRsActivatorGenericApiV1.REST_PATH + "/users/{userCode}" );
            put( DELETE + SEPARATOR + "/user/{userName}", JaxRsActivatorGenericApiV1.REST_PATH + "/users/{userCode}" );

            // for entity Calendar
            put( POST + SEPARATOR + "/calendar", JaxRsActivatorGenericApiV1.REST_PATH + "/calendars" );
            put( GET + SEPARATOR + "/calendar/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/calendars" );
            put( GET + SEPARATOR + "/calendar", JaxRsActivatorGenericApiV1.REST_PATH + "/calendars/{calendarCode}" );
            put( PUT + SEPARATOR + "/calendar", JaxRsActivatorGenericApiV1.REST_PATH + "/calendars/{calendarCode}" );
            put( DELETE + SEPARATOR + "/calendar/{calendarCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/calendars/{calendarCode}" );

            // for entity UnitOfMeasure
            put( POST + SEPARATOR + "/catalog/unitOfMeasure", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/unitOfMeasures" );
            put( GET + SEPARATOR + "/catalog/unitOfMeasure/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/unitOfMeasures" );
            put( GET + SEPARATOR + "/catalog/unitOfMeasure", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/unitOfMeasures/{unitOfMeasureCode}" );
            put( PUT + SEPARATOR + "/catalog/unitOfMeasure", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/unitOfMeasures/{unitOfMeasureCode}" );
            put( DELETE + SEPARATOR + "/catalog/unitOfMeasure/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/unitOfMeasures/{unitOfMeasureCode}" );

            // for entity Contact
            put( POST + SEPARATOR + "/contact", JaxRsActivatorGenericApiV1.REST_PATH + "/contacts" );
            put( GET + SEPARATOR + "/contact/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/contacts" );
            put( GET + SEPARATOR + "/contact", JaxRsActivatorGenericApiV1.REST_PATH + "/contacts/{contactCode}" );
            put( PUT + SEPARATOR + "/contact", JaxRsActivatorGenericApiV1.REST_PATH + "/contacts/{contactCode}" );
            put( DELETE + SEPARATOR + "/contact/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/contacts/{contactCode}" );

            // for entity Tax
            put( POST + SEPARATOR + "/tax", JaxRsActivatorGenericApiV1.REST_PATH + "/taxes" );
            put( GET + SEPARATOR + "/tax/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/taxes" );
            put( GET + SEPARATOR + "/tax", JaxRsActivatorGenericApiV1.REST_PATH + "/taxes/{taxCode}" );
            put( PUT + SEPARATOR + "/tax", JaxRsActivatorGenericApiV1.REST_PATH + "/taxes/{taxCode}" );
            put( DELETE + SEPARATOR + "/tax/{taxCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/taxes/{taxCode}" );

            // for entity TaxCategory
            put( POST + SEPARATOR + "/taxCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/taxCategories" );
            put( GET + SEPARATOR + "/taxCategory/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/taxCategories" );
            put( GET + SEPARATOR + "/taxCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/taxCategories/{taxCategoryCode}" );
            put( PUT + SEPARATOR + "/taxCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/taxCategories/{taxCategoryCode}" );
            put( DELETE + SEPARATOR + "/taxCategory/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/taxCategories/{taxCategoryCode}" );

            // for entity TaxClass
            put( POST + SEPARATOR + "/taxClass", JaxRsActivatorGenericApiV1.REST_PATH + "/taxClasses" );
            put( GET + SEPARATOR + "/taxClass/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/taxClasses" );
            put( GET + SEPARATOR + "/taxClass", JaxRsActivatorGenericApiV1.REST_PATH + "/taxClasses/{taxClassCode}" );
            put( PUT + SEPARATOR + "/taxClass", JaxRsActivatorGenericApiV1.REST_PATH + "/taxClasses/{taxClassCode}" );
            put( DELETE + SEPARATOR + "/taxClass/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/taxClasses/{taxClassCode}" );

            // for entity TaxMapping
            put( POST + SEPARATOR + "/taxMapping", JaxRsActivatorGenericApiV1.REST_PATH + "/taxMappings" );
            put( GET + SEPARATOR + "/taxMapping/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/taxMappings" );
            put( GET + SEPARATOR + "/taxMapping", JaxRsActivatorGenericApiV1.REST_PATH + "/taxMappings/{taxMappingCode}" );
            put( PUT + SEPARATOR + "/taxMapping", JaxRsActivatorGenericApiV1.REST_PATH + "/taxMappings/{taxMappingCode}" );
            put( DELETE + SEPARATOR + "/taxMapping/{id}", JaxRsActivatorGenericApiV1.REST_PATH + "/taxMappings/{taxMappingCode}" );

            // for entity CreditCategory
            put( POST + SEPARATOR + "/payment/creditCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/creditCategories" );
            put( GET + SEPARATOR + "/payment/creditCategory/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/creditCategories" );
            put( GET + SEPARATOR + "/payment/creditCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/creditCategories/{creditCategoryCode}" );
            put( PUT + SEPARATOR + "/payment/creditCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/creditCategories/{creditCategoryCode}" );
            put( DELETE + SEPARATOR + "/payment/creditCategory/{creditCategoryCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/creditCategories/{creditCategoryCode}" );

            // for entity BusinessProductModel
            put( POST + SEPARATOR + "/catalog/businessProductModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessProductModels" );
            put( GET + SEPARATOR + "/catalog/businessProductModel/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessProductModels" );
            put( GET + SEPARATOR + "/catalog/businessProductModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessProductModels/{businessProductModelCode}" );
            put( PUT + SEPARATOR + "/catalog/businessProductModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessProductModels/{businessProductModelCode}" );
            put( DELETE + SEPARATOR + "/catalog/businessProductModel/{businessProductModelCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessProductModels/{businessProductModelCode}" );

            // for entity BusinessOfferModel
            put( POST + SEPARATOR + "/catalog/businessOfferModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessOfferModels" );
            put( GET + SEPARATOR + "/catalog/businessOfferModel/list", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessOfferModels" );
            put( GET + SEPARATOR + "/catalog/businessOfferModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessOfferModels/{businessOfferModelCode}" );
            put( PUT + SEPARATOR + "/catalog/businessOfferModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessOfferModels/{businessOfferModelCode}" );
            put( DELETE + SEPARATOR + "/catalog/businessOfferModel/{businessOfferModelCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessOfferModels/{businessOfferModelCode}" );

            // for entity BusinessServiceModel
            put( POST + SEPARATOR + "/catalog/businessServiceModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessServiceModels" );
            put( GET + SEPARATOR + "/catalog/businessServiceModel/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessServiceModels" );
            put( GET + SEPARATOR + "/catalog/businessServiceModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessServiceModels/{businessServiceModelCode}" );
            put( PUT + SEPARATOR + "/catalog/businessServiceModel", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessServiceModels/{businessServiceModelCode}" );
            put( DELETE + SEPARATOR + "/catalog/businessServiceModel/{businessServiceModelCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/businessServiceModels/{businessServiceModelCode}" );

            // for entity TriggeredEdr
            put( POST + SEPARATOR + "/catalog/triggeredEdr", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/triggeredEdrs" );
            put( GET + SEPARATOR + "/catalog/triggeredEdr/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/triggeredEdrs" );
            put( GET + SEPARATOR + "/catalog/triggeredEdr", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/triggeredEdrs/{triggeredEdrCode}" );
            put( PUT + SEPARATOR + "/catalog/triggeredEdr", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/triggeredEdrs/{triggeredEdrCode}" );
            put( DELETE + SEPARATOR + "/catalog/triggeredEdr/{triggeredEdrCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/triggeredEdrs/{triggeredEdrCode}" );

            // for entity EmailTemplate
            put( POST + SEPARATOR + "/communication/emailTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/emailTemplates" );
            put( GET + SEPARATOR + "/communication/emailTemplate/list", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/emailTemplates" );
            put( GET + SEPARATOR + "/communication/emailTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/emailTemplates/{emailTemplateCode}" );
            put( PUT + SEPARATOR + "/communication/emailTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/emailTemplates/{emailTemplateCode}" );
            put( DELETE + SEPARATOR + "/communication/emailTemplate/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/emailTemplates/{emailTemplateCode}" );

            // for entity MeveoInstance
            put( POST + SEPARATOR + "/communication/meveoInstance", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/meveoInstances" );
            put( GET + SEPARATOR + "/communication/meveoInstance/list", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/meveoInstances" );
            put( GET + SEPARATOR + "/communication/meveoInstance", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/meveoInstances/{meveoInstanceCode}" );
            put( PUT + SEPARATOR + "/communication/meveoInstance", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/meveoInstances/{meveoInstanceCode}" );
            put( DELETE + SEPARATOR + "/communication/meveoInstance/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/communication/meveoInstances/{meveoInstanceCode}" );

            // for entity AccountingCode
            put( POST + SEPARATOR + "/billing/accountingCode", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/accountingCodes" );
            put( GET + SEPARATOR + "/billing/accountingCode/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/accountingCodes" );
            put( GET + SEPARATOR + "/billing/accountingCode", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/accountingCodes/{accountingCodeCode}" );
            put( PUT + SEPARATOR + "/billing/accountingCode", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/accountingCodes/{accountingCodeCode}" );
            put( DELETE + SEPARATOR + "/billing/accountingCode/{accountingCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/accountingCodes/{accountingCodeCode}" );
            put( POST + SEPARATOR + "/billing/accountingCode/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/accountingCodes/{accountingCodeCode}/enable" );
            put( POST + SEPARATOR + "/billing/accountingCode/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/accountingCodes/{accountingCodeCode}/disable" );

            // for entity CounterTemplate
            put( POST + SEPARATOR + "/catalog/counterTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/counterTemplates" );
            put( GET + SEPARATOR + "/catalog/counterTemplate/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/counterTemplates" );
            put( GET + SEPARATOR + "/catalog/counterTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/counterTemplates/{counterTemplateCode}" );
            put( PUT + SEPARATOR + "/catalog/counterTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/counterTemplates/{counterTemplateCode}" );
            put( DELETE + SEPARATOR + "/catalog/counterTemplate/{counterTemplateCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/counterTemplates/{counterTemplateCode}" );
            put( POST + SEPARATOR + "/catalog/counterTemplate/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/counterTemplates/{counterTemplateCode}/enable" );
            put( POST + SEPARATOR + "/catalog/counterTemplate/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/counterTemplates/{counterTemplateCode}/disable" );

            // for entity RecurringChargeTemplate
            put( POST + SEPARATOR + "/catalog/recurringChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/recurringChargeTemplates" );
            put( GET + SEPARATOR + "/catalog/recurringChargeTemplate/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/recurringChargeTemplates" );
            put( GET + SEPARATOR + "/catalog/recurringChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/recurringChargeTemplates/{recurringChargeTemplateCode}" );
            put( PUT + SEPARATOR + "/catalog/recurringChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/recurringChargeTemplates/{recurringChargeTemplateCode}" );
            put( DELETE + SEPARATOR + "/catalog/recurringChargeTemplate/{recurringChargeTemplateCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/recurringChargeTemplates/{recurringChargeTemplateCode}" );
            put( POST + SEPARATOR + "/catalog/recurringChargeTemplate/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/recurringChargeTemplates/{recurringChargeTemplateCode}/enable" );
            put( POST + SEPARATOR + "/catalog/recurringChargeTemplate/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/recurringChargeTemplates/{recurringChargeTemplateCode}/disable" );

            // for entity ServiceTemplate
            put( POST + SEPARATOR + "/catalog/serviceTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/serviceTemplates" );
            put( GET + SEPARATOR + "/catalog/serviceTemplate/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/serviceTemplates" );
            put( GET + SEPARATOR + "/catalog/serviceTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/serviceTemplates/{serviceTemplateCode}" );
            put( PUT + SEPARATOR + "/catalog/serviceTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/serviceTemplates/{serviceTemplateCode}" );
            put( DELETE + SEPARATOR + "/catalog/serviceTemplate/{serviceTemplateCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/serviceTemplates/{serviceTemplateCode}" );
            put( POST + SEPARATOR + "/catalog/serviceTemplate/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/serviceTemplates/{serviceTemplateCode}/enable" );
            put( POST + SEPARATOR + "/catalog/serviceTemplate/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/serviceTemplates/{serviceTemplateCode}/disable" );

            // for entity Country
            put( POST + SEPARATOR + "/country", JaxRsActivatorGenericApiV1.REST_PATH + "/countries" );
            put( GET + SEPARATOR + "/country/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/countries" );
            put( GET + SEPARATOR + "/country", JaxRsActivatorGenericApiV1.REST_PATH + "/countries/{countryCode}" );
            put( PUT + SEPARATOR + "/country", JaxRsActivatorGenericApiV1.REST_PATH + "/countries/{countryCode}" );
            put( DELETE + SEPARATOR + "/country/{countryCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/countries/{countryCode}" );
            put( POST + SEPARATOR + "/country/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/countries/{countryCode}/enable" );
            put( POST + SEPARATOR + "/country/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/countries/{countryCode}/disable" );

            // for entity PaymentMethod
            put( POST + SEPARATOR + "/payment/paymentMethod", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/paymentMethods" );
            put( GET + SEPARATOR + "/payment/paymentMethod/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/paymentMethods" );
            put( GET + SEPARATOR + "/payment/paymentMethod", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/paymentMethods/{paymentMethodCode}" );
            put( PUT + SEPARATOR + "/payment/paymentMethod", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/paymentMethods/{paymentMethodCode}" );
            put( DELETE + SEPARATOR + "/payment/paymentMethod", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/paymentMethods/{paymentMethodCode}" );
            put( POST + SEPARATOR + "/payment/paymentMethod/{id}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/paymentMethods/{paymentMethodId}/enable" );
            put( POST + SEPARATOR + "/payment/paymentMethod/{id}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/payment/paymentMethods/{paymentMethodId}/disable" );

            // for entity RatedTransaction
            put( GET + SEPARATOR + "/billing/ratedTransaction/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/ratedTransactions" );

            // for entity CurrencyIso
            put( POST + SEPARATOR + "/currencyIso", JaxRsActivatorGenericApiV1.REST_PATH + "/currenciesIso" );
            put( GET + SEPARATOR + "/currencyIso/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/currenciesIso" );
            put( GET + SEPARATOR + "/currencyIso", JaxRsActivatorGenericApiV1.REST_PATH + "/currenciesIso/{currencyIsoCode}" );
            put( PUT + SEPARATOR + "/currencyIso", JaxRsActivatorGenericApiV1.REST_PATH + "/currenciesIso/{currencyIsoCode}" );
            put( DELETE + SEPARATOR + "/currencyIso/{currencyCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/currenciesIso/{currencyIsoCode}" );

            // for entity BusinessAccountModel
            put( POST + SEPARATOR + "/account/businessAccountModel", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/businessAccountModels" );
            put( GET + SEPARATOR + "/account/businessAccountModel/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/businessAccountModels" );
            put( GET + SEPARATOR + "/account/businessAccountModel", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/businessAccountModels/{businessAccountModelCode}" );
            put( PUT + SEPARATOR + "/account/businessAccountModel", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/businessAccountModels/{businessAccountModelCode}" );
            put( DELETE + SEPARATOR + "/account/businessAccountModel/{businessAccountModelCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/businessAccountModels/{businessAccountModelCode}" );

            // for entity Currency
            put( POST + SEPARATOR + "/currency", JaxRsActivatorGenericApiV1.REST_PATH + "/currencies" );
            put( GET + SEPARATOR + "/currency/list", JaxRsActivatorGenericApiV1.REST_PATH + "/currencies" );
            put( GET + SEPARATOR + "/currency", JaxRsActivatorGenericApiV1.REST_PATH + "/currencies/{currencyCode}" );
            put( PUT + SEPARATOR + "/currency", JaxRsActivatorGenericApiV1.REST_PATH + "/currencies/{currencyCode}" );
            put( DELETE + SEPARATOR + "/currency/{currencyCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/currencies/{currencyCode}" );
            put( POST + SEPARATOR + "/currency/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/currencies/{currencyCode}/enable" );
            put( POST + SEPARATOR + "/currency/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/currencies/{currencyCode}/disable" );

            // for entity OneShotChargeTemplate
            put( POST + SEPARATOR + "/catalog/oneShotChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/oneShotChargeTemplates" );
            put( GET + SEPARATOR + "/catalog/oneShotChargeTemplate/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/oneShotChargeTemplates" );
            put( GET + SEPARATOR + "/catalog/oneShotChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/oneShotChargeTemplates/{oneShotChargeTemplateCode}" );
            put( PUT + SEPARATOR + "/catalog/oneShotChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/oneShotChargeTemplates/{oneShotChargeTemplateCode}" );
            put( DELETE + SEPARATOR + "/catalog/oneShotChargeTemplate/{oneShotChargeTemplateCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/oneShotChargeTemplates/{oneShotChargeTemplateCode}" );
            put( POST + SEPARATOR + "/catalog/oneShotChargeTemplate/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/oneShotChargeTemplates/{oneShotChargeTemplateCode}/enable" );
            put( POST + SEPARATOR + "/catalog/oneShotChargeTemplate/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/oneShotChargeTemplates/{oneShotChargeTemplateCode}/disable" );

            // for entity Invoice
            put( GET + SEPARATOR + "/invoice", JaxRsActivatorGenericApiV1.REST_PATH + "/invoices" );
            put( GET + SEPARATOR + "/invoice/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/invoices" );
            put( GET + SEPARATOR + "/invoice/getPdfInvoice", JaxRsActivatorGenericApiV1.REST_PATH + "/invoices/pdfInvoices/{invoiceId}" );
            put( GET + SEPARATOR + "/invoice/getXMLInvoice", JaxRsActivatorGenericApiV1.REST_PATH + "/invoices/xmlInvoices/{invoiceId}" );
            put( PUT + SEPARATOR + "/invoice/validate", JaxRsActivatorGenericApiV1.REST_PATH + "/invoices/{invoiceId}/validation" );
            put( POST + SEPARATOR + "/invoice/generateInvoice", JaxRsActivatorGenericApiV1.REST_PATH + "/invoices/generation" );
            put( POST + SEPARATOR + "/invoice/fetchPdfInvoice", JaxRsActivatorGenericApiV1.REST_PATH + "/invoices/pdfInvoices" );
            put( POST + SEPARATOR + "/invoice/fetchXMLInvoice", JaxRsActivatorGenericApiV1.REST_PATH + "/invoices/xmlInvoices" );

            // for entity JobInstance
            put( POST + SEPARATOR + "/jobInstance/create", JaxRsActivatorGenericApiV1.REST_PATH + "/jobInstances" );
            put( GET + SEPARATOR + "/jobInstance/list", JaxRsActivatorGenericApiV1.REST_PATH + "/jobInstances" );
            put( GET + SEPARATOR + "/jobInstance", JaxRsActivatorGenericApiV1.REST_PATH + "/jobInstances/{jobInstanceCode}" );
            put( PUT + SEPARATOR + "/jobInstance", JaxRsActivatorGenericApiV1.REST_PATH + "/jobInstances/{jobInstanceCode}" );
            put( DELETE + SEPARATOR + "/jobInstance/{jobInstanceCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/jobInstances/{jobInstanceCode}" );
            put( POST + SEPARATOR + "/jobInstance/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/jobInstances/{jobInstanceCode}/enable" );
            put( POST + SEPARATOR + "/jobInstance/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/jobInstances/{jobInstanceCode}/disable" );

            // for entity OfferTemplate
            put( POST + SEPARATOR + "/catalog/offerTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplates" );
            put( GET + SEPARATOR + "/catalog/offerTemplate/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplates" );
            put( GET + SEPARATOR + "/catalog/offerTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplates/{offerTemplateCode}" );
            put( PUT + SEPARATOR + "/catalog/offerTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplates/{offerTemplateCode}" );
            put( DELETE + SEPARATOR + "/catalog/offerTemplate/{offerTemplateCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplates/{offerTemplateCode}" );
            put( POST + SEPARATOR + "/catalog/offerTemplate/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplates/{offerTemplateCode}/enable" );
            put( POST + SEPARATOR + "/catalog/offerTemplate/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplates/{offerTemplateCode}/disable" );

            // for entity Access
            put( POST + SEPARATOR + "/account/access", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/accesses" );
            put( POST + SEPARATOR + "/account/access/createOrUpdate", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/accesses/{accessCode}/creationOrUpdate" );
            put( PUT + SEPARATOR + "/account/access", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/accesses/{accessCode}" );
            put( GET + SEPARATOR + "/account/access/list", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions/{subscriptionCode}/accesses" );
            put( GET + SEPARATOR + "/account/access", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions/{subscriptionCode}/accesses/{accessCode}" );

            // for entity CountryIso
            put( POST + SEPARATOR + "/countryIso", JaxRsActivatorGenericApiV1.REST_PATH + "/countriesIso" );
            put( GET + SEPARATOR + "/countryIso/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/countriesIso" );
            put( GET + SEPARATOR + "/countryIso", JaxRsActivatorGenericApiV1.REST_PATH + "/countriesIso/{countryIsoCode}" );
            put( PUT + SEPARATOR + "/countryIso", JaxRsActivatorGenericApiV1.REST_PATH + "/countriesIso/{countryIsoCode}" );
            put( DELETE + SEPARATOR + "/countryIso/{countryCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/countriesIso/{countryIsoCode}" );

            // for entity AccountHierarchy
            put( POST + SEPARATOR + "/account/accountHierarchy", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/accountHierarchies/{accountHierarchyCode}" );
            put( PUT + SEPARATOR + "/account/accountHierarchy", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/accountHierarchies/{accountHierarchyCode}" );

            // for entity Wallet
            put( POST + SEPARATOR + "/billing/wallet/operation", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/wallets" );
            put( GET + SEPARATOR + "/billing/wallet/operation/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/billing/wallets/operation" );

            // for entity UserAccount
            put( POST + SEPARATOR + "/account/userAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/userAccounts" );
            put( GET + SEPARATOR + "/account/userAccount/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/userAccounts" );
            put( GET + SEPARATOR + "/account/userAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/userAccounts/{userAccountCode}" );
            put( PUT + SEPARATOR + "/account/userAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/userAccounts/{userAccountCode}" );
            put( DELETE + SEPARATOR + "/account/userAccount/{userAccountCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/userAccounts/{userAccountCode}" );
            put( GET + SEPARATOR + "/account/userAccount/list", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/billingAccounts/{billingAccountCode}/userAccounts" );

            // for entity DiscountPlanItem
            put( POST + SEPARATOR + "/catalog/discountPlanItem", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlanItems" );
            put( GET + SEPARATOR + "/catalog/discountPlanItem/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlanItems" );
            put( GET + SEPARATOR + "/catalog/discountPlanItem", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlanItems/{discountPlanItemCode}" );
            put( PUT + SEPARATOR + "/catalog/discountPlanItem", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlanItems/{discountPlanItemCode}" );
            put( DELETE + SEPARATOR + "/catalog/discountPlanItem/{discountPlanItemCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlanItems/{discountPlanItemCode}" );
            put( POST + SEPARATOR + "/catalog/discountPlanItem/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlanItems/{discountPlanItemCode}/enable" );
            put( POST + SEPARATOR + "/catalog/discountPlanItem/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlanItems/{discountPlanItemCode}/disable" );

            // for entity CustomerAccount
            put( POST + SEPARATOR + "/account/customerAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customerAccounts" );
            put( GET + SEPARATOR + "/account/customerAccount/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customerAccounts" );
            put( GET + SEPARATOR + "/account/customerAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customerAccounts/{customerAccountCode}" );
            put( PUT + SEPARATOR + "/account/customerAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customerAccounts/{customerAccountCode}" );
            put( DELETE + SEPARATOR + "/account/customerAccount/{customerAccountCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customerAccounts/{customerAccountCode}" );
            put( GET + SEPARATOR + "/account/customerAccount/list", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/billingAccounts/{billingAccountCode}/userAccounts" );

            // for entity Chart
            put( POST + SEPARATOR + "/chart", JaxRsActivatorGenericApiV1.REST_PATH + "/charts" );
            put( GET + SEPARATOR + "/chart/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/charts" );
            put( GET + SEPARATOR + "/chart", JaxRsActivatorGenericApiV1.REST_PATH + "/charts/{chartCode}" );
            put( PUT + SEPARATOR + "/chart", JaxRsActivatorGenericApiV1.REST_PATH + "/charts/{chartCode}" );
            put( DELETE + SEPARATOR + "/chart", JaxRsActivatorGenericApiV1.REST_PATH + "/charts/{chartCode}" );
            put( POST + SEPARATOR + "/chart/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/charts/{chartCode}/enable" );
            put( POST + SEPARATOR + "/chart/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/charts/{chartCode}/disable" );

            // for entity PdfInvoice
            put( GET + SEPARATOR + "/PdfInvoice", JaxRsActivatorGenericApiV1.REST_PATH + "/pdfInvoices" );

            // for entity ProviderContact
            put( POST + SEPARATOR + "/account/providerContact", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/providerContacts" );
            put( GET + SEPARATOR + "/account/providerContact/list", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/providerContacts" );
            put( GET + SEPARATOR + "/account/providerContact", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/providerContacts/{providerContactCode}" );
            put( PUT + SEPARATOR + "/account/providerContact", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/providerContacts/{providerContactCode}" );
            put( DELETE + SEPARATOR + "/account/providerContact/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/providerContacts/{providerContactCode}" );

            // for entity Customer
            put( POST + SEPARATOR + "/account/customer", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customers" );
            put( GET + SEPARATOR + "/account/customer/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customers" );
            put( GET + SEPARATOR + "/account/customer", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customers/{customerCode}" );
            put( PUT + SEPARATOR + "/account/customer", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customers/{customerCode}" );
            put( DELETE + SEPARATOR + "/account/customer/{customerCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customers/{customerCode}" );

            // for entity Title
            put( POST + SEPARATOR + "/account/title", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/titles" );
            put( GET + SEPARATOR + "/account/title/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/titles" );
            put( GET + SEPARATOR + "/account/title", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/titles/{titleCode}" );
            put( PUT + SEPARATOR + "/account/title", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/titles/{titleCode}" );
            put( DELETE + SEPARATOR + "/account/title/{titleCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/titles/{titleCode}" );

            // for entity ProductChargeTemplate
            put( POST + SEPARATOR + "/catalogManagement/productChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productChargeTemplates" );
            put( GET + SEPARATOR + "/catalogManagement/productChargeTemplate/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productChargeTemplates" );
            put( GET + SEPARATOR + "/catalogManagement/productChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productChargeTemplates/{productChargeTemplateCode}" );
            put( PUT + SEPARATOR + "/catalogManagement/productChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productChargeTemplates/{productChargeTemplateCode}" );
            put( DELETE + SEPARATOR + "/catalogManagement/productChargeTemplate/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productChargeTemplates/{productChargeTemplateCode}" );
            put( POST + SEPARATOR + "/catalogManagement/productChargeTemplate/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productChargeTemplates/{productChargeTemplateCode}/enable" );
            put( POST + SEPARATOR + "/catalogManagement/productChargeTemplate/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productChargeTemplates/{productChargeTemplateCode}/disable" );

            // for entity ProductTemplate
            put( POST + SEPARATOR + "/catalogManagement/productTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productTemplates" );
            put( GET + SEPARATOR + "/catalogManagement/productTemplate/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productTemplates" );
            put( GET + SEPARATOR + "/catalogManagement/productTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productTemplates/{productTemplateCode}" );
            put( PUT + SEPARATOR + "/catalogManagement/productTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productTemplates/{productTemplateCode}" );
            put( DELETE + SEPARATOR + "/catalogManagement/productTemplate/{code}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productTemplates/{productTemplateCode}" );
            put( POST + SEPARATOR + "/catalogManagement/productTemplate/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productTemplates/{productTemplateCode}/enable" );
            put( POST + SEPARATOR + "/catalogManagement/productTemplate/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/productTemplates/{productTemplateCode}/disable" );

            // for entity Language
            put( POST + SEPARATOR + "/language", JaxRsActivatorGenericApiV1.REST_PATH + "/languages" );
            put( GET + SEPARATOR + "/language/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/languages" );
            put( GET + SEPARATOR + "/language", JaxRsActivatorGenericApiV1.REST_PATH + "/languages/{languageCode}" );
            put( PUT + SEPARATOR + "/language", JaxRsActivatorGenericApiV1.REST_PATH + "/languages/{languageCode}" );
            put( DELETE + SEPARATOR + "/language/{languageCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/languages/{languageCode}" );
            put( POST + SEPARATOR + "/language/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/languages/{languageCode}/enable" );
            put( POST + SEPARATOR + "/language/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/languages/{languageCode}/disable" );

            // for entity BillingAccount
            put( POST + SEPARATOR + "/account/billingAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/billingAccounts" );
            put( GET + SEPARATOR + "/account/billingAccount/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/billingAccounts" );
            put( GET + SEPARATOR + "/account/billingAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/billingAccounts/{billingAccountCode}" );
            put( PUT + SEPARATOR + "/account/billingAccount", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/billingAccounts/{billingAccountCode}" );
            put( DELETE + SEPARATOR + "/account/billingAccount/{billingAccountCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/billingAccounts/{billingAccountCode}" );
            put( GET + SEPARATOR + "/account/billingAccount/list", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/customerAccounts/{customerAccountCode}/billingAccounts" );

            // for entity PricePlan
            put( POST + SEPARATOR + "/catalog/pricePlan", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/pricePlans" );
            put( GET + SEPARATOR + "/catalog/pricePlan/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/pricePlans" );
            put( GET + SEPARATOR + "/catalog/pricePlan", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/pricePlans/{pricePlanCode}" );
            put( PUT + SEPARATOR + "/catalog/pricePlan", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/pricePlans/{pricePlanCode}" );
            put( DELETE + SEPARATOR + "/catalog/pricePlan/{pricePlanCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/pricePlans/{pricePlanCode}" );
            put( POST + SEPARATOR + "/catalog/pricePlan/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/pricePlans/{pricePlanCode}/enable" );
            put( POST + SEPARATOR + "/catalog/pricePlan/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/pricePlans/{pricePlanCode}/disable" );

            // for entity OfferTemplateCategory
            put( POST + SEPARATOR + "/catalog/offerTemplateCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplateCategories" );
            put( GET + SEPARATOR + "/catalog/offerTemplateCategory/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplateCategories" );
            put( GET + SEPARATOR + "/catalog/offerTemplateCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplateCategories/{offerTemplateCategoryCode}" );
            put( PUT + SEPARATOR + "/catalog/offerTemplateCategory", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplateCategories/{offerTemplateCategoryCode}" );
            put( DELETE + SEPARATOR + "/catalog/offerTemplateCategory/{offerTemplateCategoryCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplateCategories/{offerTemplateCategoryCode}" );
            put( POST + SEPARATOR + "/catalog/offerTemplateCategory/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplateCategories/{offerTemplateCategoryCode}/enable" );
            put( POST + SEPARATOR + "/catalog/offerTemplateCategory/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/offerTemplateCategories/{offerTemplateCategoryCode}/disable" );

            // for entity DiscountPlan
            put( POST + SEPARATOR + "/catalog/discountPlan", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlans" );
            put( GET + SEPARATOR + "/catalog/discountPlan/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlans" );
            put( GET + SEPARATOR + "/catalog/discountPlan", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlans/{discountPlanCode}" );
            put( PUT + SEPARATOR + "/catalog/discountPlan", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlans/{discountPlanCode}" );
            put( DELETE + SEPARATOR + "/catalog/discountPlan/{discountPlanCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlans/{discountPlanCode}" );
            put( POST + SEPARATOR + "/catalog/discountPlan/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlans/{discountPlanCode}/enable" );
            put( POST + SEPARATOR + "/catalog/discountPlan/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/discountPlans/{discountPlanCode}/disable" );

            // for entity Subscription
            put( POST + SEPARATOR + "/billing/subscription", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions" );
            put( GET + SEPARATOR + "/billing/subscription/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions" );
            put( GET + SEPARATOR + "/billing/subscription", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions/{subscriptionCode}" );
            put( PUT + SEPARATOR + "/billing/subscription", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions/{subscriptionCode}" );
            put( PUT + SEPARATOR + "/billing/subscription/activate", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions/{subscriptionCode}/activation" );
            put( PUT + SEPARATOR + "/billing/subscription/suspend", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions/{subscriptionCode}/suspension" );
            put( PUT + SEPARATOR + "/billing/subscription/updateServices", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions/{subscriptionCode}/services" );
            put( PUT + SEPARATOR + "/billing/subscription/terminate", JaxRsActivatorGenericApiV1.REST_PATH + "/accountManagement/subscriptions/{subscriptionCode}/termination" );

            // for entity UsageChargeTemplate
            put( POST + SEPARATOR + "/catalog/usageChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/usageChargeTemplates" );
            put( GET + SEPARATOR + "/catalog/usageChargeTemplate/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/usageChargeTemplates" );
            put( GET + SEPARATOR + "/catalog/usageChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/usageChargeTemplates/{usageChargeTemplateCode}" );
            put( PUT + SEPARATOR + "/catalog/usageChargeTemplate", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/usageChargeTemplates/{usageChargeTemplateCode}" );
            put( DELETE + SEPARATOR + "/catalog/usageChargeTemplate/{usageChargeTemplateCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/usageChargeTemplates/{usageChargeTemplateCode}" );
            put( POST + SEPARATOR + "/catalog/usageChargeTemplate/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/usageChargeTemplates/{usageChargeTemplateCode}/enable" );
            put( POST + SEPARATOR + "/catalog/usageChargeTemplate/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/catalog/usageChargeTemplates/{usageChargeTemplateCode}/disable" );

            // for entity Entity
            put( POST + SEPARATOR + "/entityCustomization/entity", JaxRsActivatorGenericApiV1.REST_PATH + "/entityCustomization/entities" );
            put( GET + SEPARATOR + "/entityCustomization/entity/list", JaxRsActivatorGenericApiV1.REST_PATH + "/entityCustomization/entities" );
            put( GET + SEPARATOR + "/entityCustomization/entity", JaxRsActivatorGenericApiV1.REST_PATH + "/entityCustomization/entities/{entityCode}" );
            put( PUT + SEPARATOR + "/entityCustomization/entity", JaxRsActivatorGenericApiV1.REST_PATH + "/entityCustomization/entities/{entityCode}" );
            put( DELETE + SEPARATOR + "/entityCustomization/entity/{customEntityTemplateCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/entityCustomization/entities/{entityCode}" );
            put( POST + SEPARATOR + "/entityCustomization/entity/{code}/enable", JaxRsActivatorGenericApiV1.REST_PATH + "/entityCustomization/entities/{entityCode}/enable" );
            put( POST + SEPARATOR + "/entityCustomization/entity/{code}/disable", JaxRsActivatorGenericApiV1.REST_PATH + "/entityCustomization/entities/{entityCode}/disable" );

            // for entity LanguageIso
            put( POST + SEPARATOR + "/languageIso", JaxRsActivatorGenericApiV1.REST_PATH + "/languagesIso" );
            put( GET + SEPARATOR + "/languageIso/listGetAll", JaxRsActivatorGenericApiV1.REST_PATH + "/languagesIso" );
            put( GET + SEPARATOR + "/languageIso", JaxRsActivatorGenericApiV1.REST_PATH + "/languagesIso/{languageCode}" );
            put( PUT + SEPARATOR + "/languageIso", JaxRsActivatorGenericApiV1.REST_PATH + "/languagesIso/{languageCode}" );
            put( DELETE + SEPARATOR + "/languageIso/{languageCode}", JaxRsActivatorGenericApiV1.REST_PATH + "/languagesIso/{languageCode}" );


        }
    };

}
