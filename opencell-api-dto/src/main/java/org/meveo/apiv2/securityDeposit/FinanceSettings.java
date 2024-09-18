package org.meveo.apiv2.securityDeposit;

import static java.lang.Boolean.FALSE;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.apiv2.settings.OpenOrderSettingInput;
import org.meveo.model.securityDeposit.ArticleSelectionModeEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableFinanceSettings.class)
public interface FinanceSettings extends Resource {

    @Value.Default
    @Schema(description = "use security deposit")
    default Boolean getUseSecurityDeposit() {
        return FALSE;
    }

    @Nullable
    BigDecimal getMaxAmountPerSecurityDeposit();

    @Nullable
    BigDecimal getMaxAmountPerCustomer();

    @Value.Default
    @Schema(description = "Auto refund")
    default Boolean getAutoRefund() {
        return FALSE;
    }

    @Value.Default
    @Schema(description = "Use auxiliary accounting")
    default Boolean getUseAuxiliaryAccounting() {
        return FALSE;
    }

    @Schema(description = "Auxiliary account code El")
    @Nullable
    String getAuxiliaryAccountCodeEl();

    @Schema(description = "Auxiliary account label El")
    @Nullable
    String getAuxiliaryAccountLabelEl();

    @Nullable
    OpenOrderSettingInput getOpenOrderSetting();

    @Value.Default
    @Schema(description = "Activate dunning")
    default Boolean getActivateDunning() {
        return FALSE;
    }

    @Value.Default
    @Schema(description = "Enable Billing Redirection Rules")
    default Boolean getEnableBillingRedirectionRules() {
        return FALSE;
    }

    @Value.Default
    @Schema(description = "Enable Billing Redirection Rules")
    default Boolean getDiscountAdvancedMode() {
        return FALSE;
    }

    @Value.Default
    @Schema(description = "Enable Price List")
    default Boolean getEnablePriceList() {
        return FALSE;
    }

	@Nullable
	@Schema(description = "determinate if the article will be compute before or after pricing")
	ArticleSelectionModeEnum getArticleSelectionMode();

	@Schema(description = "Entities with Huge Volume")
    Map<String, HugeEntity> getEntitiesWithHugeVolume();

    @Value.Default
    @Schema(description = "Display warning before process billing Run")
    default boolean getBillingRunProcessWarning() {
        return false;
    }

    @Nullable
    @Schema(description = "Number of partitions to keep")
    Integer getNbPartitionsToKeep();
    
    @Nullable
    @Schema(description = "Number of elements to process in a synchronous mode")
    Integer getSynchronousMassActionLimit();

    @Nullable
    @Schema(description = "Wallet Operation partition Period in Months")
    Integer getWoPartitionPeriod();

    @Nullable
    @Schema(description = "Rated Transaction partition Period in Months")
    Integer getRtPartitionPeriod();

    @Nullable
    @Schema(description = "EDR partition Period in Months")
    Integer getEdrPartitionPeriod();

    @Value.Default
    @Schema(description = "Handle Framework Agreement")
    default boolean getHandleFrameworkAgreement() {
        return FALSE;
    }

    @Value.Default
    @Schema(description = "Handle Invoice Plans")
    default boolean getHandleInvoicingPlans() {
        return FALSE;
    }

    @Value.Default
    @Schema(description = "Handle Accounting Periods")
    default boolean getHandleAccountingPeriods() {
        return FALSE;
    }

    @Value.Default
    @Schema(description = "Enable empty subscription activation")
    default Boolean getEnableEmptySubscriptionActivation() {
        return false;
    }
    
    @Value.Default
    @Schema(description = "Enable Quotes Feature")
    default boolean getEnableQuotesFeature() {
        return FALSE;
    }
    
    @Value.Default
    @Schema(description = "Display Counters")
    default boolean getDisplayCounters() {
        return FALSE;
    }
}