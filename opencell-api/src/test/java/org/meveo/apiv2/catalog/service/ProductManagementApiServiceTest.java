package org.meveo.apiv2.catalog.service;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.api.catalog.GenericChargeTemplateApi;
import org.meveo.api.catalog.OneShotChargeTemplateApi;
import org.meveo.api.catalog.PricePlanMatrixApi;
import org.meveo.api.catalog.PricePlanMatrixVersionApi;
import org.meveo.api.catalog.RecurringChargeTemplateApi;
import org.meveo.api.catalog.UsageChargeTemplateApi;
import org.meveo.api.cpq.ProductApi;
import org.meveo.api.dto.catalog.OneShotChargeTemplateDto;
import org.meveo.api.dto.catalog.PricePlanMatrixDto;
import org.meveo.api.dto.catalog.PricePlanMatrixVersionDto;
import org.meveo.api.dto.catalog.RecurringChargeTemplateDto;
import org.meveo.api.dto.catalog.UsageChargeTemplateDto;
import org.meveo.api.dto.cpq.ProductDto;
import org.meveo.apiv2.catalog.ImmutableSimpleOneshotProductDto;
import org.meveo.apiv2.catalog.ImmutableSimpleRecurrentProductDto;
import org.meveo.apiv2.catalog.ImmutableSimpleUsageProductDto;
import org.meveo.apiv2.catalog.SimpleOneshotProductDto;
import org.meveo.apiv2.catalog.SimpleRecurrentProductDto;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.model.DatePeriod;
import org.meveo.model.catalog.ChargeTemplateStatusEnum;
import org.meveo.model.catalog.OneShotChargeTemplateTypeEnum;
import org.meveo.model.cpq.enums.PriceVersionTypeEnum;
import org.meveo.model.cpq.enums.VersionStatusEnum;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProductManagementApiServiceTest extends TestCase {
    
    @InjectMocks
    private ProductManagementApiService productManagementApiService;
    
    @Mock
    private OneShotChargeTemplateApi oneshotChargeTemplateApi;

    @Mock
    private RecurringChargeTemplateApi recurringChargeTemplateApi;

    @Mock
    private UsageChargeTemplateApi usageChargeTemplateApi;

    @Mock
    private PricePlanMatrixApi pricePlanMatrixApi;

    @Mock
    private PricePlanMatrixVersionApi pricePlanMatrixVersionApi;

    @Mock
    private GenericChargeTemplateApi genericChargeTemplateApi;

    @Mock
    private ProductApi productApi;

    @Mock
    private EntityManagerWrapper emWrapper;

    @Mock
    private EntityManager entityManager;

    @Test
    public void testCreateProductSimpleOneShot() {
        
        // given
        SimpleOneshotProductDto postData = ImmutableSimpleOneshotProductDto.builder()
                                                                           .chargeCode("chargeCode")
                                                                           .productCode("productCode")
                                                                           .oneShotChargeTemplateType(OneShotChargeTemplateTypeEnum.SUBSCRIPTION)
                                                                           .label("chargeLabel")
                                                                           .validity(new DatePeriod(Date.from(LocalDateTime.of(2023, 1, 1, 0, 0, 0).toInstant(java.time.ZoneOffset.UTC)), 
                                                                                                    Date.from(LocalDateTime.of(2023, 12, 13, 23, 59, 59).toInstant(java.time.ZoneOffset.UTC))))
                                                                           .price(BigDecimal.valueOf(12345678.90))
                                                                           .build();


        when(emWrapper.getEntityManager()).thenReturn(entityManager);

        when(productApi.create(any())).thenReturn(new ProductDto());
        // when
        productManagementApiService.createProductSimpleOneShot(postData);
        
        // then
        var oneshotChargeCaptor = ArgumentCaptor.forClass(OneShotChargeTemplateDto.class);
        verify(oneshotChargeTemplateApi).create(oneshotChargeCaptor.capture());

        OneShotChargeTemplateDto osCharge = oneshotChargeCaptor.getValue();
        assertThat(osCharge).isNotNull();
        assertThat(osCharge.getCode()).isEqualTo("chargeCode");
        assertThat(osCharge.getDescription()).isEqualTo("chargeLabel");

        var pricePlanMatrixArgumentCaptor = ArgumentCaptor.forClass(PricePlanMatrixDto.class);
        verify(pricePlanMatrixApi).create(pricePlanMatrixArgumentCaptor.capture());

        var pricePlan = pricePlanMatrixArgumentCaptor.getValue();
        assertThat(pricePlan).isNotNull();
        assertThat(pricePlan.getCode()).isEqualTo("PPM_" + postData.getChargeCode());

        var pricePlanMatrixVersionArgumentCaptor = ArgumentCaptor.forClass(PricePlanMatrixVersionDto.class);
        verify(pricePlanMatrixVersionApi).create(pricePlanMatrixVersionArgumentCaptor.capture());
        
        var pricePlanVersion = pricePlanMatrixVersionArgumentCaptor.getValue();
        assertThat(pricePlanVersion).isNotNull();
        assertThat(pricePlanVersion.getValidity()).isNotNull();
        assertThat(pricePlanVersion.getValidity().getFrom()).isEqualTo(postData.getValidity().getFrom());
        assertThat(pricePlanVersion.getValidity().getTo()).isEqualTo(postData.getValidity().getTo());
        assertThat(pricePlanVersion.getLabel()).isEqualTo(postData.getLabel());
        assertThat(pricePlanVersion.getPriceVersionType()).isEqualTo(PriceVersionTypeEnum.FIXED);
        assertThat(pricePlanVersion.getMatrix()).isFalse();
        assertThat(pricePlanVersion.getPricePlanMatrixCode()).isEqualTo(pricePlan.getCode());
        
        verify(pricePlanMatrixVersionApi).updateProductVersionStatus(pricePlan.getCode(), 1, VersionStatusEnum.PUBLISHED);
        
        verify(pricePlanMatrixVersionApi).updateProductVersionStatus(pricePlan.getCode(), 1, VersionStatusEnum.PUBLISHED);
        
        verify(genericChargeTemplateApi).updateStatus(postData.getChargeCode(), ChargeTemplateStatusEnum.ACTIVE.name());

        var productArgumentCaptor = ArgumentCaptor.forClass(ProductDto.class);
        verify(productApi).create(productArgumentCaptor.capture());
        
        var product = productArgumentCaptor.getValue();
        assertThat(product).isNotNull();
        assertThat(product.getCode()).isEqualTo(postData.getProductCode());
        assertThat(product.getLabel()).isEqualTo(postData.getLabel());
        assertThat(product.getCurrentProductVersion()).isNotNull();
        assertThat(product.getCurrentProductVersion().getCurrentVersion()).isEqualTo(1);
        assertThat(product.getCurrentProductVersion().getProductCode()).isEqualTo(postData.getProductCode());
        assertThat(product.getCurrentProductVersion().getShortDescription()).isEqualTo("Product " + postData.getProductCode());
        assertThat(product.getCurrentProductVersion().getStatus()).isEqualTo(VersionStatusEnum.DRAFT);
        assertThat(product.getProductChargeTemplateMappingDto()).isNotNull().hasSize(1);
        assertThat(product.getProductChargeTemplateMappingDto().get(0).getChargeCode()).isEqualTo(postData.getChargeCode());
        assertThat(product.getProductChargeTemplateMappingDto().get(0).getProductCode()).isEqualTo(postData.getProductCode());
        
        verify(productApi).UpdateProductVersionStatus(postData.getProductCode(), 1, VersionStatusEnum.PUBLISHED);

    }
    
    @Test
    public void shouldCreateSimpleRecurrentProduct() {
        // given
        SimpleRecurrentProductDto postData = ImmutableSimpleRecurrentProductDto.builder()
                                                                               .chargeCode("chargeCode")
                                                                               .productCode("productCode")
                                                                               .label("chargeLabel")
                                                                               .calendar("calendar")
                                                                               .validity(new DatePeriod(Date.from(LocalDateTime.of(2023, 1, 1, 0, 0, 0)
                                                                                                                               .toInstant(java.time.ZoneOffset.UTC)),
                                                                                       Date.from(LocalDateTime.of(2023, 12, 13, 23, 59, 59)
                                                                                                              .toInstant(java.time.ZoneOffset.UTC))))
                                                                               .subscriptionProrata(true)
                                                                               .terminationProrata(true)
                                                                               .applyInAdvance(true)
                                                                               .anticipateEndOfSubscription(true)
                                                                               .price(BigDecimal.valueOf(12345678.90))
                                                                               .build();
        when(emWrapper.getEntityManager()).thenReturn(entityManager);
        
        when(productApi.create(any())).thenReturn(new ProductDto());
        // when
        productManagementApiService.createProductSimpleRecurrent(postData);
        
        // then
        var recurringChargeCaptor = ArgumentCaptor.forClass(RecurringChargeTemplateDto.class);
        verify(recurringChargeTemplateApi).create(recurringChargeCaptor.capture());
        RecurringChargeTemplateDto recurringCharge = recurringChargeCaptor.getValue();

        assertThat(recurringCharge).isNotNull();
        assertThat(recurringCharge.getCode()).isEqualTo("chargeCode");
        assertThat(recurringCharge.getDescription()).isEqualTo("chargeLabel");
        assertThat(recurringCharge.getCalendar()).isEqualTo("calendar");
        assertThat(recurringCharge.getSubscriptionProrata()).isTrue();
        assertThat(recurringCharge.getTerminationProrata()).isTrue();
        assertThat(recurringCharge.getApplyInAdvance()).isTrue();
        assertThat(recurringCharge.getAnticipateEndOfSubscription()).isTrue();

        var pricePlanMatrixArgumentCaptor = ArgumentCaptor.forClass(PricePlanMatrixDto.class);
        verify(pricePlanMatrixApi).create(pricePlanMatrixArgumentCaptor.capture());

        var pricePlan = pricePlanMatrixArgumentCaptor.getValue();
        assertThat(pricePlan).isNotNull();
        assertThat(pricePlan.getCode()).isEqualTo("PPM_" + postData.getChargeCode());

        var pricePlanMatrixVersionArgumentCaptor = ArgumentCaptor.forClass(PricePlanMatrixVersionDto.class);
        verify(pricePlanMatrixVersionApi).create(pricePlanMatrixVersionArgumentCaptor.capture());

        var pricePlanVersion = pricePlanMatrixVersionArgumentCaptor.getValue();
        assertThat(pricePlanVersion).isNotNull();
        assertThat(pricePlanVersion.getValidity()).isNotNull();
        assertThat(pricePlanVersion.getValidity().getFrom()).isEqualTo(postData.getValidity().getFrom());
        assertThat(pricePlanVersion.getValidity().getTo()).isEqualTo(postData.getValidity().getTo());
        assertThat(pricePlanVersion.getLabel()).isEqualTo(postData.getLabel());
        assertThat(pricePlanVersion.getPriceVersionType()).isEqualTo(PriceVersionTypeEnum.FIXED);
        assertThat(pricePlanVersion.getMatrix()).isFalse();
        assertThat(pricePlanVersion.getPricePlanMatrixCode()).isEqualTo(pricePlan.getCode());

        verify(pricePlanMatrixVersionApi).updateProductVersionStatus(pricePlan.getCode(), 1, VersionStatusEnum.PUBLISHED);

        verify(pricePlanMatrixVersionApi).updateProductVersionStatus(pricePlan.getCode(), 1, VersionStatusEnum.PUBLISHED);

        verify(genericChargeTemplateApi).updateStatus(postData.getChargeCode(), ChargeTemplateStatusEnum.ACTIVE.name());

        var productArgumentCaptor = ArgumentCaptor.forClass(ProductDto.class);
        verify(productApi).create(productArgumentCaptor.capture());

        var product = productArgumentCaptor.getValue();
        assertThat(product).isNotNull();
        assertThat(product.getCode()).isEqualTo(postData.getProductCode());
        assertThat(product.getLabel()).isEqualTo(postData.getLabel());
        assertThat(product.getCurrentProductVersion()).isNotNull();
        assertThat(product.getCurrentProductVersion().getCurrentVersion()).isEqualTo(1);
        assertThat(product.getCurrentProductVersion().getProductCode()).isEqualTo(postData.getProductCode());
        assertThat(product.getCurrentProductVersion().getShortDescription()).isEqualTo("Product " + postData.getProductCode());
        assertThat(product.getCurrentProductVersion().getStatus()).isEqualTo(VersionStatusEnum.DRAFT);
        assertThat(product.getProductChargeTemplateMappingDto()).isNotNull().hasSize(1);
        assertThat(product.getProductChargeTemplateMappingDto().get(0).getChargeCode()).isEqualTo(postData.getChargeCode());
        assertThat(product.getProductChargeTemplateMappingDto().get(0).getProductCode()).isEqualTo(postData.getProductCode());

        verify(productApi).UpdateProductVersionStatus(postData.getProductCode(), 1, VersionStatusEnum.PUBLISHED);
        
        
    }

    @Test
    public void shouldCreateSimpleUsageProduct() {
        // given
        var postData = ImmutableSimpleUsageProductDto.builder()
                                                     .chargeCode("chargeCode")
                                                     .productCode("productCode")
                                                     .label("chargeLabel")
                                                     .filterParam1("filter1")
                                                     .filterParam2("filter2")
                                                     .filterParam3("filter3")
                                                     .filterParam4("filter4")
                                                     .validity(new DatePeriod(Date.from(LocalDateTime.of(2023, 1, 1, 0, 0, 0)
                                                                                                     .toInstant(java.time.ZoneOffset.UTC)),
                                                             Date.from(LocalDateTime.of(2023, 12, 13, 23, 59, 59)
                                                                                    .toInstant(java.time.ZoneOffset.UTC))))
                                                     .price(BigDecimal.valueOf(12345678.90))
                                                     .build();
        when(emWrapper.getEntityManager()).thenReturn(entityManager);

        when(productApi.create(any())).thenReturn(new ProductDto());
        // when
        productManagementApiService.createProductSimpleUsage(postData);

        // then
        var usageChargeCaptor = ArgumentCaptor.forClass(UsageChargeTemplateDto.class);
        verify(usageChargeTemplateApi).create(usageChargeCaptor.capture());
        UsageChargeTemplateDto usageCharge = usageChargeCaptor.getValue();

        assertThat(usageCharge).isNotNull();
        assertThat(usageCharge.getCode()).isEqualTo("chargeCode");
        assertThat(usageCharge.getDescription()).isEqualTo("chargeLabel");
        assertThat(usageCharge.getFilterParam1()).isEqualTo("filter1");
        assertThat(usageCharge.getFilterParam2()).isEqualTo("filter2");
        assertThat(usageCharge.getFilterParam3()).isEqualTo("filter3");
        assertThat(usageCharge.getFilterParam4()).isEqualTo("filter4");
    

        var pricePlanMatrixArgumentCaptor = ArgumentCaptor.forClass(PricePlanMatrixDto.class);
        verify(pricePlanMatrixApi).create(pricePlanMatrixArgumentCaptor.capture());

        var pricePlan = pricePlanMatrixArgumentCaptor.getValue();
        assertThat(pricePlan).isNotNull();
        assertThat(pricePlan.getCode()).isEqualTo("PPM_" + postData.getChargeCode());

        var pricePlanMatrixVersionArgumentCaptor = ArgumentCaptor.forClass(PricePlanMatrixVersionDto.class);
        verify(pricePlanMatrixVersionApi).create(pricePlanMatrixVersionArgumentCaptor.capture());

        var pricePlanVersion = pricePlanMatrixVersionArgumentCaptor.getValue();
        assertThat(pricePlanVersion).isNotNull();
        assertThat(pricePlanVersion.getValidity()).isNotNull();
        assertThat(pricePlanVersion.getValidity().getFrom()).isEqualTo(postData.getValidity().getFrom());
        assertThat(pricePlanVersion.getValidity().getTo()).isEqualTo(postData.getValidity().getTo());
        assertThat(pricePlanVersion.getLabel()).isEqualTo(postData.getLabel());
        assertThat(pricePlanVersion.getPriceVersionType()).isEqualTo(PriceVersionTypeEnum.FIXED);
        assertThat(pricePlanVersion.getMatrix()).isFalse();
        assertThat(pricePlanVersion.getPricePlanMatrixCode()).isEqualTo(pricePlan.getCode());

        verify(pricePlanMatrixVersionApi).updateProductVersionStatus(pricePlan.getCode(), 1, VersionStatusEnum.PUBLISHED);

        verify(pricePlanMatrixVersionApi).updateProductVersionStatus(pricePlan.getCode(), 1, VersionStatusEnum.PUBLISHED);

        verify(genericChargeTemplateApi).updateStatus(postData.getChargeCode(), ChargeTemplateStatusEnum.ACTIVE.name());

        var productArgumentCaptor = ArgumentCaptor.forClass(ProductDto.class);
        verify(productApi).create(productArgumentCaptor.capture());

        var product = productArgumentCaptor.getValue();
        assertThat(product).isNotNull();
        assertThat(product.getCode()).isEqualTo(postData.getProductCode());
        assertThat(product.getLabel()).isEqualTo(postData.getLabel());
        assertThat(product.getCurrentProductVersion()).isNotNull();
        assertThat(product.getCurrentProductVersion().getCurrentVersion()).isEqualTo(1);
        assertThat(product.getCurrentProductVersion().getProductCode()).isEqualTo(postData.getProductCode());
        assertThat(product.getCurrentProductVersion().getShortDescription()).isEqualTo("Product " + postData.getProductCode());
        assertThat(product.getCurrentProductVersion().getStatus()).isEqualTo(VersionStatusEnum.DRAFT);
        assertThat(product.getProductChargeTemplateMappingDto()).isNotNull().hasSize(1);
        assertThat(product.getProductChargeTemplateMappingDto().get(0).getChargeCode()).isEqualTo(postData.getChargeCode());
        assertThat(product.getProductChargeTemplateMappingDto().get(0).getProductCode()).isEqualTo(postData.getProductCode());

        verify(productApi).UpdateProductVersionStatus(postData.getProductCode(), 1, VersionStatusEnum.PUBLISHED);


    }
}