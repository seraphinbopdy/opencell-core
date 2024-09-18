package org.meveo.apiv2.payments.resource;

import static java.util.Optional.ofNullable;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static jakarta.ws.rs.core.Response.ok;

import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.payment.PayByCardOrSepaDto;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.payment.PaymentApi;
import org.meveo.apiv2.payments.ImportRejectionCodeInput;
import org.meveo.apiv2.payments.PaymentGatewayInput;
import org.meveo.apiv2.payments.RejectionAction;
import org.meveo.apiv2.payments.RejectionCode;
import org.meveo.apiv2.payments.RejectionCodeClearInput;
import org.meveo.apiv2.payments.RejectionCodeDeleteInput;
import org.meveo.apiv2.payments.RejectionGroup;
import org.meveo.apiv2.payments.RejectionPayment;
import org.meveo.apiv2.payments.SequenceAction;
import org.meveo.apiv2.refund.CardRefund;
import org.meveo.model.payments.CreditCardTypeEnum;

@Interceptors({ WsRestApiInterceptor.class })
public class PaymentResourceImpl implements PaymentResource {

    @Inject
    private PaymentApi paymentApi;

    @Override
    public Response paymentByCard(CardRefund cardPayment) {
    	PayByCardOrSepaDto payByCardDto = toPayByCardDto(cardPayment);
        try {
            paymentApi.payByCard(payByCardDto);
            return Response.ok(new ActionStatus()).build();
        } catch (Exception e) {
            throw new ClientErrorException(e.getMessage(), PRECONDITION_FAILED);
        }
    }
    
	@Override
	public Response paymentBySepa(CardRefund cardPayment) {
		PayByCardOrSepaDto payByCardDto = toPayByCardDto(cardPayment);
        try {
            paymentApi.payBySepa(payByCardDto);
            return Response.ok(new ActionStatus()).build();
        } catch (Exception e) {
            throw new ClientErrorException(e.getMessage(), PRECONDITION_FAILED);
        }
    }
   

	private PayByCardOrSepaDto toPayByCardDto(CardRefund cardPayment) {
		PayByCardOrSepaDto payByCardDto = new PayByCardOrSepaDto();
        payByCardDto.setCtsAmount(cardPayment.getCtsAmount());
        payByCardDto.setCardNumber(cardPayment.getCardNumber());
        payByCardDto.setCustomerAccountCode(cardPayment.getCustomerAccountCode());
        payByCardDto.setOwnerName(cardPayment.getOwnerName());
        payByCardDto.setCvv(cardPayment.getCvv());
        payByCardDto.setExpiryDate(cardPayment.getExpiryDate());
        if(Objects.nonNull(cardPayment.getCardType())){
            payByCardDto.setCardType(CreditCardTypeEnum.valueOf(cardPayment.getCardType()));
        }
        payByCardDto.setAoToPay(cardPayment.getAoToPay());
        payByCardDto.setCreateAO(cardPayment.createAO());
        payByCardDto.setToMatch(cardPayment.toMatch());
        payByCardDto.setComment(cardPayment.getComment());
        return payByCardDto;
    }

    /**
     * Create payment rejection code
     *
     * @param rejectionCode payment rejection code
     * @return RejectionCode id
     */
    @Override
    public Response createRejectionCode(RejectionCode rejectionCode) {
        try {
            validateRejectionCodeInput(rejectionCode);
            Long id = paymentApi.createPaymentRejectionCode(rejectionCode);
            return ok()
                    .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                            ",\"message\":\"Rejection code successfully created\"},\"id\":" + id + "}")
                    .build();
        } catch (MissingParameterException missingParameterException) {
            return Response.status(PRECONDITION_FAILED)
                    .entity("{\"actionStatus\":{\"status\":\"FAIL\"" +
                                    ",\"message\":\"" + missingParameterException.getMessage() + "\"}")
                    .type(APPLICATION_JSON)
                    .build();
        } catch (Exception exception) {
            throw new BadRequestException(exception);
        }
    }

    private void validateRejectionCodeInput(RejectionCode rejectionCode) {
        ofNullable(rejectionCode.getPaymentGateway())
                .orElseThrow(() -> new MissingParameterException("Payment gateway is mandatory"));
    }

    /**
     * Update payment rejection code
     *
     * @param id payment rejection code id
     * @param rejectionCode rejection code input
     * @return RejectionCode updated result
     */
    @Override
    public Response updateRejectionCode(Long id, RejectionCode rejectionCode) {
        RejectionCode result = paymentApi.updatePaymentRejectionCode(id, rejectionCode);
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection code successfully updated\"},\"id\":" + result.getId() + "}")
                .build();
    }

    /**
     * Delete rejection code
     *
     * @param id payment rejection code id
     */
    @Override
    public Response removeRejectionCode(Long id, RejectionCodeDeleteInput rejectionCodeDeleteInput) {
        paymentApi.removeRejectionCode(id, rejectionCodeDeleteInput.getForce());
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection code successfully deleted\"}}")
                .build();
    }

    /**
     * Clear rejection codes by gateway
     *
     * @param clearInput payment gateway
     */
    @Override
    public Response clearAll(RejectionCodeClearInput clearInput) {
        return ok()
                .entity(paymentApi.clearAll(clearInput))
                .build();

    }

    /**
     * Import rejectionsCodes
     *
     * @param importRejectionCodeInput rejection code import input
     */
    @Override
    public Response importRejectionCodes(ImportRejectionCodeInput importRejectionCodeInput) {
        int numberOfImportedCodes = paymentApi.importRejectionCodes(importRejectionCodeInput);
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection codes successfully imported\"}," +
                        " \"numberOfImportedLines\":" + numberOfImportedCodes + "}")
                .build();
    }

    /**
     * Export rejectionsCodes by gateway
     *
     * @param paymentGateway payment gateway
     */
    @Override
    public Response export(PaymentGatewayInput paymentGateway) {
        paymentApi.export(paymentGateway);
        return ok()
                .entity(paymentApi.export(paymentGateway))
                .build();
    }

    /**
     * Create rejection action
     *
     * @param rejectionAction PaymentRejectionAction
     */
    @Override
    public Response createRejectionAction(RejectionAction rejectionAction) {
        final RejectionAction saved = paymentApi.createRejectionAction(rejectionAction);
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection action successfully created\"},\"id\":"
                        + saved.getId() + ", \"code\": \""
                        + saved.getCode() + "\"}")
                .build();
    }

    /**
     * update rejection action
     *
     * @param rejectionAction PaymentRejectionAction
     */
    @Override
    public Response updateRejectionAction(Long id, RejectionAction rejectionAction) {
        RejectionAction updatedAction = paymentApi.updateRejectionAction(id, rejectionAction);
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection action successfully updated\"},\"id\":"
                        + updatedAction.getId() + ", \"code\": \""
                        + updatedAction.getCode() + "\"}")
                .build();
    }

    /**
     * delete rejection action
     *
     * @param id PaymentRejectionAction id
     */
    @Override
    public Response removeRejectionAction(Long id) {
        paymentApi.removeRejectionAction(id);
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection action successfully deleted\"}")
                .build();
    }

    /**
     * Delete rejection code based on filters
     *
     * @param rejectionCodeDeleteInput RejectionCodeDeleteInput
     */
    @Override
    public Response removeRejectionCode(RejectionCodeDeleteInput rejectionCodeDeleteInput) {
        if (rejectionCodeDeleteInput == null || rejectionCodeDeleteInput.getFilters() == null
                || rejectionCodeDeleteInput.getFilters().getFilters().isEmpty()) {
            throw new MissingParameterException("No filter provided");
        }
        PagingAndFiltering pagingAndFiltering = new PagingAndFiltering();
        pagingAndFiltering.setFilters(rejectionCodeDeleteInput.getFilters().getFilters());
        final int deletedCodeCount = paymentApi.removeRejectionCode(pagingAndFiltering,
                rejectionCodeDeleteInput.getForce());
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Payment rejection codes successfully deleted\"}," +
                        " \"numberOfDeletedCodes\":" + deletedCodeCount + "}")
                .build();
    }

    /**
     * Create rejection code group
     *
     * @param rejectionGroup rejection code group
     */
    @Override
    public Response createRejectionGroup(RejectionGroup rejectionGroup) {
        RejectionGroup created = paymentApi.createRejectionGroup(rejectionGroup);
        return ok().entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection codes group successfully created\"},\"id\":"
                        + created.getId() + ", \"code\": \""
                        + created.getCode() + "\"}")
                .build();
    }

    /**
     * Update rejection code group
     *
     * @param id rejection code group id to update
     * @param rejectionGroup rejection code group
     */
    @Override
    public Response updateRejectionGroup(Long id, RejectionGroup rejectionGroup) {
        RejectionGroup updated = paymentApi.updateRejectionGroup(id, rejectionGroup);
        return ok().entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection codes group successfully updated\"},\"id\":"
                        + updated.getId() + ", \"code\": \""
                        + updated.getCode() + "\"}")
                .build();
    }

    /**
     * Remove payment rejection code group
     *
     * @param rejectionGroupId payment rejection code group id to remove
     */
    @Override
    public Response removeRejectionGroup(Long rejectionGroupId) {
        paymentApi.removeRejectionCodeGroup(rejectionGroupId);
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Payment rejection codes group successfully deleted\"}")
                .build();
    }

    /**
     * Remove payment rejection code group based on filter
     *
     * @param filters PagingAndFiltering
     */
    @Override
    public Response removeRejectionGroup(PagingAndFiltering filters) {
        return ok()
                .entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Payment rejection codes group successfully deleted\"}" +
                        ",\"numberOfDeletedGroups\":" + paymentApi.removeRejectionCodeGroup(filters))
                .build();
    }

    /**
     * Update payment rejection action sequence
     *
     * @param actionId action id to update
     * @param sequenceAction action type
     */
    @Override
    public Response updateActionSequence(Long actionId, SequenceAction sequenceAction) {
        RejectionAction updated = paymentApi.updateActionSequence(actionId, sequenceAction);
        return ok().entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection action sequence successfully updated\"},\"id\":"
                        + updated.getId() + ", \"code\": \""
                        + updated.getCode() + "\", \"newSequence\": "
                        + updated.getSequence() + "}")
                .build();
    }

    /**
     * Create rejection payment
     *
     * @param rejectionPayment rejection payment input
     */
    @Override
    public Response createRejectionPayment(RejectionPayment rejectionPayment) {
        RejectionPayment created = paymentApi.createRejectionPayment(rejectionPayment);
        return ok().entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Rejection payment successfully created\"},\"id\":"
                        + created.getId() + ", \"code\": \""
                        + created.getCode() + "\"}")
                .build();
    }

    @Override
    public Response retryRejectedPayment(@PathParam("id") Long paymentId) throws Exception {
        paymentApi.retryRejectedPayment(paymentId);
        return ok().entity("{\"actionStatus\":{\"status\":\"SUCCESS\"" +
                        ",\"message\":\"Payment successfully retried\"}}")
                .build();
    }
}
