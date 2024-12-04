package org.meveo.model.securityDeposit;

import java.math.BigDecimal;
import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.SqlTypes;
import org.meveo.model.BusinessEntity;
import org.meveo.model.settings.OpenOrderSetting;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;

@Entity
@Cacheable
@Table(name = "finance_settings")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "finance_settings_seq"), @Parameter(name = "increment_size", value = "1") })
public class FinanceSettings extends BusinessEntity {

    /**
     * 
     */
    private static final long serialVersionUID = -7662503000202423539L;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "use_security_deposit")
    private boolean useSecurityDeposit = false;

    @Column(name = "max_amount_security_deposit", precision = NB_PRECISION, scale = NB_DECIMALS)
    @Digits(integer = NB_PRECISION, fraction = NB_DECIMALS)
    private BigDecimal maxAmountPerSecurityDeposit;

    @Column(name = "max_amount_consumer", precision = NB_PRECISION, scale = NB_DECIMALS)
    @Digits(integer = NB_PRECISION, fraction = NB_DECIMALS)
    private BigDecimal maxAmountPerCustomer;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "auto_refund")
    private boolean autoRefund = false;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "open_order_settings_id")
    private OpenOrderSetting openOrderSetting;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "activate_dunning")
    private boolean activateDunning = false;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "enable_billing_redirection_rules")
    private boolean enableBillingRedirectionRules = false;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "discount_advanced_mode")
    private boolean discountAdvancedMode = false;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "enable_price_list")
    private boolean enablePriceList = false;

    @Column(name = "article_selection_mode")
    @Enumerated(EnumType.STRING)
    private ArticleSelectionModeEnum articleSelectionMode = ArticleSelectionModeEnum.AFTER_PRICING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entities_with_huge_volume", columnDefinition = "jsonb")
    private Map<String, HugeEntity> entitiesWithHugeVolume;

    @Column(name = "nb_partitions_keep")
    private Integer nbPartitionsToKeep;

    @Column(name = "wo_partition_range_months")
    private Integer woPartitionPeriod;

    @Column(name = "rt_partition_range_months")
    private Integer rtPartitionPeriod;

    @Column(name = "edr_partition_range_months")
    private Integer edrPartitionPeriod;

    @Embedded
    private AuxiliaryAccounting auxiliaryAccounting;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "billing_run_process_warning")
    private boolean billingRunProcessWarning;

    @Column(name = "synchronous_mass_action_limit")
    private Integer synchronousMassActionLimit = 10000;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "handle_framework_agreement")
    private boolean handleFrameworkAgreement = false;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "handle_invoice_plan")
    private boolean handleInvoicingPlans = false;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "handle_accounting_periods")
    private boolean handleAccountingPeriods = false;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "enable_empty_subscription_activation")
    private boolean enableEmptySubscriptionActivation;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "enable_quotes_feature")
    private boolean enableQuotesFeature = false;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "display_counters")
    private boolean displayCounters = false;

    public FinanceSettings() {
        super();
    }

    public FinanceSettings(boolean useSecurityDeposit, BigDecimal maxAmountPerSecurityDeposit, BigDecimal maxAmountPerCustomer, boolean autoRefund, boolean activateDunning) {
        super();
        this.useSecurityDeposit = useSecurityDeposit;
        this.maxAmountPerSecurityDeposit = maxAmountPerSecurityDeposit;
        this.maxAmountPerCustomer = maxAmountPerCustomer;
        this.autoRefund = autoRefund;
        this.activateDunning = activateDunning;
    }

    public boolean isUseSecurityDeposit() {
        return useSecurityDeposit;
    }

    public void setUseSecurityDeposit(boolean useSecurityDeposit) {
        this.useSecurityDeposit = useSecurityDeposit;
    }

    public BigDecimal getMaxAmountPerSecurityDeposit() {
        return maxAmountPerSecurityDeposit;
    }

    public void setMaxAmountPerSecurityDeposit(BigDecimal maxAmountPerSecurityDeposit) {
        this.maxAmountPerSecurityDeposit = maxAmountPerSecurityDeposit;
    }

    public BigDecimal getMaxAmountPerCustomer() {
        return maxAmountPerCustomer;
    }

    public void setMaxAmountPerCustomer(BigDecimal maxAmountPerCustomer) {
        this.maxAmountPerCustomer = maxAmountPerCustomer;
    }

    public boolean isAutoRefund() {
        return autoRefund;
    }

    public void setAutoRefund(boolean autoRefund) {
        this.autoRefund = autoRefund;
    }

    public OpenOrderSetting getOpenOrderSetting() {
        return openOrderSetting;
    }

    public void setOpenOrderSetting(OpenOrderSetting openOrderSetting) {
        this.openOrderSetting = openOrderSetting;
    }

    public AuxiliaryAccounting getAuxiliaryAccounting() {
        return auxiliaryAccounting;
    }

    public void setAuxiliaryAccounting(AuxiliaryAccounting auxiliaryAccounting) {
        this.auxiliaryAccounting = auxiliaryAccounting;
    }

    public boolean isActivateDunning() {
        return activateDunning;
    }

    public void setActivateDunning(boolean activateDunning) {
        this.activateDunning = activateDunning;
    }

    public boolean isEnableBillingRedirectionRules() {
        return enableBillingRedirectionRules;
    }

    public void setEnableBillingRedirectionRules(boolean enableBillingRedirectionRules) {
        this.enableBillingRedirectionRules = enableBillingRedirectionRules;
    }

    public boolean isDiscountAdvancedMode() {
        return discountAdvancedMode;
    }

    public void setDiscountAdvancedMode(boolean discountAdvancedMode) {
        this.discountAdvancedMode = discountAdvancedMode;
    }

    public boolean isEnablePriceList() {
        return enablePriceList;
    }

    public void setEnablePriceList(boolean enablePriceList) {
        this.enablePriceList = enablePriceList;
    }

    public ArticleSelectionModeEnum getArticleSelectionMode() {
        return articleSelectionMode;
    }

    public void setArticleSelectionMode(ArticleSelectionModeEnum articleSelectionMode) {
        this.articleSelectionMode = articleSelectionMode;
    }

    public Map<String, HugeEntity> getEntitiesWithHugeVolume() {
        return entitiesWithHugeVolume;
    }

    public void setEntitiesWithHugeVolume(Map<String, HugeEntity> entitiesWithHugeVolume) {
        this.entitiesWithHugeVolume = entitiesWithHugeVolume;
    }

    public boolean isBillingRunProcessWarning() {
        return billingRunProcessWarning;
    }

    public void setBillingRunProcessWarning(boolean billingRunProcessWarning) {
        this.billingRunProcessWarning = billingRunProcessWarning;
    }

    public Integer getNbPartitionsToKeep() {
        return nbPartitionsToKeep;
    }

    public void setNbPartitionsToKeep(Integer nbPartitionsToKeep) {
        this.nbPartitionsToKeep = nbPartitionsToKeep;
    }

    public Integer getSynchronousMassActionLimit() {
        return synchronousMassActionLimit;
    }

    public void setSynchronousMassActionLimit(Integer synchronousMassActionLimit) {
        this.synchronousMassActionLimit = synchronousMassActionLimit;
    }

    public Integer getWoPartitionPeriod() {
        return woPartitionPeriod;
    }

    public void setWoPartitionPeriod(Integer woPartitionPeriod) {
        this.woPartitionPeriod = woPartitionPeriod;
    }

    public Integer getRtPartitionPeriod() {
        return rtPartitionPeriod;
    }

    public void setRtPartitionPeriod(Integer rtPartitionPeriod) {
        this.rtPartitionPeriod = rtPartitionPeriod;
    }

    public Integer getEdrPartitionPeriod() {
        return edrPartitionPeriod;
    }

    public void setEdrPartitionPeriod(Integer edrPartitionPeriod) {
        this.edrPartitionPeriod = edrPartitionPeriod;
    }

    public boolean isHandleFrameworkAgreement() {
        return handleFrameworkAgreement;
    }

    public void setHandleFrameworkAgreement(boolean handleFrameworkAgreement) {
        this.handleFrameworkAgreement = handleFrameworkAgreement;
    }

    public boolean isHandleInvoicingPlans() {
        return handleInvoicingPlans;
    }

    public void setHandleInvoicingPlans(boolean handleInvoicingPlans) {
        this.handleInvoicingPlans = handleInvoicingPlans;
    }

    public boolean isHandleAccountingPeriods() {
        return handleAccountingPeriods;
    }

    public void setHandleAccountingPeriods(boolean handleAccountingPeriods) {
        this.handleAccountingPeriods = handleAccountingPeriods;
    }

    public boolean isEnableEmptySubscriptionActivation() {
        return enableEmptySubscriptionActivation;
    }

    public void setEnableEmptySubscriptionActivation(boolean enableEmptySubscriptionActivation) {
        this.enableEmptySubscriptionActivation = enableEmptySubscriptionActivation;
    }

    public boolean isEnableQuotesFeature() {
        return enableQuotesFeature;
    }

    public void setEnableQuotesFeature(boolean enableQuotesFeature) {
        this.enableQuotesFeature = enableQuotesFeature;
    }

    public boolean isDisplayCounters() {
        return displayCounters;
    }

    public void setDisplayCounters(boolean displayCounters) {
        this.displayCounters = displayCounters;
    }

}
