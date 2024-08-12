package org.meveo.api.dto.invoice;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.apiv2.models.Resource;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutablePaymentTerm.class)
public interface PaymentTerm extends Resource {
	
	String getDescription();
	
	List<LanguageDescriptionDto> getLanguageDescriptions();
	
}
