package org.meveo.apiv2.catalog.resource;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.cpq.ProductDto;
import org.meveo.api.dto.response.catalog.SimpleChargeProductResponseDto;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.apiv2.catalog.SimpleOneshotProductDto;
import org.meveo.apiv2.catalog.SimpleRecurrentProductDto;
import org.meveo.apiv2.catalog.SimpleUsageProductDto;
import org.meveo.apiv2.catalog.service.ProductManagementApiService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

@Stateless
@Interceptors({ WsRestApiInterceptor.class })
public class ProductManagementRsImpl extends BaseRs implements ProductManagementRs  {
    
    @Inject
    private ProductManagementApiService productManagementApiService;
    
    @Override
    public ActionStatus createProductSimpleOneshot(SimpleOneshotProductDto postData) {

        try {
            ProductDto productSimpleOneShot = productManagementApiService.createProductSimpleOneShot(postData);

            SimpleChargeProductResponseDto simpleChargeProductResponseDto = new SimpleChargeProductResponseDto().setProduct(productSimpleOneShot);
            simpleChargeProductResponseDto.setStatus(ActionStatusEnum.SUCCESS);
            return simpleChargeProductResponseDto;
        } catch (Exception e) {
            ActionStatus status = new ActionStatus();
            processException(e, status);
            return status;
        }
    }

    @Override
    public ActionStatus createProductSimpleRecurrent(SimpleRecurrentProductDto postData) {

        try {
            ProductDto productSimpleRecurrent = productManagementApiService.createProductSimpleRecurrent(postData);

            SimpleChargeProductResponseDto simpleChargeProductResponseDto = new SimpleChargeProductResponseDto().setProduct(productSimpleRecurrent);
            simpleChargeProductResponseDto.setStatus(ActionStatusEnum.SUCCESS);
            return simpleChargeProductResponseDto;
        } catch (Exception e) {
            ActionStatus status = new ActionStatus();
            processException(e, status);
            return status;
        }
    }

    @Override
    public ActionStatus createProductSimpleUsage(SimpleUsageProductDto postData) {


        try {
            ProductDto productSimpleRecurrent = productManagementApiService.createProductSimpleUsage(postData);
            SimpleChargeProductResponseDto simpleChargeProductResponseDto = new SimpleChargeProductResponseDto().setProduct(productSimpleRecurrent);
            simpleChargeProductResponseDto.setStatus(ActionStatusEnum.SUCCESS);
            return simpleChargeProductResponseDto;
        } catch (Exception e) {
            ActionStatus status = new ActionStatus();
            processException(e, status);
            return status;
        }

        
    }
}
