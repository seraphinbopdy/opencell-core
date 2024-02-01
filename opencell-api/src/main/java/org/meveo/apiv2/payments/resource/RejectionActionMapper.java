package org.meveo.apiv2.payments.resource;

import static java.util.Optional.ofNullable;
import static org.meveo.apiv2.models.ImmutableResource.builder;

import org.meveo.apiv2.generic.ResourceMapper;
import org.meveo.apiv2.payments.ImmutableRejectionAction;
import org.meveo.apiv2.payments.RejectionAction;
import org.meveo.model.payments.PaymentRejectionAction;
import org.meveo.model.payments.PaymentRejectionCodesGroup;
import org.meveo.model.scripts.ScriptInstance;

public class RejectionActionMapper extends ResourceMapper<RejectionAction, PaymentRejectionAction> {

    @Override
    public RejectionAction toResource(PaymentRejectionAction entity) {
        ImmutableRejectionAction.Builder builder = ImmutableRejectionAction.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .sequence(entity.getSequence())
                .scriptParameters(entity.getScriptParameters());
        if(entity.getScript() != null) {
            builder.scriptInstance(builder().code(entity.getScript().getCode())
                    .build());
        }
        if(entity.getPaymentRejectionCodesGroup() != null) {
            builder.rejectionCodeGroup(builder()
                    .id(entity.getPaymentRejectionCodesGroup().getId())
                    .code(entity.getPaymentRejectionCodesGroup().getCode())
                    .build());
        }
        return builder.build();
    }

    @Override
    public PaymentRejectionAction toEntity(RejectionAction resource) {
        PaymentRejectionAction rejectionAction = new PaymentRejectionAction();
        rejectionAction.setCode(resource.getCode());
        rejectionAction.setDescription(resource.getDescription());
        ofNullable(resource.getSequence()).ifPresent(rejectionAction::setSequence);
        if(resource.getScriptInstance() != null) {
            ScriptInstance scriptInstance = new ScriptInstance();
            scriptInstance.setId(resource.getScriptInstance().getId());
            scriptInstance.setCode(resource.getScriptInstance().getCode());
            rejectionAction.setScript(scriptInstance);
        }
        if(resource.getRejectionCodeGroup() != null) {
            PaymentRejectionCodesGroup paymentRejectionCodesGroup = new PaymentRejectionCodesGroup();
            paymentRejectionCodesGroup.setId(resource.getRejectionCodeGroup().getId());
            paymentRejectionCodesGroup.setCode(resource.getRejectionCodeGroup().getCode());
            rejectionAction.setPaymentRejectionCodesGroup(paymentRejectionCodesGroup);
        }
        rejectionAction.setScriptParameters(resource.getScriptParameters());
        return rejectionAction;
    }
}
