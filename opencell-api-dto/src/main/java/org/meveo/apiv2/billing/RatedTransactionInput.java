package org.meveo.apiv2.billing;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableRatedTransactionInput.class)
public interface RatedTransactionInput extends Resource {

	@Nullable
	String getBillingAccountCode();

	@Nullable
	String getUserAccountCode();

	@Nullable
	String getSubscriptionCode();

	@Nullable
	String getServiceInstanceCode();

	@Nullable
	String getChargeInstanceCode();

	@Nullable
	Date getUsageDate();

	@Nullable
	@Deprecated
	BigDecimal getUnitAmountWithoutTax();

	BigDecimal getQuantity();

    @Nullable
	String getParameter1();

    @Nullable
	String getParameter2();

    @Nullable
	String getParameter3();

    @Nullable
	String getParameterExtra();

	@Schema(description = "Rated transaction description")
	@Nullable
	String getDescription();
	
	@Nullable
	String getBusinessKey();

	@Nullable
	BigDecimal getUnitPrice();
}