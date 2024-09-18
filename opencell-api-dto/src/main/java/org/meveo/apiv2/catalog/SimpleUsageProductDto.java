package org.meveo.apiv2.catalog;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {
 *      "chargeCode": "{{chargeCode}}",
 *      "productCode": "{{productCode}}"
 *      "description": "Recurring charge",
 *      "price": 100,
 *      "filterParam1": "p1",
 *      "filterParam2": "p2",
 *      "filterParam3": "p3",
 *      "filterParam4": "p4",
 *      "validity": {
 * 		    "from": "2024-01-01",
 * 		    "to": "2024-01-31"
 * 	    }
 * }
 *
 */
@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSimpleUsageProductDto.class)
public interface SimpleUsageProductDto extends SimpleChargeProductDto {

    @Schema(description = "Usage charge filter parameter 1")
    @Nullable
    String getFilterParam1();
    
    @Schema(description = "Usage charge filter parameter 2")
    @Nullable
    String getFilterParam2();
    
    @Schema(description = "Usage charge filter parameter 3")
    @Nullable
    String getFilterParam3();
    
    @Schema(description = "Usage charge filter parameter 4")
    @Nullable
    String getFilterParam4();
    
}
