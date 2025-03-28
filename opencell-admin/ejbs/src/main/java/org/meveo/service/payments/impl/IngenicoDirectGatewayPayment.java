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

package org.meveo.service.payments.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import com.onlinepayments.ApiException;
import com.onlinepayments.Client;
import com.onlinepayments.CommunicatorConfiguration;
import com.onlinepayments.Factory;
import com.onlinepayments.Marshaller;
import com.onlinepayments.defaultimpl.DefaultMarshaller;
import com.onlinepayments.domain.APIError;
import com.onlinepayments.domain.Address;
import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.BankAccountIban;
import com.onlinepayments.domain.BrowserData;
import com.onlinepayments.domain.CapturePaymentRequest;
import com.onlinepayments.domain.CaptureResponse;
import com.onlinepayments.domain.Card;
import com.onlinepayments.domain.CardPaymentMethodSpecificInput;
import com.onlinepayments.domain.CardPaymentMethodSpecificInputBase;
import com.onlinepayments.domain.CardPayoutMethodSpecificInput;
import com.onlinepayments.domain.CardRecurrenceDetails;
import com.onlinepayments.domain.ContactDetails;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.onlinepayments.domain.CreateMandateRequest;
import com.onlinepayments.domain.CreateMandateResponse;
import com.onlinepayments.domain.CreatePaymentRequest;
import com.onlinepayments.domain.CreatePaymentResponse;
import com.onlinepayments.domain.CreatePayoutRequest;
import com.onlinepayments.domain.CreateTokenRequest;
import com.onlinepayments.domain.CreatedTokenResponse;
import com.onlinepayments.domain.Customer;
import com.onlinepayments.domain.CustomerDevice;
import com.onlinepayments.domain.GetHostedCheckoutResponse;
import com.onlinepayments.domain.GetMandateResponse;
import com.onlinepayments.domain.HostedCheckoutSpecificInput;
import com.onlinepayments.domain.MandateAddress;
import com.onlinepayments.domain.MandateContactDetails;
import com.onlinepayments.domain.MandateCustomer;
import com.onlinepayments.domain.MandatePersonalInformation;
import com.onlinepayments.domain.MandatePersonalName;
import com.onlinepayments.domain.MandateResponse;
import com.onlinepayments.domain.Order;
import com.onlinepayments.domain.OrderReferences;
import com.onlinepayments.domain.PaymentProductFilter;
import com.onlinepayments.domain.PaymentProductFiltersHostedCheckout;
import com.onlinepayments.domain.PaymentReferences;
import com.onlinepayments.domain.PaymentResponse;
import com.onlinepayments.domain.PaymentStatusOutput;
import com.onlinepayments.domain.PayoutResponse;
import com.onlinepayments.domain.PersonalInformation;
import com.onlinepayments.domain.PersonalName;
import com.onlinepayments.domain.RedirectionData;
import com.onlinepayments.domain.SepaDirectDebitPaymentMethodSpecificInput;
import com.onlinepayments.domain.SepaDirectDebitPaymentProduct771SpecificInput;
import com.onlinepayments.domain.ThreeDSecure;
import com.onlinepayments.domain.ThreeDSecureBase;
import com.onlinepayments.domain.ThreeDSecureData;
import com.onlinepayments.domain.TokenCardSpecificInput;
import com.onlinepayments.domain.TokenData;

import org.apache.commons.collections4.CollectionUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.payment.HostedCheckoutInput;
import org.meveo.api.dto.payment.HostedCheckoutStatusResponseDto;
import org.meveo.api.dto.payment.MandatInfoDto;
import org.meveo.api.dto.payment.PaymentHostedCheckoutResponseDto;
import org.meveo.api.dto.payment.PaymentResponseDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.commons.utils.EjbUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.billing.Invoice;
import org.meveo.model.payments.CardPaymentMethod;
import org.meveo.model.payments.CreditCardTypeEnum;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.DDPaymentMethod;
import org.meveo.model.payments.DDRequestLOT;
import org.meveo.model.payments.MandatStateEnum;
import org.meveo.model.payments.PaymentGateway;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.model.payments.PaymentStatusEnum;
import org.meveo.service.crm.impl.ProviderService;
import org.meveo.util.PaymentGatewayClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IngenicoDirect gateway based on Direct API Reference Direct specifications
 * (2.60.0).
 *
 * @author anasseh
 *
 */

@PaymentGatewayClass
public class IngenicoDirectGatewayPayment implements GatewayPaymentInterface {

	public static final String CHANGE_IT = "changeIt";
	/** The log. */
	protected Logger log = LoggerFactory.getLogger(IngenicoDirectGatewayPayment.class);

	/** The payment gateway. */
	private PaymentGateway paymentGateway = null;

	/** The client. */
	private Client client = null;

	private Marshaller marshaller = null;

	private CustomerAccountService customerAccountService;
   
	private ProviderService providerService = null;

	@Override
	public void setPaymentGateway(PaymentGateway paymentGateway) {
		this.paymentGateway = paymentGateway;

	}

	@Override
	public Object getClientObject() {
		if (client == null) {
			connect();
		}
		return client;
	}

	@Override
	public String createCardCvvToken(CustomerAccount customerAccount, String alias, String cardNumber,
			String cardHolderName, String expirayDate, String issueNumber, CreditCardTypeEnum cardType, String cvv)
			throws BusinessException {
		try {

			if (StringUtils.isBlank(cardHolderName)) {
				throw new BusinessException("Missing cardHolderName");
			}
			if (StringUtils.isBlank(cardNumber)) {
				throw new BusinessException("Missing cardNumber");
			}
			if (StringUtils.isBlank(cvv)) {
				throw new BusinessException("Missing cvv");
			}
			if (cardType == null) {
				throw new BusinessException("Missing cardType");
			}
			if (StringUtils.isBlank(expirayDate)) {
				throw new BusinessException("Missing expirayDate");
			}

			TokenCardSpecificInput tokenCardSpecificInput = new TokenCardSpecificInput();
			TokenData tokenData = new TokenData();
			Card card = new Card();
			card.setCardholderName(cardHolderName);
			card.setCardNumber(cardNumber);
			card.setCvv(cvv);
			card.setExpiryDate(expirayDate);

			tokenData.setCard(card);
			tokenCardSpecificInput.setData(tokenData);
			CreateTokenRequest body = new CreateTokenRequest();
			body.setCard(tokenCardSpecificInput);

			CreatedTokenResponse response = getClient().merchant(paymentGateway.getMarchandId()).tokens()
					.createToken(body);
			if (!response.getIsNewToken()) {
				throw new BusinessException(
						"A token already exist for card:" + CardPaymentMethod.hideCardNumber(cardNumber));
			}
			return response.getToken();
		} catch (ApiException ev) {
			throw new BusinessException(ev.getResponseBody());

		} catch (Exception e) {
			throw new BusinessException(e.getMessage());
		}
	}

	@Override
	public PaymentResponseDto doPaymentToken(CardPaymentMethod paymentToken, Long ctsAmount,
			Map<String, Object> additionalParams) throws BusinessException {

		return doPayment(null, paymentToken, ctsAmount, paymentToken.getCustomerAccount(), null, null, null, null, null,
				null, additionalParams);
	}

	@Override
	public PaymentResponseDto doPaymentCard(CustomerAccount customerAccount, Long ctsAmount, String cardNumber,
			String ownerName, String cvv, String expirayDate, CreditCardTypeEnum cardType, String countryCode,
			Map<String, Object> additionalParams) throws BusinessException {
		return doPayment(null, null, ctsAmount, customerAccount, cardNumber, ownerName, cvv, expirayDate, cardType,
				countryCode, additionalParams);
	}

	@Override
	public PaymentResponseDto doPaymentSepa(DDPaymentMethod paymentToken, Long ctsAmount,
			Map<String, Object> additionalParams) throws BusinessException {
		return doPayment(paymentToken, null, ctsAmount, paymentToken.getCustomerAccount(), null, null, null, null, null,
				null, additionalParams);
	}

	@Override
	public PaymentResponseDto doRefundSepa(DDPaymentMethod paymentToken, Long ctsAmount,
			Map<String, Object> additionalParams) throws BusinessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public PaymentResponseDto checkPayment(String paymentID, PaymentMethodEnum paymentMethodType)
			throws BusinessException {
		PaymentResponseDto doPaymentResponseDto = new PaymentResponseDto();
		try {
			PaymentResponse paymentResponse = getClient().merchant(paymentGateway.getMarchandId()).payments()
					.getPayment(paymentID);
			if (paymentResponse != null) {
				String errorMessage = "";
				doPaymentResponseDto.setPaymentID(paymentID);
				doPaymentResponseDto.setPaymentStatus(mappingStaus(paymentResponse.getStatus()));
				if (paymentResponse.getStatusOutput() != null) {
					if (paymentResponse.getStatusOutput().getErrors() != null) {
						for (APIError apiError : paymentResponse.getStatusOutput().getErrors()) {
							errorMessage = errorMessage + apiError.getMessage() + "\n";
						}
					}
				}
				doPaymentResponseDto.setErrorMessage(errorMessage);
				return doPaymentResponseDto;
			} else {
				throw new BusinessException("Gateway response is null");
			}
		} catch (ApiException e) {
			throw new BusinessException(e.getResponseBody());
		}
	}

	@Override
	public void cancelPayment(String paymentID) throws BusinessException {
		try {
			getClient().merchant(paymentGateway.getMarchandId()).payments().cancelPayment(paymentID);
		} catch (ApiException e) {
			throw new BusinessException(e.getResponseBody());
		}

	}

	@Override
	public void doBulkPaymentAsService(DDRequestLOT ddRequestLot) throws BusinessException {
		throw new UnsupportedOperationException();

	}

	@Override
	public PaymentResponseDto doRefundToken(CardPaymentMethod paymentToken, Long ctsAmount,
			Map<String, Object> additionalParams) throws BusinessException {
		PaymentResponseDto doPaymentResponseDto = new PaymentResponseDto();
		doPaymentResponseDto.setPaymentStatus(PaymentStatusEnum.NOT_PROCESSED);
		try {
			CustomerAccount customerAccount = paymentToken.getCustomerAccount();
			AmountOfMoney amountOfMoney = new AmountOfMoney();
			amountOfMoney.setAmount(ctsAmount);
			amountOfMoney.setCurrencyCode(customerAccount.getTradingCurrency().getCurrencyCode());

			CardPayoutMethodSpecificInput cardPayoutMethodSpecificInput = new CardPayoutMethodSpecificInput();
			cardPayoutMethodSpecificInput.setToken(paymentToken.getTokenId());
			cardPayoutMethodSpecificInput.setPaymentProductId(paymentToken.getCardType().getId());

			PaymentReferences paymentReferences = new PaymentReferences();
			paymentReferences.setMerchantReference(
					customerAccount.getId() + "-" + amountOfMoney.getAmount() + "-" + System.currentTimeMillis());

			CreatePayoutRequest body = new CreatePayoutRequest();
			body.setAmountOfMoney(amountOfMoney);
			body.setCardPayoutMethodSpecificInput(cardPayoutMethodSpecificInput);
			body.setReferences(paymentReferences);

			getClient();
			log.info("REQUEST:" + marshaller.marshal(body));
			PayoutResponse response = getClient().merchant(paymentGateway.getMarchandId()).payouts().createPayout(body);
			if (response != null) {
				log.info("RESPONSE:" + marshaller.marshal(response));

				doPaymentResponseDto.setPaymentID(response.getId());
				doPaymentResponseDto.setPaymentStatus(mappingStaus(response.getStatus()));

				doPaymentResponseDto.setTransactionId(response.getId());
				doPaymentResponseDto.setBankRefenrence(response.getId());
				if (response.getStatusOutput() != null
						&& doPaymentResponseDto.getPaymentStatus() == PaymentStatusEnum.REJECTED) {
					doPaymentResponseDto.setErrorCode("" + response.getStatusOutput().getStatusCode());
					doPaymentResponseDto.setErrorMessage(response.getStatus());
				}

			} else {
				throw new BusinessException("Gateway response is null");
			}
		} catch (ApiException e) {
			log.error("Error on doRefundToken :", e);
			doPaymentResponseDto.setPaymentStatus(PaymentStatusEnum.ERROR);
			doPaymentResponseDto.setErrorMessage(e.getResponseBody());
			if (CollectionUtils.isNotEmpty(e.getErrors())) {
				doPaymentResponseDto.setErrorCode(e.getErrors().get(0).getId());
			}
		}
		return doPaymentResponseDto;
	}

	@Override
	public PaymentResponseDto doRefundCard(CustomerAccount customerAccount, Long ctsAmount, String cardNumber,
			String ownerName, String cvv, String expirayDate, CreditCardTypeEnum cardType, String countryCode,
			Map<String, Object> additionalParams) throws BusinessException {
		throw new UnsupportedOperationException();
	}

	@Override
	 public MandatInfoDto checkMandat(String mandatReference, String mandateId) throws BusinessException {
		 MandatInfoDto mandatInfoDto=new MandatInfoDto();
		 try {
			 GetMandateResponse response = getClient().merchant(paymentGateway.getMarchandId()).mandates().getMandate(mandatReference); 
			 MandateResponse mandatResponse=response.getMandate();
			 if(mandatResponse!=null) { 
				 if("WAITING_FOR_REFERENCE".equals(mandatResponse.getStatus())) {
					 mandatInfoDto.setState(MandatStateEnum.waitingForReference); 
				 }else {
					 mandatInfoDto.setState(MandatStateEnum.valueOf(mandatResponse.getStatus().toLowerCase()));
				 }
				 mandatInfoDto.setReference(mandatResponse.getUniqueMandateReference());
				 mandatInfoDto.setCustomer(mandatResponse.getCustomerReference());
			 }  
		 }catch(ApiException  e) {
			 JsonReader reader = Json.createReader(new StringReader(e.getResponseBody()));
			 JsonObject jsonObject = reader.readObject();
			 JsonArray errorsArray = jsonObject.getJsonArray("errors");
			 JsonObject firstError = errorsArray.getJsonObject(0);
			 String message = firstError.getString("message");
			 throw new EntityDoesNotExistsException(message);
		 }
		 return mandatInfoDto;

	 }
	

	@Override
	public PaymentHostedCheckoutResponseDto getHostedCheckoutUrl(HostedCheckoutInput hostedCheckoutInput)
			throws BusinessException {
		try {
			String returnUrl = hostedCheckoutInput.getReturnUrl();
			Long id = hostedCheckoutInput.getCustomerAccountId();
			String timeMillisWithcustomerAccountId = System.currentTimeMillis() + "_-_" + id;

			log.info("hostedCheckoutInput.isOneShotPayment(): " + hostedCheckoutInput.isOneShotPayment());

			if (hostedCheckoutInput.isOneShotPayment()) {
				timeMillisWithcustomerAccountId = "oneShot_" + timeMillisWithcustomerAccountId;
			}

			String redirectionUrl;
			CustomerAccount ca = customerAccountService().findByCode(hostedCheckoutInput.getCustomerAccountCode());
			String firstName = null;
			String lastName = null;
			if (ca.getName() != null) {
				firstName = ca.getName().getFirstName();
				lastName = ca.getName().getLastName();
			}

			if (StringUtils.isBlank(firstName)) {
				throw new BusinessException("Missing firstName");
			}
			if (StringUtils.isBlank(lastName)) {
				throw new BusinessException("Missing lastName");
			}
			

			String phone = ca.getContactInformationNullSafe().getMobile();
			String email = ca.getContactInformationNullSafe().getEmail();

			if (StringUtils.isBlank(phone)) {
				throw new BusinessException("Missing Phone");
			}

			if (StringUtils.isBlank(email)) {
				throw new BusinessException("Missing Email");
			}

			HostedCheckoutSpecificInput hostedCheckoutSpecificInput = new HostedCheckoutSpecificInput();
			hostedCheckoutSpecificInput.setLocale(hostedCheckoutInput.getLocale());
			hostedCheckoutSpecificInput.setVariant(hostedCheckoutInput.getVariant());
			hostedCheckoutSpecificInput.setReturnUrl(returnUrl);
			hostedCheckoutSpecificInput.setIsRecurring(false);

			PaymentProductFiltersHostedCheckout dd = new PaymentProductFiltersHostedCheckout();
			PaymentProductFilter cc = new PaymentProductFilter();
			cc.setProducts(getListProductIds());
			dd.setRestrictTo(cc);
			hostedCheckoutSpecificInput.setPaymentProductFilters(dd);

			AmountOfMoney amountOfMoney = new AmountOfMoney();
			amountOfMoney.setAmount(Long.valueOf(hostedCheckoutInput.getAmount()));
			amountOfMoney.setCurrencyCode(hostedCheckoutInput.getCurrencyCode());

			Address billingAddress = new Address();
			billingAddress.setCountryCode(hostedCheckoutInput.getCountryCode());

			Customer customer = new Customer();
			customer.setBillingAddress(billingAddress);
			customer.setMerchantCustomerId("CA_ID_" + id);
			customer.setDevice(getDeviceData());
			PersonalInformation personalInformation = new PersonalInformation();
			PersonalName personalName = new PersonalName();
			personalName.setFirstName(firstName);
			personalName.setSurname(lastName);
			personalInformation.setName(personalName);
			customer.setPersonalInformation(personalInformation);
			ContactDetails contactDetails = new ContactDetails();
			contactDetails.setEmailAddress(email);
			contactDetails.setPhoneNumber(phone);
			customer.setContactDetails(contactDetails);

			OrderReferences orderReferences = new OrderReferences();
			orderReferences.setMerchantReference(timeMillisWithcustomerAccountId);

			Order order = new Order();
			order.setAmountOfMoney(amountOfMoney);
			order.setCustomer(customer);
			order.setReferences(orderReferences);

			CardPaymentMethodSpecificInputBase cardPaymentMethodSpecificInputBase = new CardPaymentMethodSpecificInputBase();
			// cardPaymentMethodSpecificInputBase.setRequiresApproval(true);
			cardPaymentMethodSpecificInputBase.setAuthorizationMode(hostedCheckoutInput.getAuthorizationMode());
			cardPaymentMethodSpecificInputBase.setTokenize(true);

			AmountOfMoney amountOfMoney3DS = new AmountOfMoney();
			amountOfMoney3DS.setAmount(Long.valueOf(hostedCheckoutInput.getAuthenticationAmount()));
			amountOfMoney3DS.setCurrencyCode(hostedCheckoutInput.getCurrencyCode());

			ThreeDSecureBase threeDSecure = new ThreeDSecureBase();
			// threeDSecure.setAuthenticationAmount(amountOfMoney3DS);
			threeDSecure.setSkipAuthentication(hostedCheckoutInput.isSkipAuthentication());
			cardPaymentMethodSpecificInputBase.setThreeDSecure(threeDSecure);

			CardRecurrenceDetails cardRecurrenceDetails = new CardRecurrenceDetails();
			cardRecurrenceDetails.setRecurringPaymentSequenceIndicator(
					paramBean().getProperty("ingenico.HostedCheckout.RecurringPaymentSequenceIndicator", ""));
			cardPaymentMethodSpecificInputBase.setRecurring(cardRecurrenceDetails);

			cardPaymentMethodSpecificInputBase.setUnscheduledCardOnFileRequestor(paramBean()
					.getProperty("ingenico.HostedCheckout.UnscheduledCardOnFileRequestor", "cardholderInitiated"));
			cardPaymentMethodSpecificInputBase.setUnscheduledCardOnFileSequenceIndicator(
					paramBean().getProperty("ingenico.HostedCheckout.UnscheduledCardOnFileSequenceIndicator", "first"));

			CreateHostedCheckoutRequest body = new CreateHostedCheckoutRequest();
			body.setHostedCheckoutSpecificInput(hostedCheckoutSpecificInput);
			body.setCardPaymentMethodSpecificInput(cardPaymentMethodSpecificInputBase);
			body.setOrder(order);
			getClient();
			log.info("REQUEST:" + marshaller.marshal(body));
			CreateHostedCheckoutResponse response = getClient().merchant(paymentGateway.getMarchandId())
					.hostedCheckout().createHostedCheckout(body);
			log.info("RESPONSE:" + marshaller.marshal(response));
			redirectionUrl = paramBean().getProperty("ingenico.hostedCheckoutUrl.prefix", "https://payment.")
					+ response.getPartialRedirectUrl();
			return new PaymentHostedCheckoutResponseDto(redirectionUrl, null, null, response.getHostedCheckoutId());

		} catch (Exception e) {
			log.error("Error on getHostedCheckoutUrl:", e);
			throw new BusinessException(e.getMessage());
		}
	}

	@Override
	public HostedCheckoutStatusResponseDto getHostedCheckoutStatus(String id) throws BusinessException {
		try {
			GetHostedCheckoutResponse response = getClient().merchant(paymentGateway.getMarchandId()).hostedCheckout()
					.getHostedCheckout(id);
			log.info("RESPONSE:" + marshaller.marshal(response));

			HostedCheckoutStatusResponseDto hostedCheckoutStatusResponseDto = new HostedCheckoutStatusResponseDto();
			hostedCheckoutStatusResponseDto.setHostedCheckoutStatus(response.getStatus());
			if (response.getCreatedPaymentOutput() != null && response.getCreatedPaymentOutput().getPayment() != null) {
				hostedCheckoutStatusResponseDto.setPaymentId(response.getCreatedPaymentOutput().getPayment().getId());
				hostedCheckoutStatusResponseDto
						.setPaymentStatus(mappingStaus(response.getCreatedPaymentOutput().getPayment().getStatus()));
			}
			return hostedCheckoutStatusResponseDto;

		} catch (Exception e) {
			log.error("Error on getHostedCheckoutStatus:", e);
			throw new BusinessException(e.getMessage());
		}
	}

	@Override
	public String createInvoice(Invoice invoice) throws BusinessException {
		 throw new UnsupportedOperationException();
	}

	@Override
	public String createSepaDirectDebitToken(CustomerAccount customerAccount, String alias, String accountHolderName,
			String iban) throws BusinessException {
		 return null;
	}

	
	  private ProviderService getProviderService() {
	    	if(providerService != null) {
	    		return providerService;
	    	}
	    	providerService = (ProviderService) EjbUtils.getServiceInterface(ProviderService.class.getSimpleName());
	    	return providerService;
	    }
	  
	  /**
		 * Ingenico Sepa does not support some characters when creating the mandate, it throws invalid format if data contains "/","&",digits (in the city field), and accents
		 * @param data
		 * @param stripDigits
		 */
		private String formatIngenicoData(String data, boolean stripDigits) {
			String formatData = paramBean().getProperty("ingenico.formatData", "true");
			String insuportedCharaters = paramBean().getProperty("ingenico.formatData.insupportedCharacters.", "/,&");
			if(Boolean.parseBoolean(formatData)) {
				for(String character:insuportedCharaters.split(",")) {
					data=data.replaceAll(character, " ");
					data = org.apache.commons.lang3.StringUtils.stripAccents(data);
				}
				
				data = org.apache.commons.lang3.StringUtils.stripAccents(data);
				if(stripDigits) {
					data=data.replaceAll("\\d", "");
				}
			}
			return data;
		}
	@Override
	public void createMandate(CustomerAccount customerAccount, String iban, String mandateReference)
			throws BusinessException {	
		
		try {
			try {
				String customerCode=customerAccount.getExternalRef1();
				MandatInfoDto mandateDto=checkMandat(mandateReference, null);
				if(customerCode!=null&& mandateDto.getReference()!=null && mandateDto.getCustomer()!=null) {
					if(!customerCode.equals(mandateDto.getCustomer())) {
						throw new EntityAlreadyExistsException("The mandate: " + mandateReference+ " already exist and is attached to another customer"); 
					}else {
						log.info("The mandate: {} already exist for this customer: {}",mandateReference,customerCode);
						return;
					}		
				}		
			}catch(EntityDoesNotExistsException e) {	 
			}
            BankAccountIban bankAccountIban = new BankAccountIban();
            bankAccountIban.setIban(iban);

            MandateContactDetails contactDetails = new MandateContactDetails();
            if (customerAccount.getContactInformation() != null) {
                contactDetails.setEmailAddress(customerAccount.getContactInformation().getEmail());
            }

            MandateAddress address = new MandateAddress();
            if (customerAccount.getAddress() != null) {
            	address.setCity(customerAccount.getAddress().getCity().replaceAll("\\d", ""));
                address.setCountryCode(customerAccount.getAddress().getCountry() == null ? null : customerAccount.getAddress().getCountry().getCountryCode());
                
                String address1=customerAccount.getAddress().getAddress1();
      		  if (address1!=null && address1.length() > 50) {
                    address1 = address1.substring(0, 50);
                }
      		    address.setStreet(formatIngenicoData(address1, false));
                address.setZip(customerAccount.getAddress().getZipCode());
            }
            MandatePersonalName name = new MandatePersonalName();
            MandatePersonalInformation personalInformation = new MandatePersonalInformation();
            boolean isEntreprise=getProviderService().getProvider().isEntreprise();
            if (customerAccount.getName() != null) {
    			name.setFirstName(isEntreprise?"-":formatIngenicoData(customerAccount.getName().getFirstName(), false));
    			name.setSurname(formatIngenicoData(customerAccount.getName().getLastName(), true)); 
    			personalInformation.setTitle(isEntreprise?"Mr":(customerAccount.getName().getTitle() == null ? "" : customerAccount.getName().getTitle().getDescription()));
    		} 
            personalInformation.setName(name);
            MandateCustomer customer = new MandateCustomer();
            customer.setBankAccountIban(bankAccountIban);
            customer.setContactDetails(contactDetails);
            customer.setMandateAddress(address);
            customer.setPersonalInformation(personalInformation);
            
        	if(isEntreprise) {
    			String customerLastName=customerAccount.getName().getLastName();
    			if (customerLastName!=null && customerLastName.length() > 40) {
    			    customerLastName = customerLastName.substring(0, 40);
    			}
    			customer.setCompanyName(formatIngenicoData(customerLastName, true));
    		}

            CreateMandateRequest body = new CreateMandateRequest();
            body.setUniqueMandateReference(mandateReference);
            body.setCustomer(customer);
            body.setCustomerReference(customerAccount.getCode());
            body.setRecurrenceType("RECURRING");
            body.setSignatureType("UNSIGNED");
            getClient();
            log.info("createMandate REQUEST:" + marshaller.marshal(body));
            CreateMandateResponse response = getClient().merchant(paymentGateway.getMarchandId()).mandates().createMandate(body);
            log.info("createMandate RESPONSE:" + marshaller.marshal(response));
           
		}catch (ApiException ev) { 
			JsonReader reader = Json.createReader(new StringReader(ev.getResponseBody()));
			JsonObject jsonObject = reader.readObject();
			JsonArray errorsArray = jsonObject.getJsonArray("errors");
			JsonObject firstError = errorsArray.getJsonObject(0);
			String message = firstError.getString("message");
			throw new MeveoApiException(message);
	   }catch (Exception e) { 
		   throw new MeveoApiException(e.getMessage());
		
		
	}

    }

	@Override
	public void approveSepaDDMandate(String token, Date signatureDate) throws BusinessException {
		throw new UnsupportedOperationException();

	}

	private void connect() {
		ParamBean paramBean = paramBean();
		// Init properties

		paramBean.getProperty("onlinePayments.api.endpoint.host", "payment.preprod.direct.worldline-solutions.com");
		paramBean.getProperty("onlinePayments.api.endpoint.scheme", "https");
		paramBean.getProperty("onlinePayments.api.endpoint.port", "443");

		CommunicatorConfiguration communicatorConfiguration = new CommunicatorConfiguration(
				ParamBean.getInstance().getProperties());
		communicatorConfiguration.setApiKeyId(paymentGateway.getApiKey());
		communicatorConfiguration.setSecretApiKey(paymentGateway.getSecretKey());

		client = (Client) Factory.createClient(communicatorConfiguration);
		marshaller = DefaultMarshaller.INSTANCE;
	}

	private Client getClient() {
		if (client == null) {
			connect();
		}
		return client;
	}

	private ParamBean paramBean() {
		ParamBeanFactory paramBeanFactory = (ParamBeanFactory) EjbUtils
				.getServiceInterface(ParamBeanFactory.class.getSimpleName());
		ParamBean paramBean = paramBeanFactory.getInstance();
		return paramBean;
	}

	private CreatePaymentRequest buildPaymentRequest(DDPaymentMethod ddPaymentMethod,
			CardPaymentMethod paymentCardToken, Long ctsAmount, CustomerAccount customerAccount, String cardNumber,
			String ownerName, String cvv, String expirayDate, CreditCardTypeEnum cardType) {

		CreatePaymentRequest body = new CreatePaymentRequest();

		AmountOfMoney amountOfMoney = new AmountOfMoney();
		amountOfMoney.setAmount(ctsAmount);
		String currencyCode=customerAccount.getTradingCurrency().getCurrencyCode()!=null?customerAccount.getTradingCurrency().getCurrencyCode():getProviderService().getProvider().getCurrency().getCurrencyCode();
		amountOfMoney.setCurrencyCode(currencyCode);

		Customer customer = new Customer();
		customer.setBillingAddress(getBillingAddress(customerAccount));
		if ("true".equals(paramBean().getProperty("ingenico.CreatePayment.includeDeviceData", "true"))) {
			customer.setDevice(getDeviceData());
		}

		Order order = new Order();
		order.setAmountOfMoney(amountOfMoney);
		order.setCustomer(customer);

		if (ddPaymentMethod != null) {
			body.setSepaDirectDebitPaymentMethodSpecificInput(getSepaInput(ddPaymentMethod));
		}
		if (paymentCardToken != null) {
			body.setCardPaymentMethodSpecificInput(getCardTokenInput(paymentCardToken));
		}
		if (!StringUtils.isBlank(cardNumber)) {
			body.setCardPaymentMethodSpecificInput((getCardInput(cardNumber, ownerName, cvv, expirayDate, cardType)));
		}

		body.setOrder(order);

		return body;
	}

	/**
	 * Gets the sepa input.
	 *
	 * @param ddPaymentMethod the dd payment method
	 * @return the sepa input
	 */
	 private SepaDirectDebitPaymentMethodSpecificInput getSepaInput(DDPaymentMethod ddPaymentMethod) {
	        SepaDirectDebitPaymentMethodSpecificInput sepaPmInput = new SepaDirectDebitPaymentMethodSpecificInput();
	        sepaPmInput.setPaymentProductId(771);
	        String mandateRef=ddPaymentMethod.getMandateIdentification();
	        if(mandateRef!=null) {
	        SepaDirectDebitPaymentProduct771SpecificInput sepaDirectDebitPaymentProduct771SpecificInput = new SepaDirectDebitPaymentProduct771SpecificInput();
	        MandatInfoDto mandateDto=checkMandat(mandateRef,null);
	        if(MandatStateEnum.blocked.equals(mandateDto.getState())) {
	        	GetMandateResponse response= getClient().merchant(paymentGateway.getMarchandId()).mandates().unblockMandate(mandateRef);
	        	log.info("Reactivate mandate={}, status={}",mandateRef,response.getMandate().getStatus());
	        }
	        sepaDirectDebitPaymentProduct771SpecificInput.setExistingUniqueMandateReference(mandateRef);
	        sepaPmInput.setPaymentProduct771SpecificInput(sepaDirectDebitPaymentProduct771SpecificInput);
	        }
	        return sepaPmInput;
	    }
	
	 
	

	/**
	 * Gets the card input.
	 *
	 * @param cardNumber  the card number
	 * @param ownerName   the owner name
	 * @param cvv         the cvv
	 * @param expirayDate the expiray date
	 * @param cardType    the card type
	 * @return the card input
	 */
	private CardPaymentMethodSpecificInput getCardInput(String cardNumber, String ownerName, String cvv,
			String expirayDate, CreditCardTypeEnum cardType) {
		CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
		Card card = new Card();
		card.setCardNumber(cardNumber);
		card.setCardholderName(ownerName);
		card.setCvv(cvv);
		card.setExpiryDate(expirayDate);
		cardPaymentMethodSpecificInput.setCard(card);
		cardPaymentMethodSpecificInput.setPaymentProductId(cardType.getId());
		cardPaymentMethodSpecificInput.setAuthorizationMode(getAuthorizationMode());
		return cardPaymentMethodSpecificInput;
	}

	private PaymentResponseDto doPayment(DDPaymentMethod ddPaymentMethod, CardPaymentMethod paymentCardToken,
			Long ctsAmount, CustomerAccount customerAccount, String cardNumber, String ownerName, String cvv,
			String expirayDate, CreditCardTypeEnum cardType, String countryCode, Map<String, Object> additionalParams)
			throws BusinessException {
		PaymentResponseDto doPaymentResponseDto = new PaymentResponseDto();
		doPaymentResponseDto.setPaymentStatus(PaymentStatusEnum.NOT_PROCESSED);
		try {

			CreatePaymentRequest body = buildPaymentRequest(ddPaymentMethod, paymentCardToken, ctsAmount,
					customerAccount, cardNumber, ownerName, cvv, expirayDate, cardType);
			getClient();
			log.info("doPayment REQUEST :" + marshaller.marshal(body));

			CreatePaymentResponse response = getClient().merchant(paymentGateway.getMarchandId()).payments()
					.createPayment(body);

			if (response != null) {
				log.info("doPayment RESPONSE :" + marshaller.marshal(response));

				doPaymentResponseDto.setPaymentID(response.getPayment().getId());
				doPaymentResponseDto.setPaymentStatus(mappingStaus(response.getPayment().getStatus()));
				  if (response.getCreationOutput() != null) {
	                    doPaymentResponseDto.setTransactionId(response.getCreationOutput().getExternalReference());
	                    doPaymentResponseDto.setTokenId(response.getCreationOutput().getToken());
	                    if(response.getCreationOutput().getIsNewToken() != null) {
	                    	doPaymentResponseDto.setNewToken(response.getCreationOutput().getIsNewToken());
	                    }else {
	                    	doPaymentResponseDto.setNewToken(false);
	                    }
	                    
	                }
				PaymentResponse payment = response.getPayment();
				if (payment != null && response.getPayment().getStatusOutput().getErrors() != null) {
					PaymentStatusOutput statusOutput = payment.getStatusOutput();
					if (statusOutput != null) {
						List<APIError> errors = statusOutput.getErrors();
						if (CollectionUtils.isNotEmpty(errors)) {
							doPaymentResponseDto.setErrorMessage(errors.toString());
							doPaymentResponseDto.setErrorCode(errors.get(0).getId());
						}
					}
				}
				return doPaymentResponseDto;
			} else {
				throw new BusinessException("Gateway response is null");
			}
		} catch (ApiException e) {
			log.error("Error on doPayment :", e);
			doPaymentResponseDto.setPaymentStatus(PaymentStatusEnum.ERROR);
			doPaymentResponseDto.setErrorMessage(e.getResponseBody());
			if (CollectionUtils.isNotEmpty(e.getErrors())) {
				doPaymentResponseDto.setErrorCode(e.getErrors().get(0).getId());
			}
		}
		return doPaymentResponseDto;
	}

	private CustomerDevice getDeviceData() {
		CustomerDevice customerDevice = new CustomerDevice();
		customerDevice.setAcceptHeader(paramBean().getProperty("ingenico.device.AcceptHeader",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"));
		customerDevice.setLocale(paramBean().getProperty("ingenico.device.Locale", "fr_FR"));
		customerDevice
				.setTimezoneOffsetUtcMinutes(paramBean().getProperty("ingenico.device.TimezoneOffsetUtcMinutes", "60"));
		customerDevice.setUserAgent(paramBean().getProperty("ingenico.device.UserAgent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36"));
		BrowserData browserData = new BrowserData();
		browserData.setColorDepth(paramBean().getPropertyAsInteger("ingenico.device.ColorDepth", 24));
		browserData.setJavaEnabled("true".equals(paramBean().getProperty("ingenico.device.JavaEnabled", "false")));
		browserData.setScreenHeight(paramBean().getProperty("ingenico.device.ScreenHeight", "1080"));
		browserData.setScreenWidth(paramBean().getProperty("ingenico.device.ScreenWidth", "1920"));
		customerDevice.setBrowserData(browserData);
		return customerDevice;
	}

	/**
	 * Gets the billing address.
	 *
	 * @param customerAccount the customer account
	 * @return the billing address
	 */
	private Address getBillingAddress(CustomerAccount customerAccount) {
		Address billingAddress = new Address();
		if (customerAccount.getAddress() != null) {
			billingAddress
					.setAdditionalInfo(StringUtils.truncate(customerAccount.getAddress().getAddress3(), 50, true));
			billingAddress.setCity(StringUtils.truncate(customerAccount.getAddress().getCity(), 20, true));
			billingAddress.setCountryCode(customerAccount.getAddress().getCountry() == null ? null
					: customerAccount.getAddress().getCountry().getCountryCode());
			billingAddress.setHouseNumber("");
			billingAddress.setState(StringUtils.truncate(customerAccount.getAddress().getState(), 35, true));
			billingAddress.setStreet(StringUtils.truncate(customerAccount.getAddress().getAddress1(), 50, true));
			billingAddress.setZip(StringUtils.truncate(customerAccount.getAddress().getZipCode(), 8, true));
		}
		return billingAddress;
	}

	private CardPaymentMethodSpecificInput getCardTokenInput(CardPaymentMethod cardPaymentMethod) {
		if ("true".equals(paramBean().getProperty("ingenico.CreatePayment.use3DSecure", "true"))) {
			return getCardTokenInput3dSecure(cardPaymentMethod);
		}
		return getCardTokenInputDefault(cardPaymentMethod);

	}

	/**
	 * Gets the card token input.
	 *
	 * @param cardPaymentMethod the card payment method
	 * @return the card token input
	 */
	private CardPaymentMethodSpecificInput getCardTokenInput3dSecure(CardPaymentMethod cardPaymentMethod) {
		ParamBean paramBean = paramBean();
		CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
		cardPaymentMethodSpecificInput.setToken(cardPaymentMethod.getTokenId());

		ThreeDSecure threeDSecure = new ThreeDSecure();
		ThreeDSecureData threeDSecureData = new ThreeDSecureData();
		threeDSecureData.setAcsTransactionId(cardPaymentMethod.getToken3DsId());
		threeDSecure.setPriorThreeDSecureData(threeDSecureData);
		threeDSecure.setSkipAuthentication(Boolean
				.valueOf("true".equals(paramBean().getProperty("ingenico.CreatePayment.SkipAuthentication", "false"))));
		RedirectionData redirectionData = new RedirectionData();
		redirectionData.setReturnUrl(paramBean.getProperty("ingenico.urlReturnPayment", "changeIt"));
		threeDSecure.setRedirectionData(redirectionData);
		threeDSecure.setChallengeIndicator(paramBean.getProperty("ingenico.3ds.ChallengeIndicator", "no-preference"));
		// threeDSecure.setAuthenticationFlow(paramBean.getProperty("ingenico.3ds.AuthenticationFlow",
		// "browser"));
		threeDSecure.setChallengeCanvasSize(paramBean.getProperty("ingenico.3ds.ChallengeCanvasSize", "600x400"));
		cardPaymentMethodSpecificInput.setThreeDSecure(threeDSecure);
		cardPaymentMethodSpecificInput.setIsRecurring(
				Boolean.valueOf("true".equals(paramBean().getProperty("ingenico.CreatePayment.IsRecurring", "false"))));

		cardPaymentMethodSpecificInput.setAuthorizationMode(getAuthorizationMode());

		cardPaymentMethodSpecificInput.setUnscheduledCardOnFileRequestor(
				paramBean().getProperty("ingenico.CreatePayment.UnscheduledCardOnFileRequestor", "merchantInitiated"));
		cardPaymentMethodSpecificInput.setUnscheduledCardOnFileSequenceIndicator(
				paramBean().getProperty("ingenico.CreatePayment.UnscheduledCardOnFileSequenceIndicator", "subsequent"));

		CardRecurrenceDetails cardRecurrenceDetails = new CardRecurrenceDetails();
		cardRecurrenceDetails.setRecurringPaymentSequenceIndicator(
				paramBean().getProperty("ingenico.CreatePayment.RecurringPaymentSequenceIndicator", "null"));
		cardPaymentMethodSpecificInput.setRecurring(cardRecurrenceDetails);

		return cardPaymentMethodSpecificInput;
	}

	private CardPaymentMethodSpecificInput getCardTokenInputDefault(CardPaymentMethod cardPaymentMethod) {
		ParamBean paramBean = paramBean();
		CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
		cardPaymentMethodSpecificInput.setToken(cardPaymentMethod.getTokenId());
		cardPaymentMethodSpecificInput.setReturnUrl(paramBean.getProperty("ingenico.urlReturnPayment", "changeIt"));
		cardPaymentMethodSpecificInput.setIsRecurring(Boolean.TRUE);
		// cardPaymentMethodSpecificInput.setRecurringPaymentSequenceIndicator("recurring");
		cardPaymentMethodSpecificInput.setAuthorizationMode(getAuthorizationMode());
		return cardPaymentMethodSpecificInput;
	}

	/**
	 * Mapping staus.
	 *
	 * @param ingenicoStatus the ingenico status
	 * @return the payment status enum
	 */
	private PaymentStatusEnum mappingStaus(String ingenicoStatus) {
		if (ingenicoStatus == null) {
			return PaymentStatusEnum.ERROR;
		}
		if ("CREATED".equals(ingenicoStatus) || "PAID".equals(ingenicoStatus) || "REFUNDED".equals(ingenicoStatus)
				|| "CAPTURED".equals(ingenicoStatus)) {
			return PaymentStatusEnum.ACCEPTED;
		}
		if (ingenicoStatus.startsWith("PENDING")) {
			return PaymentStatusEnum.PENDING;
		}
		if (ingenicoStatus.equals("ACCOUNT_VERIFIED")) {
			return PaymentStatusEnum.PENDING;
		}
		if (ingenicoStatus.equals("AUTHORIZATION_REQUESTED")) {
			return PaymentStatusEnum.PENDING;
		}
		if (ingenicoStatus.equals("CAPTURE_REQUESTED")) {
			return PaymentStatusEnum.PENDING;
		}
		if (ingenicoStatus.equals("REJECTED_CAPTURE")) {
			return PaymentStatusEnum.REJECTED;
		}
		if (ingenicoStatus.equals("REVERSED")) {
			return PaymentStatusEnum.ACCEPTED;
		}
		if (ingenicoStatus.equals("CHARGEBACKED")) {
			return PaymentStatusEnum.ACCEPTED;
		}
		if (ingenicoStatus.equals("REFUND_REQUESTED")) {
			return PaymentStatusEnum.PENDING;
		}
		if (ingenicoStatus.equals("PAYOUT_REQUESTED")) {
			return PaymentStatusEnum.PENDING;
		}
		if (ingenicoStatus.equals("ACCOUNT_CREDITED")) {
			return PaymentStatusEnum.ACCEPTED;
		}
		if (ingenicoStatus.equals("ACCOUNT_CREDITED")) {
			return PaymentStatusEnum.ACCEPTED;
		}
		if (ingenicoStatus.equals("PENDING_APPROVAL")) {
			return PaymentStatusEnum.ACCEPTED;
		}

		return PaymentStatusEnum.REJECTED;
	}

	private String getAuthorizationMode() {
		return paramBean().getProperty("ingenico.api.authorizationMode", "SALE");
	}

	private List<Integer> getListProductIds() {
		List<Integer> listProduct = new ArrayList<Integer>();

		String productFilter = paramBean().getProperty("ingenico.HostedCheckout.ProductFilter",
				"1,2,3,122,114,119,130,771");
		for (String s : productFilter.split(",")) {
			listProduct.add(Integer.valueOf(s));
		}
		return listProduct;
	}
	 private CustomerAccountService customerAccountService() {
	        if (customerAccountService == null) {
	            customerAccountService = (CustomerAccountService) EjbUtils.getServiceInterface(CustomerAccountService.class.getSimpleName());
	        }

	        return customerAccountService;
	    }

	@Override
	public String createCardToken(CustomerAccount customerAccount, String alias, String cardNumber,
			String cardHolderName, String expirayDate, String issueNumber, CreditCardTypeEnum cardType)
			throws BusinessException {
		throw new BusinessException("Use createCardCvvToken instead.");
		
	}

	@Override
	public PaymentResponseDto capturePayment(String preAuthorisationId, Long ctsAmount, String merchantParameters)
			throws BusinessException {
		PaymentResponseDto doPaymentResponseDto = new PaymentResponseDto();
		doPaymentResponseDto.setPaymentStatus(PaymentStatusEnum.NOT_PROCESSED);
		try {
		CapturePaymentRequest capturePaymentRequest = new CapturePaymentRequest();
		capturePaymentRequest.setAmount(ctsAmount);
		capturePaymentRequest.setIsFinal(Boolean.TRUE);
		PaymentReferences paymentReferences = new PaymentReferences();
		paymentReferences.setMerchantParameters(preAuthorisationId);

		capturePaymentRequest.setReferences(null);

		CaptureResponse response = getClient().merchant(paymentGateway.getMarchandId()).payments().capturePayment(preAuthorisationId, capturePaymentRequest);
		log.info("CancelPaymentResponse:" + marshaller.marshal(response));
		
		if (response != null) {
			 if (isCaptured(response.getStatus())) {
				 doPaymentResponseDto.setPaymentID(response.getId());
				 doPaymentResponseDto.setPaymentStatus(PaymentStatusEnum.ACCEPTED);
			 }else {
				 doPaymentResponseDto.setPaymentStatus(PaymentStatusEnum.REJECTED);
				 doPaymentResponseDto.setErrorCode(response.getStatus());
			 }
		}
						
		} catch (ApiException e) {
			log.error("Error on capturePayment :", e);
			doPaymentResponseDto.setPaymentStatus(PaymentStatusEnum.ERROR);
			doPaymentResponseDto.setErrorMessage(e.getResponseBody());
			if (CollectionUtils.isNotEmpty(e.getErrors())) {
				doPaymentResponseDto.setErrorCode(e.getErrors().get(0).getId());
			}
		}
		return doPaymentResponseDto;

	}
	
	private boolean isCaptured(String status) {
		if ("CANCELLED".equals(status) || "REJECTED".equals(status) || "REJECTED_CAPTURE".equals(status)) {
			return false;
		}
		return true;
	}
}