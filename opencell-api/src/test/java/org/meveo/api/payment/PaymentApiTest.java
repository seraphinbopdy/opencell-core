package org.meveo.api.payment;

import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.admin.exception.NoAllOperationUnmatchedException;
import org.meveo.admin.exception.UnbalanceAmountException;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.api.dto.payment.PaymentDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.apiv2.models.ImmutableResource;
import org.meveo.apiv2.payments.ImmutableImportRejectionCodeInput;
import org.meveo.apiv2.payments.ImmutablePaymentGatewayInput;
import org.meveo.apiv2.payments.ImmutableRejectionAction;
import org.meveo.apiv2.payments.ImmutableRejectionCode;
import org.meveo.apiv2.payments.ImmutableRejectionGroup;
import org.meveo.apiv2.payments.ImportRejectionCodeInput;
import org.meveo.apiv2.payments.PaymentGatewayInput;
import org.meveo.apiv2.payments.RejectionAction;
import org.meveo.apiv2.payments.RejectionCode;
import org.meveo.apiv2.payments.RejectionCodesExportResult;
import org.meveo.apiv2.payments.RejectionGroup;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.payments.Journal;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.model.payments.PaymentGateway;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.model.payments.PaymentRejectionAction;
import org.meveo.model.payments.PaymentRejectionCode;
import org.meveo.model.payments.PaymentRejectionCodesGroup;
import org.meveo.service.billing.impl.JournalService;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.OCCTemplateService;
import org.meveo.service.payments.impl.PaymentGatewayService;
import org.meveo.service.payments.impl.PaymentHistoryService;
import org.meveo.service.payments.impl.PaymentRejectionActionService;
import org.meveo.service.payments.impl.PaymentRejectionCodeService;
import org.meveo.service.payments.impl.PaymentRejectionCodesGroupService;
import org.meveo.service.payments.impl.PaymentService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class PaymentApiTest {

    static class PaymentApiMock extends PaymentApi {
        @Override
        protected ICustomFieldEntity populateCustomFields(CustomFieldsDto customFieldsDto, ICustomFieldEntity entity, boolean isNewEntity) throws MeveoApiException {
        return null;
    }
    }

    @InjectMocks()
    private PaymentApi paymentApi = new PaymentApiMock();

    @Mock
    private CustomerAccountService customerAccountService;

    @Mock
    private OCCTemplateService oCCTemplateService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private JournalService journalService;

    @Mock
    private PaymentHistoryService paymentHistoryService;

    @Mock
    private AccountOperationService accountOperationService;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    @Mock
    private PaymentRejectionCodeService paymentRejectionCodeService;

    @Mock
    private PaymentRejectionActionService paymentRejectionActionService;

    @Mock
    private PaymentRejectionCodesGroupService paymentRejectionCodesGroupService;


    @Test
    public void createPayment_throwNoExceptionWhenCustomerIsNull() throws UnbalanceAmountException, NoAllOperationUnmatchedException {
        PaymentDto paymentDto = new PaymentDto();

        paymentDto.setAmount(BigDecimal.valueOf(100L));
        paymentDto.setOccTemplateCode("PAY_CHK");
        paymentDto.setReference("123456789");
        paymentDto.setPaymentMethod(PaymentMethodEnum.CHECK);

        doReturn(null).when(customerAccountService).findByCode(any());
        doReturn(new OCCTemplate()).when(oCCTemplateService).findByCode(any());
        doReturn(new Journal()).when(journalService).findByCode(any());
        doNothing().when(paymentService).create(any());
        doNothing().when(accountOperationService).handleAccountingPeriods(any());
        Long id = paymentApi.createPayment(paymentDto);
        Assert.assertNull(id);

    }

    @Test
    public void should_fail_to_create_payment_rejection_code_if_payment_gateway_found() {
        RejectionCode rejectionCode = ImmutableRejectionCode.builder().code("CODE_RC")
                .description("DESCRIPTION")
                .paymentGateway(ImmutableResource.builder().id(1L).build())
                .build();

        when(paymentGatewayService.findById(any())).thenReturn(null);

        assertThrows("Payment gateway not found", NotFoundException.class,
                () -> paymentApi.createPaymentRejectionCode(rejectionCode));
    }

    @Test
    public void should_update_payment_rejection_code() {
        RejectionCode rejectionCode = ImmutableRejectionCode.builder().code("CODE_RC")
                .description("DESCRIPTION")
                .paymentGateway(ImmutableResource.builder().id(1L).build())
                .build();
        PaymentGateway paymentGateway = new PaymentGateway();
        paymentGateway.setId(1L);
        PaymentRejectionCode entity = new PaymentRejectionCode();
        entity.setCode("CODE_RC");
        entity.setId(1L);

        when(paymentGatewayService.findById(any())).thenReturn(paymentGateway);
        when(paymentRejectionCodeService.findById(any())).thenReturn(entity);
        when(paymentRejectionCodeService.update(any())).thenReturn(entity);

        RejectionCode updatedEntity = paymentApi.updatePaymentRejectionCode(1L, rejectionCode);

        assertTrue(updatedEntity instanceof ImmutableRejectionCode);
    }

    @Test
    public void should_fail_to_update_payment_rejection_code_if_payment_gateway_not_found() {
        RejectionCode rejectionCode = ImmutableRejectionCode.builder().code("CODE_RC")
                .description("DESCRIPTION")
                .paymentGateway(ImmutableResource.builder().id(1L).build())
                .build();
        PaymentGateway paymentGateway = new PaymentGateway();
        paymentGateway.setId(1L);

        when(paymentGatewayService.findById(any())).thenReturn(paymentGateway);

        assertThrows("Payment rejection code not found", NotFoundException.class,
                () -> paymentApi.updatePaymentRejectionCode(1L, rejectionCode));
    }

    @Test
    public void should_fail_to_update_payment_rejection_code_if_if_payment_gateway_found() {
        RejectionCode rejectionCode = ImmutableRejectionCode.builder().code("CODE_RC")
                .description("DESCRIPTION")
                .paymentGateway(ImmutableResource.builder().id(1L).build())
                .build();
        PaymentGateway paymentGateway = new PaymentGateway();
        paymentGateway.setId(1L);

        when(paymentGatewayService.findById(any())).thenReturn(null);

        assertThrows("Payment gateway not found", NotFoundException.class,
                () -> paymentApi.updatePaymentRejectionCode(1L, rejectionCode));
    }

    @Test
    public void should_export_rejection_code() {
        PaymentRejectionCode entity = new PaymentRejectionCode();
        entity.setCode("CODE_RC");
        entity.setId(1L);

        PaymentGatewayInput paymentGatewayInput = ImmutablePaymentGatewayInput.builder()
                .paymentGateway(ImmutableResource.builder().id(1L).build())
                .build();
        PaymentGateway paymentGateway = new PaymentGateway();
        paymentGateway.setId(1L);
        Map<String, Object> exportResult = new HashMap<>();
        exportResult.put("FILE_PATH", "PATH/AAA");
        exportResult.put("EXPORT_SIZE", 1);
        when(paymentGatewayService.findById(any())).thenReturn(paymentGateway);
        when(paymentRejectionCodeService.export(any())).thenReturn(exportResult);

        RejectionCodesExportResult export = paymentApi.export(paymentGatewayInput);

        assertEquals(1, export.getExportSize().intValue());
    }

    @Test
    public void should_fail_to_export_rejection_code_if_gateway_not_found() {
        PaymentRejectionCode entity = new PaymentRejectionCode();
        entity.setCode("CODE_RC");
        entity.setId(1L);

        PaymentGatewayInput paymentGatewayInput = ImmutablePaymentGatewayInput.builder()
                .paymentGateway(ImmutableResource.builder().id(1L).build())
                .build();
        when(paymentGatewayService.findById(any())).thenReturn(null);

        assertThrows("Payment gateway not found", NotFoundException.class,
                () -> paymentApi.export(paymentGatewayInput));
    }

    @Test
    public void should_validate_if_encoded_file_is_provided() {
        PaymentRejectionCode entity = new PaymentRejectionCode();
        entity.setCode("CODE_RC");
        entity.setId(1L);

        ImportRejectionCodeInput importRejectionCodeInput = ImmutableImportRejectionCodeInput.builder()
                .base64csv("")
                .build();

        assertThrows("Encoded file should not be null or empty", BusinessApiException.class,
                () -> paymentApi.importRejectionCodes(importRejectionCodeInput));
    }

    @Test
    public void should_create_payment_rejection_action() {
        RejectionAction rejectionAction = ImmutableRejectionAction.builder()
                .code("ACTION_01")
                .description("ACTION DESCRIPTION")
                .build();

        when(paymentRejectionActionService.findByCode("ACTION_01")).thenReturn(null);

        RejectionAction result = paymentApi.createRejectionAction(rejectionAction);

        assertTrue(result instanceof ImmutableRejectionAction);

        verify(paymentRejectionActionService, times(1)).create(any());
    }

    @Test
    public void should_fail_when_create_payment_rejection_action_with_an_existing_code() {
        RejectionAction rejectionAction = ImmutableRejectionAction.builder()
                .code("ACTION_01")
                .description("ACTION DESCRIPTION")
                .build();

        when(paymentRejectionActionService.findByCode("ACTION_01")).thenReturn(new PaymentRejectionAction());

        assertThrows("Payment rejection action with code ACTION_01 already exists",
                EntityAlreadyExistsException.class,
                () -> paymentApi.createRejectionAction(rejectionAction));
    }

    @Test
    public void should_update_payment_rejection_action() {
        RejectionAction rejectionAction = ImmutableRejectionAction.builder()
                .code("ACTION_01")
                .description("ACTION DESCRIPTION")
                .build();

        when(paymentRejectionActionService.findById(1L)).thenReturn(new PaymentRejectionAction());

        when(paymentRejectionActionService.findByCode("ACTION_01")).thenReturn(null);
        when(paymentRejectionActionService.update(any())).thenReturn(new PaymentRejectionAction());

        paymentApi.updateRejectionAction(1L, rejectionAction);

        verify(paymentRejectionActionService, times(1)).update(any());
    }

    @Test
    public void should_fail_when_update_payment_rejection_action_with_an_existing_code() {
        RejectionAction rejectionAction = ImmutableRejectionAction.builder()
                .code("ACTION_01")
                .description("ACTION DESCRIPTION")
                .build();

        when(paymentRejectionActionService.findById(1L)).thenReturn(new PaymentRejectionAction());
        when(paymentRejectionActionService.findByCode("ACTION_01")).thenReturn(new PaymentRejectionAction());

        assertThrows("Payment rejection action with code " + rejectionAction.getCode() + " already exists",
                EntityAlreadyExistsException.class,
                () -> paymentApi.updateRejectionAction(1L, rejectionAction));
    }

    @Test
    public void should_fail_to_update_when_no_payment_rejection_action_found() {
        RejectionAction rejectionAction = ImmutableRejectionAction.builder()
                .code("ACTION_01")
                .description("ACTION DESCRIPTION")
                .build();

        when(paymentRejectionActionService.findById(1L)).thenReturn(null);

        assertThrows("Payment rejection action not found", NotFoundException.class,
                () -> paymentApi.updateRejectionAction(1L, rejectionAction));
    }

    @Test
    public void should_remove_payment_rejection_action_found() {
        PaymentRejectionAction action = new PaymentRejectionAction();
        action.setRejectionActionReports(emptyList());
        when(paymentRejectionActionService.findById(1L)).thenReturn(action);

        paymentApi.removeRejectionAction(1L);

        verify(paymentRejectionActionService, times(1)).remove(any(PaymentRejectionAction.class));
    }

    @Test
    public void should_fail_to_remove_when_no_payment_rejection_action_found() {
        when(paymentRejectionActionService.findById(1L)).thenReturn(null);

        assertThrows("Payment rejection action not found", NotFoundException.class,
                () -> paymentApi.removeRejectionAction(1L));
    }

    @Test
    public void should_create_payment_rejection_codes_group() {
        List<RejectionCode> rejectionCodes = of(ImmutableRejectionCode.builder().id(1L).build());
        List<RejectionAction> actions = of(ImmutableRejectionAction.builder().id(2L).build());
        RejectionGroup rejectionGroup = ImmutableRejectionGroup.builder()
                .code("RCG_01")
                .description("DESCRIPTION")
                .rejectionCodes(rejectionCodes)
                .rejectionActions(actions)
                .build();

        when(paymentRejectionCodesGroupService.findByCode("RCG_01")).thenReturn(null);
        when(paymentRejectionCodeService.findByIdOrCode(any())).thenReturn(new PaymentRejectionCode());
        when(paymentRejectionActionService.findByIdOrCode(any())).thenReturn(new PaymentRejectionAction());

        paymentApi.createRejectionGroup(rejectionGroup);

        verify(paymentRejectionCodesGroupService, times(1)).create(any());
    }

    @Test
    public void should_fail_to_create_payment_rejection_codes_group_if_rejection_code_not_found() {
        List<RejectionCode> rejectionCodes = of(ImmutableRejectionCode.builder().id(1L).build());
        List<RejectionAction> actions = of(ImmutableRejectionAction.builder().id(2L).build());
        RejectionGroup rejectionGroup = ImmutableRejectionGroup.builder()
                .code("RCG_01")
                .description("DESCRIPTION")
                .rejectionCodes(rejectionCodes)
                .rejectionActions(actions)
                .build();

        when(paymentRejectionCodesGroupService.findByCode("RCG_01")).thenReturn(null);
        when(paymentRejectionCodeService.findByIdOrCode(any())).thenReturn(null);

        assertThrows("Payment rejection code 1 does not exists", NotFoundException.class,
                () -> paymentApi.createRejectionGroup(rejectionGroup));
    }

    @Test
    public void should_fail_to_create_payment_rejection_codes_group_if__code_already_exists() {
        RejectionGroup rejectionGroup = ImmutableRejectionGroup.builder()
                .code("RCG_01")
                .description("DESCRIPTION")
                .build();

        when(paymentRejectionCodesGroupService.findByCode("RCG_01")).thenReturn(new PaymentRejectionCodesGroup());

        assertThrows("Payment rejection codes group with code RCG_01 already exists",
                EntityAlreadyExistsException.class,
                () -> paymentApi.createRejectionGroup(rejectionGroup));
    }

    @Test
    public void should_remove_payment_rejection_codes_group() {
        PaymentRejectionCodesGroup toRemove = new PaymentRejectionCodesGroup();
        toRemove.setId(1L);
        toRemove.setPaymentRejectionCodes(of(new PaymentRejectionCode()));
        toRemove.setPaymentRejectionActions(of(new PaymentRejectionAction()));

        when(paymentRejectionCodesGroupService.findById(1L)).thenReturn(toRemove);

        paymentApi.removeRejectionCodeGroup(1L);

        verify(paymentRejectionCodesGroupService,
                times(1)).remove(any(PaymentRejectionCodesGroup.class));
    }

    @Test
    public void should_fail_to_remove_when_no_payment_rejection_codes_group_found() {
        assertThrows("Payment rejection codes group with id 1 does not exists", NotFoundException.class,
                () -> paymentApi.removeRejectionCodeGroup(1L));
    }
}
