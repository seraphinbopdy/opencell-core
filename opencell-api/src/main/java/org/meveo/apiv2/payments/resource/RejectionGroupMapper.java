package org.meveo.apiv2.payments.resource;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import org.meveo.apiv2.generic.ResourceMapper;
import org.meveo.apiv2.models.ImmutableResource;
import org.meveo.apiv2.models.Resource;
import org.meveo.apiv2.payments.ImmutableRejectionGroup;
import org.meveo.apiv2.payments.RejectionGroup;
import org.meveo.model.BusinessEntity;
import org.meveo.model.payments.PaymentGateway;
import org.meveo.model.payments.PaymentRejectionAction;
import org.meveo.model.payments.PaymentRejectionCode;
import org.meveo.model.payments.PaymentRejectionCodesGroup;

import java.util.List;

public class RejectionGroupMapper extends ResourceMapper<RejectionGroup, PaymentRejectionCodesGroup>  {

    @Override
    public RejectionGroup toResource(PaymentRejectionCodesGroup entity) {
        ImmutableRejectionGroup.Builder builder = ImmutableRejectionGroup.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .paymentGateway(ImmutableResource.builder().id(entity.getPaymentGateway().getId()).build());
        if (entity.getPaymentRejectionActions() != null && !entity.getPaymentRejectionActions().isEmpty()) {
            builder.rejectionCodes(toResource(entity.getPaymentRejectionActions()));
        }
        if (entity.getPaymentRejectionCodes() != null && !entity.getPaymentRejectionCodes().isEmpty()) {
            builder.rejectionCodes(toResource(entity.getPaymentRejectionCodes()));
        }
        return builder.build();
    }

    private List<Resource> toResource(List<? extends BusinessEntity> entities) {
        return entities.stream()
                .map(entity -> ImmutableResource.builder().id(entity.getId()).code(entity.getCode()).build())
                .collect(toList());
    }

    @Override
    public PaymentRejectionCodesGroup toEntity(RejectionGroup resource) {
        PaymentRejectionCodesGroup entity = toEntity(resource, new PaymentRejectionCodesGroup());
        if (resource.getRejectionCodes() != null && !resource.getRejectionCodes().isEmpty()) {
            entity.setPaymentRejectionCodes(resource.getRejectionCodes()
                    .stream()
                    .map(rejectionCode -> createRejectionCode(rejectionCode, entity))
                    .collect(toList()));
        }
        if (resource.getRejectionActions() != null && !resource.getRejectionActions().isEmpty()) {
            entity.setPaymentRejectionActions(resource.getRejectionActions()
                    .stream()
                    .map(rejectionAction -> createRejectionAction(rejectionAction, entity))
                    .collect(toList()));
        }
        return entity;
    }

    private PaymentRejectionCode createRejectionCode(Resource rejectionCode, PaymentRejectionCodesGroup entity) {
        PaymentRejectionCode paymentRejectionCode = new PaymentRejectionCode();
        paymentRejectionCode.setId(rejectionCode.getId());
        paymentRejectionCode.setCode(rejectionCode.getCode());
        paymentRejectionCode.setPaymentRejectionCodesGroup(entity);
        return paymentRejectionCode;
    }

    private PaymentRejectionAction createRejectionAction(Resource rejectionAction, PaymentRejectionCodesGroup entity) {
        PaymentRejectionAction paymentRejectionAction = new PaymentRejectionAction();
        paymentRejectionAction.setId(rejectionAction.getId());
        paymentRejectionAction.setCode(rejectionAction.getCode());
        paymentRejectionAction.setPaymentRejectionCodesGroup(entity);
        return paymentRejectionAction;
    }

    public PaymentRejectionCodesGroup toEntity(RejectionGroup resource, PaymentRejectionCodesGroup entity) {
        ofNullable(resource.getCode()).ifPresent(entity::setCode);
        ofNullable(resource.getDescription()).ifPresent(entity::setDescription);
        if (resource.getPaymentGateway() != null) {
            PaymentGateway paymentGateway = new PaymentGateway();
            paymentGateway.setId(resource.getPaymentGateway().getId());
            paymentGateway.setCode(resource.getPaymentGateway().getCode());
            entity.setPaymentGateway(paymentGateway);
        }
        return entity;
    }
}
