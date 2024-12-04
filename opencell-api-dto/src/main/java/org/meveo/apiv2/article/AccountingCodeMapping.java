package org.meveo.apiv2.article;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAccountingCodeMapping.class)
public interface AccountingCodeMapping extends Resource {

    @Schema(description = "Accounting article code")
    @Nullable
    String getAccountingArticleCode();

    @Schema(description = "Billing account trading country code")
    @Nullable
    String getBillingCountryCode();

    @Schema(description = "Billing account trading currency")
    @Nullable
    String getBillingCurrencyCode();

    @Schema(description = "Seller trading country code")
    @Nullable
    String getSellerCountryCode();

    @Schema(description = "Seller code")
    @Nullable
    String getSellerCode();

    @Schema(description = "Criteria El Value")
    @Nullable
    String getCriteriaElValue();

    @Schema(description = "Accounting code")
    @Nullable
    String getAccountingCode();
}