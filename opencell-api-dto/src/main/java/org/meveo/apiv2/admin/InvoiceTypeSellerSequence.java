package org.meveo.apiv2.admin;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableInvoiceTypeSellerSequence.class)
public interface InvoiceTypeSellerSequence {
	
	@Nullable
	public Long getId();
	@Nullable
	public Long getInvoiceTypeId();
	@Nullable
	public Long getInvoiceSequenceId();
	@Nullable
	public String getPrefixEL();
	
}
