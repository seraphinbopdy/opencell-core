package org.meveo.apiv2.AcountReceivable;

import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAmountsTransferDto.class)
public interface AmountsTransferDto {
	@Schema(description = "List of amounts to transfer")
	@Nullable
	List<AmountToTransferDto> getAmountsToTransfer();
}
