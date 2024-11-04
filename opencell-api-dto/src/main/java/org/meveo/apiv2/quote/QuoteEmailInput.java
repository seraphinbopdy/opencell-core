package org.meveo.apiv2.quote;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.api.dto.cpq.QuoteAttributeDTO;
import org.meveo.api.dto.cpq.QuoteProductDTO;
import org.meveo.apiv2.models.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableQuoteEmailInput.class)
public interface QuoteEmailInput {

    @Schema(description = "Discount plan attached to quote offer")
    @Nullable
	String getQuoteCode();

    @Schema(description = "quote version")
    @Nonnull
	Integer getQuoteVersion();
	
	@Schema(description = "list from users to send email")
	@Nullable
	List<String> getFrom();
	
	@Schema(description = "list to users to send email")
	@Nullable
	List<String> getTo();
	
	@Schema(description = "list cc users to send email")
	@Nullable
	List<String> getCc();
}
