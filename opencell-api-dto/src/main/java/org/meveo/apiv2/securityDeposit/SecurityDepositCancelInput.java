package org.meveo.apiv2.securityDeposit;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSecurityDepositCancelInput.class)
public interface SecurityDepositCancelInput extends Resource {
    
    @Nullable
    String getCancelReason();
    
}