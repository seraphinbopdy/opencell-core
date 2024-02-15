package org.meveo.apiv2.AcountReceivable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import javax.annotation.Nullable;
import java.util.List;

@Immutable
@Style(jdkOnly = true)
@JsonDeserialize(builder = ImmutableAmountsTransferDto.class)
public interface AmountsTransferDto {
	@Schema(description = "List of amounts to transfer")
	@Nullable
	List<AmountToTransferDto> getAmountsToTransfer();
}
