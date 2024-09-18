package org.meveo.apiv2.generic;

import java.util.Collections;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableGenericFieldDetails.class)
public interface GenericFieldDetails {

	@Schema(description = "name generic field")
    String getName();
    @Nullable
    String getHeader();
    @Nullable
    String getTransformation();
    @Nullable
    @Value.Default default Map<String, String> getMappings(){ return Collections.emptyMap();}
    @Nullable
    String getFormulaInputs();
    @Nullable
    String getFormula();

}
