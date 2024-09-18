package org.meveo.api.dto.invoice;

import java.util.List;

import org.immutables.value.Value;
import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutablePaymentTerm.class)
public interface PaymentTerm extends Resource {
	
	String getDescription();
	
	List<LanguageDescriptionDto> getLanguageDescriptions();
	
}
