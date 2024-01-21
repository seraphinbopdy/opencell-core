package org.meveo.apiv2.catalog;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.model.DatePeriod;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.OneShotChargeTemplateTypeEnum;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

/**
 * {
 *       "chargeCode": "{{chargeCode}}",
 *      "productCode": "{{productCode}}"
 *      "description": "Recurring charge",
 *      "price": 100,
 *      "calendar": "MONTHLY",
 *      "subscriptionProrata": true,
 *      "terminationProrata": true,
 *      "applyInAdvance": true,
 *      "anticipateEndOfSubscription": true
 *      "validity": {
 * 		"from": "2024-01-01",
 * 		"to": "2024-01-31"
 * 	    }
 * }
 *
 */
@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableSimpleRecurrentProductDto.class)
public interface SimpleRecurrentProductDto extends SimpleChargeProductDto {

    @Schema(description = "Recurrence calendar")
    String getCalendar();
    
    @Schema(description = "Calculate subscription prorata")
    Boolean getSubscriptionProrata();
    
    @Schema(description = "Calculate termination prorata")
    Boolean getTerminationProrata();
    
    @Schema(description = "Apply in advance")
    Boolean getApplyInAdvance();
    
    @Schema(description = "Anticipate end of subscription")
    Boolean getAnticipateEndOfSubscription();
    
}
