package org.meveo.apiv2.billing;

import java.math.BigDecimal;
import java.util.Date;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableInvoiceMatchedOperation.class)
public interface InvoiceMatchedOperation {
    Long getPaymentId();
    Long getMatchingAmountId();
    String getPaymentCode();
    String getPaymentDescription();
    String getMatchingType();
    Date getMatchingDate();
    Date getPaymentDate();
    String getPaymentStatus();
    String getPaymentMethod();
    String getPaymentRef();
    BigDecimal getAmount();
    BigDecimal getPercentageCovered();
    String getRejectedDescription();
    String getRejectedCode();
    @Schema(description = "Transactional amount with tax")
    BigDecimal getTransactionalAmount();
}
