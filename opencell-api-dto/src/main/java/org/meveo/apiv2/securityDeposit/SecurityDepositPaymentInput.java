package org.meveo.apiv2.securityDeposit;

import java.math.BigDecimal;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSecurityDepositPaymentInput.class)
public interface SecurityDepositPaymentInput extends Resource {

    @NotNull
    BigDecimal getAmount();

    @NotNull
    Resource getAccountOperation();

}