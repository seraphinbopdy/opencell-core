package org.meveo.service.billing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.annotations.QueryHints;
import org.meveo.admin.exception.InvalidELException;
import org.meveo.admin.exception.NoPricePlanException;
import org.meveo.admin.exception.RatingException;
import org.meveo.commons.utils.ELUtils;
import org.meveo.commons.utils.PersistenceUtils;
import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.billing.ChargeInstance;
import org.meveo.model.billing.RecurringChargeInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.PricePlanMatrixForRating;
import org.meveo.model.catalog.PricePlanMatrixLine;
import org.meveo.model.catalog.PricePlanMatrixValueForRating;
import org.meveo.model.catalog.PricePlanMatrixVersion;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.cpq.Attribute;
import org.meveo.model.cpq.AttributeCategoryEnum;
import org.meveo.model.cpq.AttributeValue;
import org.meveo.model.cpq.enums.PriceVersionDateSettingEnum;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.settings.impl.AdvancedSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

@Stateless
public class PricePlanSelectionService implements Serializable {

    private static final long serialVersionUID = 6228282058872986933L;

    Logger log = LoggerFactory.getLogger(PricePlanSelectionService.class);

    @Inject
    @MeveoJpa
    private EntityManagerWrapper emWrapper;

    @Inject
    private AttributeInstanceService attributeInstanceService;

    @Inject
    private AdvancedSettingsService advancedSettingsService;

    @Inject
    private ELUtils elUtils;

    public EntityManager getEntityManager() {
        return emWrapper.getEntityManager();
    }

    /**
     * Determine a first/highest priority price plan that match a wallet operation and buyer country and currency
     * 
     * @param bareWo Wallet operation to determine price for
     * @param buyerCountryId Buyer country id
     * @param buyerCurrency Buyer currency
     * @return Price plan matched
     * @throws NoPricePlanException No price plan line was matched
     * @throws InvalidELException Failed to evaluate EL expression
     */
    public PricePlanMatrix determineDefaultPricePlan(WalletOperation bareWo, Long buyerCountryId, TradingCurrency buyerCurrency) throws NoPricePlanException, InvalidELException {

        List<PricePlanMatrix> pps = getActivePricePlansByChargeCodeForRating(bareWo, buyerCountryId, buyerCurrency, true);
        return pps.get(0);
    }

    /**
     * Determine all price plans that match a wallet operation and buyer country and currency
     * 
     * @param bareWo Wallet operation to determine price for
     * @param buyerCountryId Buyer country id
     * @param buyerCurrency Buyer currency
     * @return A list of Price plans matched
     * @throws NoPricePlanException No price plan line was matched
     * @throws InvalidELException Failed to evaluate EL expression
     */
    public List<PricePlanMatrix> determineAvailablePricePlansForRating(WalletOperation bareWo, Long buyerCountryId, TradingCurrency buyerCurrency) {

        List<PricePlanMatrix> pps = getActivePricePlansByChargeCodeForRating(bareWo, buyerCountryId, buyerCurrency, false);

        return pps;
    }

    /**
     * Determine price plans that match a wallet operation and buyer country and currency
     * 
     * @param bareWo Wallet operation to determine price for
     * @param buyerCountryId Buyer country id
     * @param buyerCurrency Buyer currency
     * @param returnFirst Return only the best matched price plan - the first or with a highest priority
     * @return A list of Price plans matched
     * @throws NoPricePlanException No price plan line was matched
     * @throws InvalidELException Failed to evaluate EL expression
     */
    private List<PricePlanMatrix> getActivePricePlansByChargeCodeForRating(WalletOperation bareWo, Long buyerCountryId, TradingCurrency buyerCurrency, boolean returnFirst) {

        long subscriptionAge = 0;
        Date subscriptionDate = DateUtils.truncateTime(bareWo.getSubscriptionDate());
        Date operationDate = DateUtils.truncateTime(bareWo.getOperationDate());
        if (subscriptionDate != null && operationDate != null) {
            subscriptionAge = DateUtils.monthsBetween(operationDate, DateUtils.addDaysToDate(subscriptionDate, -1));
        }

        Date startDate = operationDate;
        Date endDate = operationDate;
        RecurringChargeTemplate recurringChargeTemplate = getRecurringChargeTemplateFromChargeInstance(bareWo.getChargeInstance());

        if ((recurringChargeTemplate != null && recurringChargeTemplate.isProrataOnPriceChange() && bareWo.getEndDate().after(bareWo.getStartDate()))) {
            startDate = DateUtils.truncateTime(bareWo.getStartDate());
            endDate = DateUtils.truncateTime(bareWo.getEndDate());
        }

        Map<String, Object> advancedSettingsValues = advancedSettingsService.getAdvancedSettingsMapByGroup("pricePlanFilters", Object.class);

        // A short match is data filtering in DB side only
        // A long match requires an additional record filtering in code once data is retrieved from DB

        boolean filterByCriteria1ValueInCode = advancedSettingsValues.get("pricePlanFilters.enableCriteria1Value") instanceof Integer && 2 == (Integer) advancedSettingsValues.get("pricePlanFilters.enableCriteria1Value");
        boolean filterByCriteria2ValueInCode = advancedSettingsValues.get("pricePlanFilters.enableCriteria2Value") instanceof Integer && 2 == (Integer) advancedSettingsValues.get("pricePlanFilters.enableCriteria2Value");
        boolean filterByCriteria3ValueInCode = advancedSettingsValues.get("pricePlanFilters.enableCriteria3Value") instanceof Integer && 2 == (Integer) advancedSettingsValues.get("pricePlanFilters.enableCriteria3Value");
        boolean filterByValidityCalendarInCode = (Boolean) advancedSettingsValues.get("pricePlanFilters.enableValidityCalendar");
        boolean filterByCriteriaELInCode = (Boolean) advancedSettingsValues.get("pricePlanFilters.enableCriteriaEL");

        boolean enablePPFilters = (Boolean) advancedSettingsValues.get("pricePlanFilters.enablePricePlanFilters");

        boolean matchLong = enablePPFilters && (filterByCriteria1ValueInCode || filterByCriteria2ValueInCode || filterByCriteria3ValueInCode || filterByValidityCalendarInCode || filterByCriteriaELInCode);

        StringBuilder queryBuilder = null;
        if (matchLong) {
            queryBuilder = new StringBuilder(
                "SELECT new org.meveo.model.catalog.PricePlanMatrixForRating(ppm.id,  ppm.code, ppm.offerTemplate.id,  ppm.startSubscriptionDate,  ppm.endSubscriptionDate,  ppm.startRatingDate,  ppm.endRatingDate,  ppm.minQuantity, "
                        + "ppm.maxQuantity, ppm.minSubscriptionAgeInMonth, ppm.maxSubscriptionAgeInMonth,  ppm.criteria1Value,  ppm.criteria2Value,  ppm.criteria3Value,  ppm.criteriaEL,  ppm.amountWithoutTax, "
                        + "ppm.amountWithTax,  ppm.amountWithoutTaxEL,  ppm.amountWithTaxEL, ppm.tradingCurrency.id, ppm.tradingCountry.id,  ppm.priority, ppm.seller.id, ppm.validityCalendar.id, ppm.sequence, ppm.scriptInstance.id, "
                        + "ppm.totalAmountEL,  ppm.minimumAmountEL,  ppm.invoiceSubCategoryEL,  ppm.validityFrom,  ppm.validityDate) from PricePlanMatrix ppm join ppm.chargeTemplates as ct WHERE ppm.disabled = false AND ct.code = :chargeCode");
        } else {
            queryBuilder = new StringBuilder("SELECT ppm FROM PricePlanMatrix ppm join ppm.chargeTemplates as ct WHERE ppm.disabled = false AND ct.code = :chargeCode");
        }

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("chargeCode", bareWo.getCode());

        if (enablePPFilters) {
            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableSeller")) {
                queryBuilder.append(" AND (ppm.seller.id = :sellerId OR ppm.seller.id IS NULL)");
                queryParams.put("sellerId", bareWo.getSeller().getId());
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableOfferTemplate")) {
                queryBuilder.append(" AND (ppm.offerTemplate.id = :offerId OR ppm.offerTemplate.id IS NULL)");
                queryParams.put("offerId", bareWo.getOfferTemplate() != null ? bareWo.getOfferTemplate().getId() : null);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableTradingCountry")) {
                queryBuilder.append(" AND (ppm.tradingCountry.id = :tradingCountryId OR ppm.tradingCountry.id IS NULL)");
                queryParams.put("tradingCountryId", buyerCountryId);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableTradingCurrency")) {
                queryBuilder.append(" AND (ppm.tradingCurrency.id = :tradingCurrencyId OR ppm.tradingCurrency.id IS NULL)");
                queryParams.put("tradingCurrencyId", buyerCurrency != null ? buyerCurrency.getId() : null);
            }

            // Value of FALSE or 1 will filter by criteria1Value property in DB
            if ((advancedSettingsValues.get("pricePlanFilters.enableCriteria1Value") instanceof Boolean && (Boolean) advancedSettingsValues.get("pricePlanFilters.enableCriteria1Value"))
                    || (advancedSettingsValues.get("pricePlanFilters.enableCriteria1Value") instanceof Integer && 1 == (Integer) advancedSettingsValues.get("pricePlanFilters.enableCriteria1Value"))) {
                queryBuilder.append(" AND (ppm.criteria1Value = :param1 OR ppm.criteria1Value IS NULL)");
                queryParams.put("param1", bareWo.getParameter1());
            }

            // Value of FALSE or 1 will filter by criteria2Value property in DB
            if ((advancedSettingsValues.get("pricePlanFilters.enableCriteria2Value") instanceof Boolean && (Boolean) advancedSettingsValues.get("pricePlanFilters.enableCriteria2Value"))
                    || (advancedSettingsValues.get("pricePlanFilters.enableCriteria2Value") instanceof Integer && 1 == (Integer) advancedSettingsValues.get("pricePlanFilters.enableCriteria2Value"))) {
                queryBuilder.append(" AND (ppm.criteria2Value = :param2 OR ppm.criteria2Value IS NULL)");
                queryParams.put("param2", bareWo.getParameter2());
            }

            // Value of FALSE or 1 will filter by criteria3Value property in DB
            if ((advancedSettingsValues.get("pricePlanFilters.enableCriteria3Value") instanceof Boolean && (Boolean) advancedSettingsValues.get("pricePlanFilters.enableCriteria3Value"))
                    || (advancedSettingsValues.get("pricePlanFilters.enableCriteria3Value") instanceof Integer && 1 == (Integer) advancedSettingsValues.get("pricePlanFilters.enableCriteria3Value"))) {
                queryBuilder.append(" AND (ppm.criteria3Value = :param3 OR ppm.criteria3Value IS NULL)");
                queryParams.put("param3", bareWo.getParameter3());
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableStartSubscriptionDate")) {
                queryBuilder.append(" AND (ppm.startSubscriptionDate IS NULL OR ppm.startSubscriptionDate <= :subscriptionDate)");
                queryParams.put("subscriptionDate", subscriptionDate);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableEndSubscriptionDate")) {
                queryBuilder.append(" AND (ppm.endSubscriptionDate IS NULL OR ppm.endSubscriptionDate > :subscriptionDate)");
                queryParams.put("subscriptionDate", subscriptionDate);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableMinSubscriptionAgeInMonth")) {
                queryBuilder.append(" AND (ppm.minSubscriptionAgeInMonth IS NULL OR ppm.minSubscriptionAgeInMonth <= :subscriptionAge)");
                queryParams.put("subscriptionAge", subscriptionAge);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableMaxSubscriptionAgeInMonth")) {
                queryBuilder.append(" AND (ppm.maxSubscriptionAgeInMonth IS NULL OR ppm.maxSubscriptionAgeInMonth > :subscriptionAge)");
                queryParams.put("subscriptionAge", subscriptionAge);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableStartRatingDate")) {
                queryBuilder.append(" AND (ppm.startRatingDate IS NULL OR ppm.startRatingDate <= :operationDate)");
                queryParams.put("operationDate", operationDate);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableEndRatingDate")) {
                queryBuilder.append(" AND (ppm.endRatingDate IS NULL OR ppm.endRatingDate > :operationDate)");
                queryParams.put("operationDate", operationDate);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableValidityFrom")) {
                queryBuilder.append(" AND (ppm.validityFrom IS NULL OR ppm.validityFrom < :startDate)");
                queryParams.put("startDate", startDate);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableValidityDate")) {
                queryBuilder.append(" AND (ppm.validityDate IS NULL OR ppm.validityDate >= :startDate OR ppm.validityDate >= :endDate)");
                queryParams.put("startDate", startDate);
                queryParams.put("endDate", endDate);
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableMaxQuantity")) {
                queryBuilder.append(" AND (ppm.maxQuantity IS NULL OR ppm.maxQuantity > :quantity)");
                queryParams.put("quantity", bareWo.getQuantity());
            }

            if ((Boolean) advancedSettingsValues.get("pricePlanFilters.enableMinQuantity")) {
                queryBuilder.append(" AND (ppm.minQuantity IS NULL OR ppm.minQuantity <= :quantity)");
                queryParams.put("quantity", bareWo.getQuantity());
            }
        }

        queryBuilder.append(" ORDER BY ppm.priority ASC, ppm.id");

        EntityManager em = getEntityManager();
        // A long match will retrieve multiple PricePlanMatrixForRating entities and will require a further filtering in code
        if (matchLong) {

            TypedQuery<PricePlanMatrixForRating> query = em.createQuery(queryBuilder.toString(), PricePlanMatrixForRating.class);
            query.setHint(QueryHints.CACHEABLE, true);
            query.setHint(QueryHints.CACHE_REGION, PricePlanMatrix.CACHE_REGION_PP);

            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            List<PricePlanMatrixForRating> pricePlansRating = query.getResultList();
            pricePlansRating = matchPricePlan(pricePlansRating, bareWo, buyerCountryId, buyerCurrency, filterByCriteria1ValueInCode, filterByCriteria2ValueInCode, filterByCriteria3ValueInCode, returnFirst);
            if (pricePlansRating.isEmpty()) {
                throw new NoPricePlanException("No active price plan matched for parameters: " + StringUtils.concatenate(queryParams));
            }

            List<PricePlanMatrix> pricePlans = new ArrayList<PricePlanMatrix>();
            for (PricePlanMatrixForRating pricePlan : pricePlansRating) {
                pricePlans.add(em.getReference(PricePlanMatrix.class, pricePlan.getId()));
            }

            return pricePlans;

            // A short match will retrieve a single PricePlanMatrix entity directly
        } else {

            TypedQuery<PricePlanMatrix> query = em.createQuery(queryBuilder.toString(), PricePlanMatrix.class);

            query.setHint(QueryHints.CACHEABLE, true);
            query.setHint(QueryHints.CACHE_REGION, PricePlanMatrix.CACHE_REGION_PP);
            query.setHint(QueryHints.READ_ONLY, true);

            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            if (returnFirst) {
                query.setMaxResults(1);
            }
            List<PricePlanMatrix> pricePlans = query.getResultList();
            if (pricePlans.isEmpty()) {
                throw new NoPricePlanException("No active price plan matched for parameters: " + StringUtils.concatenate(queryParams));
            }
            return pricePlans;
        }
    }

    /**
     * Find a matching price plan for a given wallet operation - used to resolve Price plan criteriaEL and validityCalendar fields that can not be done in DB or filter by param1, param2 or param3 fields if their values
     * are vary widely
     *
     * @param listPricePlan List of price plans to consider
     * @param bareOperation Wallet operation to lookup price plan for
     * @param buyerCountryId Buyer's county id
     * @param buyerCurrency Buyer's trading currency
     * @param filterByCriteria3Value Filer PP by
     * @param filterByCriteria2Value
     * @param filterByCriteria1Value
     * @param returnFirst Return only the first matched price plan
     * @return Matched price plan
     * @throws InvalidELException Failed to evaluate EL expression
     */
    private List<PricePlanMatrixForRating> matchPricePlan(List<PricePlanMatrixForRating> listPricePlan, WalletOperation bareOperation, Long buyerCountryId, TradingCurrency buyerCurrency, boolean filterByCriteria1Value,
            boolean filterByCriteria2Value, boolean filterByCriteria3Value, boolean returnFirst) throws InvalidELException {

        List<PricePlanMatrixForRating> ppsMatched = new ArrayList<PricePlanMatrixForRating>();

        for (PricePlanMatrixForRating pricePlan : listPricePlan) {

            log.trace("Try to verify price plan {} for WO {}", pricePlan.getId(), bareOperation.getCode());

            if (filterByCriteria1Value && pricePlan.getCriteria1Value() != null && !pricePlan.getCriteria1Value().equals(bareOperation.getParameter1())) {
                continue;
            }

            if (filterByCriteria2Value && pricePlan.getCriteria2Value() != null && !pricePlan.getCriteria2Value().equals(bareOperation.getParameter2())) {
                continue;
            }

            if (filterByCriteria3Value && pricePlan.getCriteria3Value() != null && !pricePlan.getCriteria3Value().equals(bareOperation.getParameter3())) {
                continue;
            }

            if (!StringUtils.isBlank(pricePlan.getCriteriaEL())) {
                UserAccount ua = bareOperation.getWallet().getUserAccount();
                if (!elUtils.evaluateBooleanExpression(pricePlan.getCriteriaEL(), bareOperation, ua, null, pricePlan, null)) {
                    // log.trace("The operation is not compatible with price plan criteria EL: {}", pricePlan.getCriteriaEL());
                    continue;
                }
            }

            if (pricePlan.getValidityCalendar() != null) {
                org.meveo.model.catalog.Calendar validityCalendar = getEntityManager().find(org.meveo.model.catalog.Calendar.class, pricePlan.getValidityCalendar());
                boolean validityCalendarOK = validityCalendar.previousCalendarDate(bareOperation.getOperationDate()) != null;
                if (!validityCalendarOK) {
                    // log.trace("The operation date " + operationDate + " does not match pricePlan validity calendar " + validityCalendar.getCode() + "period range ");
                    continue;
                }
            }

            ppsMatched.add(pricePlan);
            if (returnFirst) {
                return ppsMatched;
            }
        }
        return ppsMatched;

    }

    /**
     * get pricePlanVersion Valid for the given operationDate
     * 
     * @param pricePlanId Price plan ID
     * @param serviceInstance Service instance
     * @param operationDate Operation date
     * @return PricePlanMatrixVersion Matched Price plan version or NULL if nothing found
     * @throws RatingException More than one Price plan version was found for a given date
     */
    public PricePlanMatrixVersion getPublishedVersionValidForDate(Long pricePlanId, ServiceInstance serviceInstance, Date operationDate) throws RatingException {
        Date operationDateParam = new Date();
        if (serviceInstance == null || serviceInstance.getPriceVersionDateSetting() == null || PriceVersionDateSettingEnum.EVENT.equals(serviceInstance.getPriceVersionDateSetting())) {
            operationDateParam = operationDate;
        } else if (PriceVersionDateSettingEnum.DELIVERY.equals(serviceInstance.getPriceVersionDateSetting()) || PriceVersionDateSettingEnum.RENEWAL.equals(serviceInstance.getPriceVersionDateSetting())
                || PriceVersionDateSettingEnum.QUOTE.equals(serviceInstance.getPriceVersionDateSetting())) {
            operationDateParam = serviceInstance.getPriceVersionDate() != null ? serviceInstance.getPriceVersionDate() : operationDate;
        }

        if (operationDateParam != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(operationDate);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            operationDateParam = calendar.getTime();
        }

        List<PricePlanMatrixVersion> result = this.getEntityManager().createNamedQuery("PricePlanMatrixVersion.getPublishedVersionValideForDate", PricePlanMatrixVersion.class)
            .setParameter("pricePlanMatrixId", pricePlanId).setParameter("operationDate", operationDateParam).getResultList();
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        if (result.size() > 1) {
            throw new RatingException("More than one pricePlanVersion for pricePlan '" + pricePlanId + "' matching date: " + operationDate);
        }
        return result.get(0);
    }

    /**
     * Get a valid Price plan version for the given operationDate.
     * 
     * @param ppmCode Price plan code
     * @param serviceInstance Service instance
     * @param operationDate Operation date
     * @return PricePlanMatrixVersion Matched Price plan version or NULL if nothing found
     * @throws RatingException More than one Price plan version was found for a given date
     */
    public PricePlanMatrixVersion getPublishedVersionValideForDate(String ppmCode, ServiceInstance serviceInstance, Date operationDate) throws RatingException {
        Date operationDateParam = new Date();
        if (serviceInstance == null || PriceVersionDateSettingEnum.EVENT.equals(serviceInstance.getPriceVersionDateSetting())) {
            operationDateParam = operationDate;
        } else if (PriceVersionDateSettingEnum.DELIVERY.equals(serviceInstance.getPriceVersionDateSetting()) || PriceVersionDateSettingEnum.RENEWAL.equals(serviceInstance.getPriceVersionDateSetting())
                || PriceVersionDateSettingEnum.QUOTE.equals(serviceInstance.getPriceVersionDateSetting())) {
            operationDateParam = serviceInstance.getPriceVersionDate();
        }

        if (operationDateParam != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(operationDate);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            operationDateParam = calendar.getTime();
        }

        List<PricePlanMatrixVersion> result = this.getEntityManager().createNamedQuery("PricePlanMatrixVersion.getPublishedVersionValideForDateByPpmCode", PricePlanMatrixVersion.class)
            .setParameter("pricePlanMatrixCode", ppmCode).setParameter("operationDate", operationDateParam).getResultList();
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        if (result.size() > 1) {
            throw new RatingException("More than one pricePlaneVersion for pricePlan '" + ppmCode + "' matching date: " + operationDate);
        }
        return result.get(0);
    }

    /**
     * Determine a price plan matrix line matching wallet operation parameters. Business type attributes defined in a price plan version will be resolved via Wallet operation properties.
     * 
     * @param pricePlan Applicable price plan
     * @param bareWalletOperation Wallet operation to match
     * @return A matched Price plan matrix line
     * @throws NoPricePlanException No price plan line was matched
     */
    public PricePlanMatrixLine determinePricePlanLine(PricePlanMatrix pricePlan, WalletOperation bareWalletOperation) throws NoPricePlanException {
        return determinePricePlanLine(pricePlan.getId(), bareWalletOperation);
    }

    /**
     * Determine a price plan matrix line matching wallet operation parameters. Business type attributes defined in a price plan version will be resolved via Wallet operation properties.
     * 
     * @param pricePlanId Applicable price plan ID
     * @param bareWalletOperation Wallet operation to match
     * @return A matched Price plan matrix line
     * @throws NoPricePlanException No price plan line was matched
     */
    public PricePlanMatrixLine determinePricePlanLine(Long pricePlanId, WalletOperation bareWalletOperation) throws NoPricePlanException {
        PricePlanMatrixVersion ppmVersion = getPublishedVersionValidForDate(pricePlanId, bareWalletOperation.getServiceInstance(), bareWalletOperation.getOperationDate());
        if (ppmVersion != null) {
            PricePlanMatrixLine ppLine = determinePricePlanLine(ppmVersion, bareWalletOperation);
            if (ppLine != null) {
                return ppLine;
            }
        }
        return null;
    }

    /**
     * Determine a price plan matrix line matching wallet operation parameters. Business type attributes defined in a price plan version will be resolved via Wallet operation properties.
     * 
     * @param pricePlanMatrixVersion Price plan version
     * @param walletOperation Wallet operation to match
     * @return A matched Price plan matrix line
     * @throws NoPricePlanException No price plan line was matched
     */
    @SuppressWarnings("rawtypes")
    public PricePlanMatrixLine determinePricePlanLine(PricePlanMatrixVersion pricePlanMatrixVersion, WalletOperation walletOperation) throws NoPricePlanException {
        ChargeInstance chargeInstance = walletOperation.getChargeInstance();
        if (chargeInstance.getServiceInstance() == null) {
            return null;
        }
        Set<AttributeValue> attributeValues = chargeInstance.getServiceInstance().getAttributeInstances().stream().map(attributeInstance -> attributeInstanceService.getAttributeValue(attributeInstance, walletOperation))
            .filter(value -> value != null).collect(Collectors.toSet());

        addBusinessAttributeValues(pricePlanMatrixVersion.getColumns().stream().filter(column -> AttributeCategoryEnum.BUSINESS.equals(column.getAttribute().getAttributeCategory())).map(column -> column.getAttribute())
            .collect(Collectors.toList()), attributeValues, walletOperation);

        return determinePricePlanLine(pricePlanMatrixVersion, attributeValues);
    }

    /**
     * Determine a price matrix line matching the attribute values passed or throw and exception if matching was unsucessfull
     * 
     * @param pricePlanMatrixVersion Price plan version
     * @param attributeValues Attributes to match
     * @return A matched price matrix line
     * @throws NoPricePlanException No matching price line was found
     */
    @SuppressWarnings("rawtypes")
    public PricePlanMatrixLine determinePricePlanLine(PricePlanMatrixVersion pricePlanMatrixVersion, Set<AttributeValue> attributeValues) throws NoPricePlanException {
        PricePlanMatrixLine ppLine = determinePricePlanLineOptional(pricePlanMatrixVersion, attributeValues);
        if (ppLine == null) {
            throw new NoPricePlanException("No price match with price plan matrix: (code : " + pricePlanMatrixVersion.getPricePlanMatrix().getCode() + ", version: " + pricePlanMatrixVersion.getCurrentVersion() + " id: "
                    + pricePlanMatrixVersion.getId() + ") using attributes : " + attributeValues);
        }
        return ppLine;
    }

    /**
     * Determine a price matrix line matching the attribute values passed
     * 
     * @param pricePlanMatrixVersion Price plan version
     * @param attributeValues Attributes to match
     * @return A matched price matrix line or NULL if no line was matched
     */
    @SuppressWarnings("rawtypes")
    public PricePlanMatrixLine determinePricePlanLineOptional(PricePlanMatrixVersion pricePlanMatrixVersion, Set<AttributeValue> attributeValues) {

        EntityManager em = getEntityManager();

        if (attributeValues == null || attributeValues.isEmpty()) {

            try {
                return em.createNamedQuery("PricePlanMatrixLine.findDefaultByPricePlanMatrixVersion", PricePlanMatrixLine.class).setParameter("pricePlanMatrixVersionId", pricePlanMatrixVersion.getId()).setMaxResults(1)
                    .getSingleResult();
            } catch (NoResultException e) {
                return null;
            }

        } else {

            List<PricePlanMatrixValueForRating> ppValues = em.createNamedQuery("PricePlanMatrixValue.findByPPVersionForRating", PricePlanMatrixValueForRating.class)
                .setParameter("pricePlanMatrixVersionId", pricePlanMatrixVersion.getId()).getResultList();

            long lastPLId = -100;// A value indicating its initial value. Real values are expected to be above 0.
            boolean allMatch = true;
            Long matchedPlId = null;
            for (PricePlanMatrixValueForRating ppValue : ppValues) {
                // A new price plan Line
                if (lastPLId != ppValue.getPricePlanMatrixLineId()) {
                    // All values were matched in the Last Price plan line
                    if (lastPLId > 0 && allMatch) {
                        matchedPlId = lastPLId;
                        break;
                    }
                    lastPLId = ppValue.getPricePlanMatrixLineId();

                } else if (!allMatch) {
                    continue;
                }

                allMatch = ppValue.isMatch(attributeValues);

            }
            // Case when was matched the last item in the ppValue list (e.g. default value)
            if (matchedPlId == null && (lastPLId > 0 && allMatch)) {
                matchedPlId = lastPLId;
            }
            if (matchedPlId != null) {
                return em.find(PricePlanMatrixLine.class, matchedPlId);
            }
        }

        return null;
    }

    /**
     * Business type attributes are resolved via Wallet operation properties
     * 
     * @param businessAttributes Business type attributes
     * @param attributeValues Attribute values to supplement with new attributes
     * @param walletOperation Wallet operation to use for EL expression resolution
     */
    private void addBusinessAttributeValues(List<Attribute> businessAttributes, @SuppressWarnings("rawtypes") Set<AttributeValue> attributeValues, WalletOperation walletOperation) {
        businessAttributes.stream().forEach(attribute -> attributeValues.add(getBusinessAttributeValue(attribute, walletOperation)));
    }

    /**
     * Resolve attribute value from EL expression
     * 
     * @param attribute Attribute to resolve
     * @param op Wallet operation to use for EL expression resolution
     * @return Resolved Attribute value
     */
    @SuppressWarnings("rawtypes")
    private AttributeValue getBusinessAttributeValue(Attribute attribute, WalletOperation op) {
        Object value = ValueExpressionWrapper.evaluateExpression(attribute.getElValue(), Object.class, op);
        AttributeValue<AttributeValue> attributeValue = new AttributeValue<AttributeValue>(attribute, value);
        return attributeValue;
    }

    private RecurringChargeTemplate getRecurringChargeTemplateFromChargeInstance(ChargeInstance chargeInstance) {
        RecurringChargeTemplate recurringChargeTemplate = null;
        if (chargeInstance != null && chargeInstance.getChargeMainType() == ChargeTemplate.ChargeMainTypeEnum.RECURRING) {
            recurringChargeTemplate = ((RecurringChargeInstance) PersistenceUtils.initializeAndUnproxy(chargeInstance)).getRecurringChargeTemplate();
        }
        return recurringChargeTemplate;
    }
}