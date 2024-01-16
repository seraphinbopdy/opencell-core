package org.meveo.apiv2.catalog.service;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.catalog.GenericChargeTemplateApi;
import org.meveo.api.catalog.OneShotChargeTemplateApi;
import org.meveo.api.catalog.PricePlanMatrixApi;
import org.meveo.api.catalog.PricePlanMatrixVersionApi;
import org.meveo.api.cpq.ProductApi;
import org.meveo.api.dto.catalog.OneShotChargeTemplateDto;
import org.meveo.api.dto.catalog.PricePlanMatrixDto;
import org.meveo.api.dto.catalog.PricePlanMatrixVersionDto;
import org.meveo.api.dto.cpq.ProductChargeTemplateMappingDto;
import org.meveo.api.dto.cpq.ProductDto;
import org.meveo.api.dto.cpq.ProductVersionDto;
import org.meveo.apiv2.catalog.SimpleOneshotProductDto;
import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.catalog.ChargeTemplateStatusEnum;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.cpq.enums.PriceVersionTypeEnum;
import org.meveo.model.cpq.enums.ProductStatusEnum;
import org.meveo.model.cpq.enums.VersionStatusEnum;
import org.meveo.service.catalog.impl.OneShotChargeTemplateService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Set;

@Stateless
public class ProductManagementApiService extends BaseApi {
    
    @Inject
    private OneShotChargeTemplateApi oneShotChargeTemplateApi;

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
    
    public void createProductSimpleOneShot(SimpleOneshotProductDto postData) {

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
    }

    private static PricePlanMatrixVersionDto createPricePlanVersionDto(SimpleOneshotProductDto postData, PricePlanMatrixDto pricePlanMatrixDto) {
        PricePlanMatrixVersionDto pricePlanMatrixVersionDto = new PricePlanMatrixVersionDto();
        pricePlanMatrixVersionDto.setPricePlanMatrixCode(pricePlanMatrixDto.getCode());
        pricePlanMatrixVersionDto.setLabel(postData.getLabel());
        pricePlanMatrixVersionDto.setAmountWithoutTax(postData.getPrice());
        pricePlanMatrixVersionDto.setValidity(postData.getValidity());
        pricePlanMatrixVersionDto.setVersion(1);
        pricePlanMatrixVersionDto.setPriceVersionType(PriceVersionTypeEnum.FIXED);
        pricePlanMatrixVersionDto.setMatrix(false);
        return pricePlanMatrixVersionDto;
    }

    private OneShotChargeTemplateDto buildOneShotChargeTemplateDto(SimpleOneshotProductDto postData) {
        OneShotChargeTemplateDto oneShotChargeTemplate = new OneShotChargeTemplateDto();
        oneShotChargeTemplate.setCode(StringUtils.getDefaultIfEmpty(postData.getChargeCode(), "CH_ONESHOT_" + postData.getProductCode()));
        oneShotChargeTemplate.setDescription(postData.getLabel());
        oneShotChargeTemplate.setOneShotChargeTemplateType(postData.getOneShotChargeTemplateType());

        if (postData.getParameter1Description() != null) {
            oneShotChargeTemplate.setParameter1Description(postData.getParameter1Description());
        }
        if (postData.getParameter1TranslatedDescriptions() != null && !postData.getParameter1TranslatedDescriptions().isEmpty()) {
            oneShotChargeTemplate.setParameter1TranslatedDescriptions(postData.getParameter1TranslatedDescriptions());
        }
        if (postData.getParameter1TranslatedLongDescriptions() != null && !postData.getParameter1TranslatedLongDescriptions().isEmpty()) {
            oneShotChargeTemplate.setParameter1TranslatedLongDescriptions(postData.getParameter1TranslatedLongDescriptions());
        }
        if (postData.getParameter1Format() != null) {
            oneShotChargeTemplate.setParameter1Format(postData.getParameter1Format());
        }
        if (postData.getParameter1IsMandatory() != null) {
            oneShotChargeTemplate.setParameter1IsMandatory(postData.getParameter1IsMandatory());
        }
        if (postData.getParameter1IsHidden() != null) {
            oneShotChargeTemplate.setParameter1IsHidden(postData.getParameter1IsHidden());
        }
        if (postData.getParameter2Description() != null) {
            oneShotChargeTemplate.setParameter2Description(postData.getParameter2Description());
        }
        if (postData.getParameter2TranslatedDescriptions() != null && !postData.getParameter2TranslatedDescriptions().isEmpty()) {
            oneShotChargeTemplate.setParameter2TranslatedDescriptions(postData.getParameter2TranslatedDescriptions());
        }
        if (postData.getParameter2TranslatedLongDescriptions() != null && !postData.getParameter2TranslatedLongDescriptions().isEmpty()) {
            oneShotChargeTemplate.setParameter2TranslatedLongDescriptions(postData.getParameter2TranslatedLongDescriptions());
        }
        if (postData.getParameter2Format() != null) {
            oneShotChargeTemplate.setParameter2Format(postData.getParameter2Format());
        }
        if (postData.getParameter2IsMandatory() != null) {
            oneShotChargeTemplate.setParameter2IsMandatory(postData.getParameter2IsMandatory());
        }
        if (postData.getParameter2IsHidden() != null) {
            oneShotChargeTemplate.setParameter2IsHidden(postData.getParameter2IsHidden());
        }

        if (postData.getParameter3Description() != null) {
            oneShotChargeTemplate.setParameter3Description(postData.getParameter3Description());
        }
        if (postData.getParameter3TranslatedDescriptions() != null && !postData.getParameter3TranslatedDescriptions().isEmpty()) {
            oneShotChargeTemplate.setParameter3TranslatedDescriptions(postData.getParameter3TranslatedDescriptions());
        }
        if (postData.getParameter3TranslatedLongDescriptions() != null && !postData.getParameter3TranslatedLongDescriptions().isEmpty()) {
            oneShotChargeTemplate.setParameter3TranslatedLongDescriptions(postData.getParameter3TranslatedLongDescriptions());
        }
        if (postData.getParameter3Format() != null) {
            oneShotChargeTemplate.setParameter3Format(postData.getParameter3Format());
        }
        if (postData.getParameter3IsMandatory() != null) {
            oneShotChargeTemplate.setParameter3IsMandatory(postData.getParameter3IsMandatory());
        }
        if (postData.getParameter3IsHidden() != null) {
            oneShotChargeTemplate.setParameter3IsHidden(postData.getParameter3IsHidden());
        }

        if (postData.getParameterExtraDescription() != null) {
            oneShotChargeTemplate.setParameterExtraDescription(postData.getParameterExtraDescription());
        }
        if (postData.getParameterExtraTranslatedDescriptions() != null && !postData.getParameterExtraTranslatedDescriptions().isEmpty()) {
            oneShotChargeTemplate.setParameterExtraTranslatedDescriptions(postData.getParameterExtraTranslatedDescriptions());
        }
        if (postData.getParameterExtraTranslatedLongDescriptions() != null && !postData.getParameterExtraTranslatedLongDescriptions().isEmpty()) {
            oneShotChargeTemplate.setParameterExtraTranslatedLongDescriptions(postData.getParameterExtraTranslatedLongDescriptions());
        }
        if (postData.getParameterExtraFormat() != null) {
            oneShotChargeTemplate.setParameterExtraFormat(postData.getParameterExtraFormat());
        }
        if (postData.getParameterExtraIsMandatory() != null) {
            oneShotChargeTemplate.setParameterExtraIsMandatory(postData.getParameterExtraIsMandatory());
        }
        if (postData.getParameterExtraIsHidden() != null) {
            oneShotChargeTemplate.setParameterExtraIsHidden(postData.getParameterExtraIsHidden());
        }
        
        return oneShotChargeTemplate;
        
    }
    
}
