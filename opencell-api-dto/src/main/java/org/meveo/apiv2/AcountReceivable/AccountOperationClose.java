package org.meveo.apiv2.AcountReceivable;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableAccountOperationClose.class)
public interface AccountOperationClose {
	
	@Schema(description = "List of AccountOperation to be closed")
	@NotEmpty
	List<AccountOperationInput> getAccountOperations();
    
}