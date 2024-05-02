package org.meveo.apiv2.catalog.service;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.catalog.GenericChargeTemplateApi;
import org.meveo.api.catalog.OneShotChargeTemplateApi;
import org.meveo.api.catalog.PricePlanMatrixApi;
import org.meveo.api.catalog.PricePlanMatrixVersionApi;
import org.meveo.api.catalog.RecurringChargeTemplateApi;
import org.meveo.api.catalog.UsageChargeTemplateApi;
import org.meveo.api.cpq.ProductApi;
import org.meveo.api.dto.catalog.ChargeTemplateDto;
import org.meveo.api.dto.catalog.OneShotChargeTemplateDto;
import org.meveo.api.dto.catalog.PricePlanMatrixDto;
import org.meveo.api.dto.catalog.PricePlanMatrixVersionDto;
import org.meveo.api.dto.catalog.RecurringChargeTemplateDto;
import org.meveo.api.dto.catalog.UsageChargeTemplateDto;
import org.meveo.api.dto.cpq.ProductChargeTemplateMappingDto;
import org.meveo.api.dto.cpq.ProductDto;
import org.meveo.api.dto.cpq.ProductVersionDto;
import org.meveo.apiv2.catalog.SimpleChargeProductDto;
import org.meveo.apiv2.catalog.SimpleOneshotProductDto;
import org.meveo.apiv2.catalog.SimpleRecurrentProductDto;
import org.meveo.apiv2.catalog.SimpleUsageProductDto;
import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.catalog.ChargeTemplateStatusEnum;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.UsageChargeTemplate;
import org.meveo.model.cpq.enums.PriceVersionTypeEnum;
import org.meveo.model.cpq.enums.ProductStatusEnum;
import org.meveo.model.cpq.enums.VersionStatusEnum;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class ProductManagementApiService extends BaseApi {
    
    @Inject
    private OneShotChargeTemplateApi oneShotChargeTemplateApi;

    @Inject
    private RecurringChargeTemplateApi recurringChargeTemplateApi;

    @Inject
    private UsageChargeTemplateApi usageChargeTemplateApi;

    @Inject
    private PricePlanMatrixApi pricePlanMatrixApi;

    @Inject
    private PricePlanMatrixVersionApi pricePlanMatrixVersionApi;

    @Inject
    private GenericChargeTemplateApi genericChargeTemplateApi;

    @Inject
    private ProductApi productApi;
    
    @MeveoJpa
    @Inject
    private EntityManagerWrapper emWrapper; 
    
    public ProductDto createProductSimpleOneShot(SimpleOneshotProductDto postData) {

        if(StringUtils.isBlank(postData.getChargeCode()) && StringUtils.isBlank(postData.getProductCode())) {
            throw new BusinessException("Either chargeCode or productCode must be provided");
        }

        OneShotChargeTemplateDto oneShotChargeTemplate = buildOneShotChargeTemplateDto(postData);
        OneShotChargeTemplate createdCharge = oneShotChargeTemplateApi.create(oneShotChargeTemplate);
        EntityManager entityManager = emWrapper.getEntityManager();
        entityManager.flush();

        PricePlanMatrixDto pricePlanMatrixDto = new PricePlanMatrixDto();
        pricePlanMatrixDto.setCode("PPM_"+oneShotChargeTemplate.getCode());
        pricePlanMatrixDto.setEventCode(oneShotChargeTemplate.getCode());
        PricePlanMatrix createdPP = pricePlanMatrixApi.create(pricePlanMatrixDto);
        entityManager.flush();

        PricePlanMatrixVersionDto pricePlanMatrixVersionDto = createPricePlanVersionDto(postData, pricePlanMatrixDto);
        pricePlanMatrixVersionApi.create(pricePlanMatrixVersionDto);
        entityManager.flush();
        
        
        pricePlanMatrixVersionApi.updateProductVersionStatus(pricePlanMatrixDto.getCode(), 1, VersionStatusEnum.PUBLISHED);
        genericChargeTemplateApi.updateStatus(oneShotChargeTemplate.getCode(), ChargeTemplateStatusEnum.ACTIVE.name());
        entityManager.flush();

        ProductDto productDto = new ProductDto();
        productDto.setCode(postData.getProductCode());
        productDto.setLabel(postData.getLabel());
        productDto.setCurrentProductVersion(new ProductVersionDto());
        productDto.getCurrentProductVersion().setCurrentVersion(1);
        productDto.getCurrentProductVersion().setProductCode(postData.getProductCode());
        productDto.getCurrentProductVersion().setStatus(VersionStatusEnum.DRAFT);
        productDto.getCurrentProductVersion().setShortDescription("Product " + postData.getProductCode());
        productDto.setProductChargeTemplateMappingDto(new ArrayList<>());
        ProductChargeTemplateMappingDto mapping = new ProductChargeTemplateMappingDto();
        mapping.setChargeCode(oneShotChargeTemplate.getCode());
        mapping.setProductCode(productDto.getCode());
        productDto.getProductChargeTemplateMappingDto().add(mapping);
        ProductDto createdProduct = productApi.create(productDto);
        entityManager.flush();
        
        productApi.UpdateProductVersionStatus(productDto.getCode(), 1, VersionStatusEnum.PUBLISHED);
        entityManager.flush();
        productApi.updateStatus(productDto.getCode(), ProductStatusEnum.ACTIVE);
        entityManager.flush();

        createdProduct = productApi.findByCode(createdProduct.getCode());
        return createdProduct;
    }

    public ProductDto createProductSimpleRecurrent(SimpleRecurrentProductDto postData) {

        RecurringChargeTemplateDto recurringChargeTemplateDto = buildRecurringChargeTemplateDto(postData);
        RecurringChargeTemplate createdCharge = recurringChargeTemplateApi.create(recurringChargeTemplateDto);
        EntityManager entityManager = emWrapper.getEntityManager();
        entityManager.flush();

        PricePlanMatrixDto pricePlanMatrixDto = new PricePlanMatrixDto();
        pricePlanMatrixDto.setCode("PPM_"+recurringChargeTemplateDto.getCode());
        pricePlanMatrixDto.setEventCode(recurringChargeTemplateDto.getCode());
        PricePlanMatrix createdPP = pricePlanMatrixApi.create(pricePlanMatrixDto);
        entityManager.flush();

        PricePlanMatrixVersionDto pricePlanMatrixVersionDto = createPricePlanVersionDto(postData, pricePlanMatrixDto);
        pricePlanMatrixVersionApi.create(pricePlanMatrixVersionDto);
        entityManager.flush();


        pricePlanMatrixVersionApi.updateProductVersionStatus(pricePlanMatrixDto.getCode(), 1, VersionStatusEnum.PUBLISHED);
        genericChargeTemplateApi.updateStatus(recurringChargeTemplateDto.getCode(), ChargeTemplateStatusEnum.ACTIVE.name());

        ProductDto productDto = new ProductDto();
        productDto.setCode(postData.getProductCode());
        productDto.setLabel(postData.getLabel());
        productDto.setCurrentProductVersion(new ProductVersionDto());
        productDto.getCurrentProductVersion().setCurrentVersion(1);
        productDto.getCurrentProductVersion().setProductCode(postData.getProductCode());
        productDto.getCurrentProductVersion().setStatus(VersionStatusEnum.DRAFT);
        productDto.getCurrentProductVersion().setShortDescription("Product " + postData.getProductCode());
        productDto.setProductChargeTemplateMappingDto(new ArrayList<>());
        ProductChargeTemplateMappingDto mapping = new ProductChargeTemplateMappingDto();
        mapping.setChargeCode(recurringChargeTemplateDto.getCode());
        mapping.setProductCode(productDto.getCode());
        productDto.getProductChargeTemplateMappingDto().add(mapping);
        ProductDto createdProduct = productApi.create(productDto);
        entityManager.flush();

        productApi.UpdateProductVersionStatus(productDto.getCode(), 1, VersionStatusEnum.PUBLISHED);
        entityManager.flush();
        productApi.updateStatus(productDto.getCode(), ProductStatusEnum.ACTIVE);
        entityManager.flush();
        
        createdProduct = productApi.findByCode(createdProduct.getCode());
        return createdProduct;

    }

    public ProductDto createProductSimpleUsage(SimpleUsageProductDto postData) {
        
        UsageChargeTemplateDto usageChargeTemplateDto = buildUsageChargeTemplateDto(postData);
        UsageChargeTemplate createdCharge = usageChargeTemplateApi.create(usageChargeTemplateDto);
        EntityManager entityManager = emWrapper.getEntityManager();
        entityManager.flush();

        PricePlanMatrixDto pricePlanMatrixDto = new PricePlanMatrixDto();
        pricePlanMatrixDto.setCode("PPM_"+usageChargeTemplateDto.getCode());
        pricePlanMatrixDto.setEventCode(usageChargeTemplateDto.getCode());
        PricePlanMatrix createdPP = pricePlanMatrixApi.create(pricePlanMatrixDto);
        entityManager.flush();

        PricePlanMatrixVersionDto pricePlanMatrixVersionDto = createPricePlanVersionDto(postData, pricePlanMatrixDto);
        pricePlanMatrixVersionApi.create(pricePlanMatrixVersionDto);
        entityManager.flush();


        pricePlanMatrixVersionApi.updateProductVersionStatus(pricePlanMatrixDto.getCode(), 1, VersionStatusEnum.PUBLISHED);
        genericChargeTemplateApi.updateStatus(usageChargeTemplateDto.getCode(), ChargeTemplateStatusEnum.ACTIVE.name());

        ProductDto productDto = new ProductDto();
        productDto.setCode(postData.getProductCode());
        productDto.setLabel(postData.getLabel());
        productDto.setCurrentProductVersion(new ProductVersionDto());
        productDto.getCurrentProductVersion().setCurrentVersion(1);
        productDto.getCurrentProductVersion().setProductCode(postData.getProductCode());
        productDto.getCurrentProductVersion().setStatus(VersionStatusEnum.DRAFT);
        productDto.getCurrentProductVersion().setShortDescription("Product " + postData.getProductCode());
        productDto.setProductChargeTemplateMappingDto(new ArrayList<>());
        ProductChargeTemplateMappingDto mapping = new ProductChargeTemplateMappingDto();
        mapping.setChargeCode(usageChargeTemplateDto.getCode());
        mapping.setProductCode(productDto.getCode());
        productDto.getProductChargeTemplateMappingDto().add(mapping);
        ProductDto createdProduct = productApi.create(productDto);
        entityManager.flush();

        productApi.UpdateProductVersionStatus(productDto.getCode(), 1, VersionStatusEnum.PUBLISHED);
        entityManager.flush();
        productApi.updateStatus(productDto.getCode(), ProductStatusEnum.ACTIVE);
        entityManager.flush();

        createdProduct = productApi.findByCode(createdProduct.getCode());
        return createdProduct;
    }

    private static PricePlanMatrixVersionDto createPricePlanVersionDto(SimpleChargeProductDto postData, PricePlanMatrixDto pricePlanMatrixDto) {
        PricePlanMatrixVersionDto pricePlanMatrixVersionDto = new PricePlanMatrixVersionDto();
        pricePlanMatrixVersionDto.setPricePlanMatrixCode(pricePlanMatrixDto.getCode());
        pricePlanMatrixVersionDto.setLabel(postData.getLabel());
        pricePlanMatrixVersionDto.setPrice(postData.getPrice());
        pricePlanMatrixVersionDto.setValidity(postData.getValidity());
        pricePlanMatrixVersionDto.setVersion(1);
        pricePlanMatrixVersionDto.setPriceVersionType(PriceVersionTypeEnum.FIXED);
        pricePlanMatrixVersionDto.setMatrix(false);
        return pricePlanMatrixVersionDto;
    }

    private OneShotChargeTemplateDto buildOneShotChargeTemplateDto(SimpleOneshotProductDto postData) {
        OneShotChargeTemplateDto oneShotChargeTemplate = new OneShotChargeTemplateDto();

        populateCommonsData(postData, oneShotChargeTemplate);
        oneShotChargeTemplate.setOneShotChargeTemplateType(postData.getOneShotChargeTemplateType());
        oneShotChargeTemplate.setCode(StringUtils.getDefaultIfEmpty(postData.getChargeCode(), "CH_ONESHOT_" + postData.getProductCode()));

        return oneShotChargeTemplate;
    }

    private RecurringChargeTemplateDto buildRecurringChargeTemplateDto(SimpleRecurrentProductDto postData) {
        RecurringChargeTemplateDto recurringChargeTemplate = new RecurringChargeTemplateDto();
        
        populateCommonsData(postData, recurringChargeTemplate);
        recurringChargeTemplate.setCode(StringUtils.getDefaultIfEmpty(postData.getChargeCode(), "CH_REC_" + postData.getProductCode()));
        recurringChargeTemplate.setSubscriptionProrata(postData.getSubscriptionProrata());
        recurringChargeTemplate.setTerminationProrata(postData.getTerminationProrata());
        recurringChargeTemplate.setApplyInAdvance(postData.getApplyInAdvance());
        recurringChargeTemplate.setAnticipateEndOfSubscription(postData.getAnticipateEndOfSubscription());
        recurringChargeTemplate.setCalendar(postData.getCalendar());
        
        return recurringChargeTemplate;
    }
    
    private UsageChargeTemplateDto buildUsageChargeTemplateDto(SimpleUsageProductDto postData) {
        UsageChargeTemplateDto usageChargeTemplate = new UsageChargeTemplateDto();
        
        populateCommonsData(postData, usageChargeTemplate);
        usageChargeTemplate.setCode(StringUtils.getDefaultIfEmpty(postData.getChargeCode(), "CH_USAGE_" + postData.getProductCode()));
        usageChargeTemplate.setFilterParam1(postData.getFilterParam1());
        usageChargeTemplate.setFilterParam2(postData.getFilterParam2());
        usageChargeTemplate.setFilterParam3(postData.getFilterParam3());
        usageChargeTemplate.setFilterParam4(postData.getFilterParam4());
        
        return usageChargeTemplate;
    }

    private void populateCommonsData(SimpleChargeProductDto postData, ChargeTemplateDto destData) {
        destData.setCode(postData.getChargeCode());
        destData.setDescription(postData.getLabel());

        if (postData.getParameter1Description() != null) {
            destData.setParameter1Description(postData.getParameter1Description());
        }
        if (postData.getParameter1TranslatedDescriptions() != null && !postData.getParameter1TranslatedDescriptions().isEmpty()) {
            destData.setParameter1TranslatedDescriptions(postData.getParameter1TranslatedDescriptions());
        }
        if (postData.getParameter1TranslatedLongDescriptions() != null && !postData.getParameter1TranslatedLongDescriptions().isEmpty()) {
            destData.setParameter1TranslatedLongDescriptions(postData.getParameter1TranslatedLongDescriptions());
        }
        if (postData.getParameter1Format() != null) {
            destData.setParameter1Format(postData.getParameter1Format());
        }
        if (postData.getParameter1IsMandatory() != null) {
            destData.setParameter1IsMandatory(postData.getParameter1IsMandatory());
        }
        if (postData.getParameter1IsHidden() != null) {
            destData.setParameter1IsHidden(postData.getParameter1IsHidden());
        }
        if (postData.getParameter2Description() != null) {
            destData.setParameter2Description(postData.getParameter2Description());
        }
        if (postData.getParameter2TranslatedDescriptions() != null && !postData.getParameter2TranslatedDescriptions().isEmpty()) {
            destData.setParameter2TranslatedDescriptions(postData.getParameter2TranslatedDescriptions());
        }
        if (postData.getParameter2TranslatedLongDescriptions() != null && !postData.getParameter2TranslatedLongDescriptions().isEmpty()) {
            destData.setParameter2TranslatedLongDescriptions(postData.getParameter2TranslatedLongDescriptions());
        }
        if (postData.getParameter2Format() != null) {
            destData.setParameter2Format(postData.getParameter2Format());
        }
        if (postData.getParameter2IsMandatory() != null) {
            destData.setParameter2IsMandatory(postData.getParameter2IsMandatory());
        }
        if (postData.getParameter2IsHidden() != null) {
            destData.setParameter2IsHidden(postData.getParameter2IsHidden());
        }

        if (postData.getParameter3Description() != null) {
            destData.setParameter3Description(postData.getParameter3Description());
        }
        if (postData.getParameter3TranslatedDescriptions() != null && !postData.getParameter3TranslatedDescriptions().isEmpty()) {
            destData.setParameter3TranslatedDescriptions(postData.getParameter3TranslatedDescriptions());
        }
        if (postData.getParameter3TranslatedLongDescriptions() != null && !postData.getParameter3TranslatedLongDescriptions().isEmpty()) {
            destData.setParameter3TranslatedLongDescriptions(postData.getParameter3TranslatedLongDescriptions());
        }
        if (postData.getParameter3Format() != null) {
            destData.setParameter3Format(postData.getParameter3Format());
        }
        if (postData.getParameter3IsMandatory() != null) {
            destData.setParameter3IsMandatory(postData.getParameter3IsMandatory());
        }
        if (postData.getParameter3IsHidden() != null) {
            destData.setParameter3IsHidden(postData.getParameter3IsHidden());
        }

        if (postData.getParameterExtraDescription() != null) {
            destData.setParameterExtraDescription(postData.getParameterExtraDescription());
        }
        if (postData.getParameterExtraTranslatedDescriptions() != null && !postData.getParameterExtraTranslatedDescriptions().isEmpty()) {
            destData.setParameterExtraTranslatedDescriptions(postData.getParameterExtraTranslatedDescriptions());
        }
        if (postData.getParameterExtraTranslatedLongDescriptions() != null && !postData.getParameterExtraTranslatedLongDescriptions().isEmpty()) {
            destData.setParameterExtraTranslatedLongDescriptions(postData.getParameterExtraTranslatedLongDescriptions());
        }
        if (postData.getParameterExtraFormat() != null) {
            destData.setParameterExtraFormat(postData.getParameterExtraFormat());
        }
        if (postData.getParameterExtraIsMandatory() != null) {
            destData.setParameterExtraIsMandatory(postData.getParameterExtraIsMandatory());
        }
        if (postData.getParameterExtraIsHidden() != null) {
            destData.setParameterExtraIsHidden(postData.getParameterExtraIsHidden());
        }
        
    }
}
